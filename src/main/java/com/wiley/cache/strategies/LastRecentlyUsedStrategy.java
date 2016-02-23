package com.wiley.cache.strategies;

import com.wiley.cache.Cache;

import java.util.Comparator;

public class LastRecentlyUsedStrategy extends AbstractStrategy {

    public <K, V> Comparator<Cache.Entry<K, V>> getComparator(Cache<K, V> cache) {
        return new Comparator<Cache.Entry<K, V>>() {
            @Override
            public int compare(Cache.Entry<K, V> o1, Cache.Entry<K, V> o2) {
                int res = - o1.getLastAccessTime().compareTo(o2.getLastAccessTime());
                if (res != 0) return res;
                return -1;
            }
        };
    }
}
