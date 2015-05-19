/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
 *
 * Copyright (C) 2005-2008 Jive Software. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lippi.cache;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DefaultLocalCacheStrategy implements CacheFactoryStrategy {

    /**
     * Keep track of the locks that are currently being used.
     */
    private Map<Object, LockAndCount> locks = new ConcurrentHashMap<Object, LockAndCount>();

    public DefaultLocalCacheStrategy() {
    }


    public Cache createCache(String name) {
        // Get cache configuration from system properties or default (hardcoded) values
        long maxSize = CacheFactory.getMaxCacheSize();
        long lifetime = CacheFactory.getMaxCacheLifetime();
        // Create cache with located properties
        return new DefaultCache(name, maxSize, lifetime);

    }

    public void destroyCache(Cache cache) {
        cache.clear();
    }


    public void updateCacheStats(Map<String, Cache> caches) {
    }


    public Lock getLock(Object key, Cache cache) {
        Object lockKey = key;
        if (key instanceof String) {
            lockKey = ((String) key).intern();
        }

		return new LocalLock(lockKey);
    }

	private void acquireLock(Object key) {
		ReentrantLock lock = lookupLockForAcquire(key);
		lock.lock();
	}

	private void releaseLock(Object key) {
		ReentrantLock lock = lookupLockForRelease(key);
		lock.unlock();
	}

	private ReentrantLock lookupLockForAcquire(Object key) {
        synchronized(key) {
            LockAndCount lac = locks.get(key);
            if (lac == null) {
                lac = new LockAndCount(new ReentrantLock());
                lac.count = 1;
                locks.put(key, lac);
            }
            else {
                lac.count++;
            }

            return lac.lock;
        }
    }

	private ReentrantLock lookupLockForRelease(Object key) {
        synchronized(key) {
            LockAndCount lac = locks.get(key);
            if (lac == null) {
                throw new IllegalStateException("No lock found for object " + key);
            }

            if (lac.count <= 1) {
                locks.remove(key);
            }
            else {
                lac.count--;
            }

            return lac.lock;
        }
    }


    private class LocalLock implements Lock {
		private final Object key;

		LocalLock(Object key) {
			this.key = key;
		}

		public void lock(){
			acquireLock(key);
		}

		public void	unlock() {
			releaseLock(key);
		}

        public void	lockInterruptibly(){
			throw new UnsupportedOperationException();
		}

		public Condition newCondition(){
			throw new UnsupportedOperationException();
		}

		public boolean 	tryLock() {
			throw new UnsupportedOperationException();
		}

		public boolean 	tryLock(long time, TimeUnit unit) {
			throw new UnsupportedOperationException();
		}

	}

    /**
     * wrapper class for ReentrantLock with a count for holders
     */
    private static class LockAndCount {
        final ReentrantLock lock;
        int count;

        LockAndCount(ReentrantLock lock) {
            this.lock = lock;
        }
    }

}
