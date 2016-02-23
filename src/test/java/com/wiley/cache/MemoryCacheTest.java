package com.wiley.cache;

import com.wiley.cache.impl.MemoryCache;
import com.wiley.cache.strategies.LastRecentlyUsedStrategy;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;
import java.util.TreeSet;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MemoryCacheTest extends AbstractCacheTest {

    @Before
    public void before() {
        cache = new MemoryCache<>();
    }

    @Test
    public void testLimit() {
        Properties props = new Properties();
        props.setProperty(MemoryCache.LIMIT, String.valueOf(50));
        MemoryCache<Object, Object> cache = new MemoryCache<>(props);

        try {
            for (int i = 0; i < 51; i++) {
                cache.put(i, i);
            }
            fail("Cache size should be exceeded");
        } catch (CacheException e) {
            assertTrue(cache.size() == 50);
        }
    }

    @Test
    public void testWeight() {
        MemoryCache<Object, Object> cache = new MemoryCache<>();

        for (int i = 0; i < 50; i++) {
            cache.put(i, i);
        }

        cache.get(13);
        cache.get(15);
        cache.get(16);
        cache.get(21);

        cache.remove(15);
        cache.remove(48);

        TreeSet<Cache.Entry<Object, Object>> entries = new TreeSet<>(new LastRecentlyUsedStrategy().getComparator(cache));

        for (Cache.Entry<Object, Object> entry : cache.getEntries()) {
            entries.add(entry);
        }

        assertTrue(((Integer) entries.pollFirst().getKey()) == 21);
        assertTrue(((Integer) entries.pollFirst().getKey()) == 16);
        assertTrue(((Integer) entries.pollFirst().getKey()) == 13);
        assertTrue(((Integer) entries.pollFirst().getKey()) == 49);
    }
}
