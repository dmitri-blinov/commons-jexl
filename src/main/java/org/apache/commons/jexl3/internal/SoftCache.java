/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.jexl3.internal;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.AbstractMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A soft referenced cache.
 * <p>
 * The actual cache is held through a soft reference, allowing it to be GCed
 * under memory pressure.</p>
 *
 * @param <K> the cache key entry type
 * @param <V> the cache key value type
 */
public class SoftCache<K, V> {
    /**
     * The default cache load factor.
     */
    private static final float LOAD_FACTOR = 0.75f;
    /**
     * The cache size.
     */
    private final int size;
    /**
     * The soft reference to the cache map.
     */
    private SoftReference<Map<K, V>> ref = null;
    /**
     * The cache r/w lock.
     */
    private final ReadWriteLock lock;

    /**
     * Creates a new instance of a soft cache.
     *
     * @param theSize the cache size
     */
    SoftCache(final int theSize) {
        size = theSize;
        lock = new ReentrantReadWriteLock();
    }

    /**
     * Returns the cache size.
     *
     * @return the cache size
     */
    public int size() {
        return size;
    }

    /**
     * Clears the cache.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            ref = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets a value from cache.
     *
     * @param key the cache entry key
     * @return the cache entry value
     */
    public V get(final K key) {
        lock.readLock().lock();
        try {
            final Map<K, V> map = ref != null ? ref.get() : null;
            return map != null ? map.get(key) : null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Puts a value in cache.
     *
     * @param key the cache entry key
     * @param script the cache entry value
     */
    public void put(final K key, final V script) {
        lock.writeLock().lock();
        try {
            Map<K, V> map = ref != null ? ref.get() : null;
            if (map == null) {
                map = createCache(size);
                ref = new SoftReference<>(map);
            }
            map.put(key, script);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Produces the cache entry set.
     * <p>
     * For testing only, perform deep copy of cache entries
     *
     * @return the cache entry list
     */
    public List<Map.Entry<K, V>> entries() {
        lock.readLock().lock();
        try {
            final Map<K, V> map = ref != null ? ref.get() : null;
            if (map == null) {
                return Collections.emptyList();
            }
            final Set<Map.Entry<K, V>> set = map.entrySet();
            final List<Map.Entry<K, V>> entries = new ArrayList<>(set.size());
            for (final Map.Entry<K, V> e : set) {
                entries.add(new AbstractMap.SimpleEntry<>(e));
            }
            return entries;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Creates the cache store.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param cacheSize the cache size, must be &gt; 0
     * @return a Map usable as a cache bounded to the given size
     */
    public <K, V> Map<K, V> createCache(final int cacheSize) {
        return new java.util.LinkedHashMap<K, V>(cacheSize, LOAD_FACTOR, true) {
            /**
             * Serial version UID.
             */
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
                return super.size() > cacheSize;
            }
        };
    }
}
