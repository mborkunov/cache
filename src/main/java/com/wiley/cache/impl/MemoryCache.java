package com.wiley.cache.impl;

import com.wiley.cache.AbstractCache;
import com.wiley.cache.Cache;
import com.wiley.cache.CacheException;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MemoryCache<K, V> extends AbstractCache<K, V> {

    private Map<K, Cache.Entry<K, V>> storage = new HashMap<>();

    private final static Logger logger = Logger.getLogger(MemoryCache.class.getName());

    public static final String LIMIT = "cache.memory.limit";


    public MemoryCache() {
    }

    public MemoryCache(Properties props) {
        super(props);
    }

    protected void init() {
        try {
            this.limit = Math.max(0, Integer.parseInt(properties.getProperty(LIMIT)));
        } catch(NumberFormatException e) {
            logger.log(Level.WARNING, "wrong value for limit property", e);
            this.limit = Integer.valueOf(defaultProperties.getProperty(LIMIT));
        }
        logger.info("Memory cache limit set to " + limit);
    }

    @Override
    public int size() {
        try {
            readLock.lock();
            return storage.size();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean containsKey(Object key) {
        try {
            readLock.lock();
            return storage.containsKey(key);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean containsValue(Object value) {
        try {
            readLock.lock();
            return storage.containsValue(value);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public V get(Object key) {
        try {
            readLock.lock();
            try {
                return storage.get(key).getValue(false);
            } catch (NullPointerException e) {
                return null;
            }
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public V put(K key, V value) {
        if (key == null  || value == null) {
            throw new NullPointerException("Key and value cannot be null");
        }
        try {
            writeLock.lock();
            if (size() >= limit) {
                throw new CacheException("Cache limit is exceeded. Limit: " + limit);
            }
            Cache.Entry<K, V> entry  = new SimpleEntry<>(key, value);
            try {
                return storage.put(key, entry).getValue(false);
            } catch (NullPointerException e) {
                return null;
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public V remove(Object key) {
        try {
            writeLock.lock();
            try {
                return storage.remove(key).getValue();
            } catch (NullPointerException e) {
                return null;
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        try {
            writeLock.lock();
            for (Map.Entry<? extends K, ? extends V>  entry : map.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void clear() {
        try {
            writeLock.lock();
            storage.clear();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Set<K> keySet() {
        try {
            readLock.lock();
            return storage.keySet();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Collection<V> values() {
        try {
            readLock.lock();
            Collection<V> collection = new ArrayList<>();
            for (Cache.Entry<K, V> entry: storage.values()) {
                collection.add(entry.getValue());
            }
            return collection;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        try {
            readLock.lock();
            Set<Map.Entry<K, V>> set = new HashSet<>();
            for (Map.Entry<K, Cache.Entry<K, V>> entry : storage.entrySet()) {
                set.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().getValue()));
            }
            return set;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public List<Entry<K, V>> getEntries() {
        try {
            readLock.lock();
            List<Entry<K, V>> list = new ArrayList<>();
            list.addAll(storage.values());
            return list;
        } finally {
            readLock.unlock();
        }
    }
}
