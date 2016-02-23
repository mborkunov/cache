package com.wiley.cache;

import org.junit.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class AbstractCacheTest {
    
    protected Cache<Serializable, Serializable> cache;
    
    @Test
    public void testSize() throws Exception {
        for (int i = 0; i < 5; i++) {
            cache.put(i, i);
            assertTrue(cache.size() == i + 1);
        }
        assertTrue(cache.size() == 5);

        for (int i = 0; i < 3; i++) {
            cache.remove(i);
        }
        assertTrue(cache.size() == 2);
    }

    @Test
    public void testIsEmpty() throws Exception {
        assertTrue(cache.isEmpty());
        cache.put("hi", "hello");
        assertFalse(cache.isEmpty());
        cache.remove("hi");
        assertTrue(cache.isEmpty());
    }

    @Test
    public void testContainsKey() throws Exception {
        assertFalse(cache.containsKey(false));
        assertFalse(cache.containsKey("test"));
        cache.put("test", true);
        assertTrue(cache.containsKey("test"));
        cache.remove("test");
        assertFalse(cache.containsKey("test"));
    }

    @Test
    public void testGet() throws Exception {
        cache.put(true, 998);
        assertTrue(((Integer) cache.get(true)) == 998);
        assertTrue(cache.get(false) == null);
    }

    @Test
    public void testPut() throws Exception {
        cache.put(true, 4);
        assertTrue((Integer) cache.put(true, 6) == 4);
        assertTrue((Integer) cache.get(true) == 6);
    }

    @Test
    public void testRemove() throws Exception {
        cache.put("ninja", "gaiden");
        assertTrue(cache.remove("ninja").equals("gaiden"));
        assertTrue(cache.isEmpty());
    }

    @Test
    public void testPutAll() throws Exception {
        Map<Serializable, Serializable> map = new HashMap<>();
        map.put(2L, 1);
        map.put("test", false);
        map.put(false, "Value");

        cache.putAll(map);
        assertTrue(cache.size() == map.size());
    }

    @Test
    public void testClear() throws Exception {
        Map<Serializable, Serializable> map = new HashMap<>();
        map.put(2L, 1);
        map.put("test", false);
        map.put(true, "Value");

        cache.putAll(map);

        cache.clear();
        assertTrue(cache.isEmpty());
    }
}
