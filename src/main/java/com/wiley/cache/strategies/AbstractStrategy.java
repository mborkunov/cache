package com.wiley.cache.strategies;

import com.wiley.cache.Cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractStrategy implements Strategy {

    @Override
    public <K, V> Cache.Entry<K, V> pop(Cache<K, V> cache) {
        List<Cache.Entry<K, V>> list = list(cache);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    @Override
    public <K, V> Cache.Entry<K, V> poll(Cache<K, V> cache) {
        List<Cache.Entry<K, V>> list = list(cache);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(list.size() - 1);
    }
    
    private <K, V> List<Cache.Entry<K, V>> list(Cache<K, V> cache) {
        List<Cache.Entry<K, V>> list = new ArrayList<>();
        list.addAll(cache.getEntries());
        Collections.sort(list, getComparator(cache));
        return list;
    }
}
