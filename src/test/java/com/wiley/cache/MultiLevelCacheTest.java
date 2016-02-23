package com.wiley.cache;


import com.wiley.cache.impl.FileCache;
import com.wiley.cache.impl.MemoryCache;
import com.wiley.cache.impl.MultiLevelCache;
import com.wiley.cache.strategies.LastRecentlyUsedStrategy;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.Arrays;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MultiLevelCacheTest extends AbstractCacheTest {

    @Before
    public void before() {
        Cache<Serializable, Serializable> memoryCache = new MemoryCache<>();
        Cache<Serializable, Serializable> fileCache = new FileCache<>();

        cache = new MultiLevelCache<>(Arrays.asList(memoryCache, fileCache), new LastRecentlyUsedStrategy());
    }

    @Test
    public void multiTest() {
        MultiLevelCache<Serializable, Serializable> multiCache = (MultiLevelCache<Serializable, Serializable>) cache;
        
        for (int i = 0; i < 100; i++) {
            multiCache.put(i, i);
        }

        multiCache.remove(30);
        multiCache.remove(60);
        multiCache.get(20);
        multiCache.get(80);

        multiCache.reorganize();
        
        assertTrue(multiCache.size() == 98);
    }

    @Test
    public void sizeExceededTest() {
        try {
            for (int i = 0; i < 210; i++) {
                cache.put(i, i);
            }
            fail("exception should be thrown");
        } catch (CacheException ignored) {
        }
    }
}
