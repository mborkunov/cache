package com.wiley.cache;

import java.util.List;
import java.util.Map;

public interface Cache<K, V> extends Map<K, V> {

    List<Entry<K, V>> getEntries();
    int getLimit();

    abstract class Entry<K, V> implements Map.Entry<K, V> {
        protected K key;
        protected V value;

        protected Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }
        
        public abstract Long getLastAccessTime();

        public abstract V getValue(boolean quite);
    }
}
