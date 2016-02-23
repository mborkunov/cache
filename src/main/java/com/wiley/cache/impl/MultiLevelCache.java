package com.wiley.cache.impl;

import com.wiley.cache.AbstractCache;
import com.wiley.cache.Cache;
import com.wiley.cache.CacheException;
import com.wiley.cache.strategies.Strategy;

import java.util.*;

public class MultiLevelCache<K, V> extends AbstractCache<K, V> {
    
    protected List<Cache<K, V>> caches;
    protected Strategy strategy;

    public MultiLevelCache(List<Cache<K, V>> caches, Strategy strategy) {
        if (caches.isEmpty()) {
            throw new IllegalArgumentException("At least one cache should be provided");
        }
        this.caches = caches;
        this.strategy = strategy;
    }

    @Override
    public List<Entry<K, V>> getEntries() {
        List<Entry<K, V>> entries = new ArrayList<>();
        for (Cache<K, V> cache : caches) {
            entries.addAll(cache.getEntries());
        }
        return entries;
    }

    @Override
    protected void init() {
    }

    @Override
    public int size() {
        try {
            readLock.lock();
            return getEntries().size();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean containsKey(Object key) {
        try {
            readLock.lock();
            for (Cache<K, V> cache : caches) {
                if (cache.containsKey(key)) {
                    return true;
                }
            }
            return false;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean containsValue(Object value) {
        try {
            readLock.lock();
            for (Cache<K, V> cache : caches) {
                if (cache.containsValue(value)) {
                    return true;
                }
            }
            return false;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public V get(Object key) {
        try {
            readLock.lock();
            V value = null;
            byte counter = 0;
            for (Cache<K, V> cache : caches) {
                counter++;
                if (cache.containsKey(key)) {
                    value = cache.get(key);
                    break;
                }

            }
            if (value != null && counter > 1) {
                //reorganize();
            }
            return value;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public V put(K key, V value) {
        try {
            writeLock.lock();
            byte level = 0;
            for (Cache<K, V> cache : caches) {
                level++;
                try {
                    V v = cache.put(key, value);
                    //reorganize();
                    return v;
                } catch (CacheException ignored) {
                    // that means this value should be stored in the next cache
                }
            }
            throw new CacheException("Cannot store");
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public V remove(Object key) {
        try {
            writeLock.lock();
            V value;
            byte level = 0;
            for (Cache<K, V> cache : caches) {
                level++;
                if (cache.containsKey(key) && (value = cache.remove(key)) != null) {
                    //reorganize();
                    return value;
                }
            }
            return null;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        try {
            writeLock.lock();
            for (Map.Entry<? extends K, ? extends V> entry: m.entrySet()) {
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
            for (Cache<K, V> cache : caches) {
                cache.clear();
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException("not implemented yet");
    }

    public void reorganize() {
        try {
            writeLock.lock();
            List<Entry<K, V>> entries = getEntries();

            Collections.sort(entries, strategy.getComparator(this));
            Collections.reverse(entries);
            clear();

            for (Cache<K, V> cache : caches) {
                for (int i = entries.size() - 1; i >= 0; i--) {
                    Map.Entry<K, V> entry = entries.get(i);
                    int available = cache.getLimit() - cache.size();
                    if (available == 0) {
                        break;
                    }
                    cache.put(entry.getKey(), entry.getValue());
                    entries.remove(i);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }
}
