package com.wiley;

import com.wiley.cache.Cache;
import com.wiley.cache.impl.FileCache;
import com.wiley.cache.impl.MemoryCache;
import com.wiley.cache.impl.MultiLevelCache;
import com.wiley.cache.strategies.RandomStrategy;

import java.io.Serializable;
import java.util.Arrays;

public class CacheApp {
    
    private Cache<Serializable, Serializable> cache;

    public static void main(String[] args) {
        new CacheApp().start();
    }

    private void start() {
        Cache<Serializable, Serializable> memoryCache = new MemoryCache<>();
        Cache<Serializable, Serializable> fileCache = new FileCache<>();

        cache = new MultiLevelCache<>(Arrays.asList(memoryCache, fileCache), new RandomStrategy());
    }
}
