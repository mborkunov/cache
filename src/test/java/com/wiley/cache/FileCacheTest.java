package com.wiley.cache;

import com.wiley.cache.impl.FileCache;
import com.wiley.cache.strategies.LastRecentlyUsedStrategy;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.util.Properties;
import java.util.Random;
import java.util.TreeSet;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FileCacheTest extends AbstractCacheTest {
    @Before
    public void before() {
        cache = new FileCache<>();
    }

    @Test
    public void testLimit() {
        Random random = new Random();
        Properties fileProps = new Properties();
        fileProps.setProperty(FileCache.FILES_LIMIT, String.valueOf(10));
        FileCache<Serializable, Serializable> fileCache = new FileCache<>(fileProps);

        try {
            for (int i = 0; i < 11; i++) {
                fileCache.put(i, random.nextInt(1000));
            }
            fail("Cache size should be exceeded");
        } catch (CacheException e) {
            assertTrue(fileCache.size() == 10);
        }
    }

    @Test
    public void testWeight() {
        FileCache<Serializable, Serializable> cache = new FileCache<>();

        for (int i = 0; i < 50; i++) {
            cache.put(i, i);
        }

        cache.get(13);
        cache.get(15);
        cache.get(16);
        cache.get(21);

        cache.remove(15);
        cache.remove(48);

        TreeSet<Cache.Entry<Serializable, Serializable>> entries = new TreeSet<>(new LastRecentlyUsedStrategy().getComparator(cache));

        for (Cache.Entry<Serializable, Serializable> entry : cache.getEntries()) {
            entries.add(entry);
        }
        
        assertTrue(((Integer) entries.pollFirst().getKey()) == 21);
        assertTrue(((Integer) entries.pollFirst().getKey()) == 16);
        assertTrue(((Integer) entries.pollFirst().getKey()) == 13);
        assertTrue(((Integer) entries.pollFirst().getKey()) == 49);
    }
}
