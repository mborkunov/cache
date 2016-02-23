package com.wiley.cache.strategies;

import com.wiley.cache.Cache;

import java.util.Comparator;

public interface Strategy {

    <K, V> Cache.Entry<K, V> pop(Cache<K, V> cache);
    <K, V> Cache.Entry<K, V> poll(Cache<K, V> cache);

    <K, V> Comparator<Cache.Entry<K, V>> getComparator(Cache<K, V> cache);
}
