package com.wiley.cache.strategies;

import com.wiley.cache.Cache;

import java.util.Comparator;
import java.util.Random;

public class RandomStrategy extends AbstractStrategy {

    private Random random = new Random();

    public <K, V> Comparator<Cache.Entry<K, V>>  getComparator(Cache<K, V> cache) {
        return new Comparator<Cache.Entry<K, V>>() {
            @Override
            public int compare(Cache.Entry<K, V> o1, Cache.Entry<K, V> o2) {
                return random.nextBoolean() ? 1 : -1;
            }
        };
    }
}
