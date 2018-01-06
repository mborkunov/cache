package com.wiley.cache;

import java.io.*;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractCache<K, V> implements Cache<K, V> {

    private final static Logger logger = Logger.getLogger(AbstractCache.class.getName());

    protected ReentrantReadWriteLock.ReadLock readLock;
    protected ReentrantReadWriteLock.WriteLock writeLock;

    protected static Properties defaultProperties;
    protected Properties properties;
    protected int limit;

    {
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        readLock = lock.readLock();
        writeLock = lock.writeLock();
        if (defaultProperties == null) {
            defaultProperties = new Properties();
            InputStream is = null;
            try {
                is = new BufferedInputStream(getClass().getResourceAsStream("/default.properties"));
                defaultProperties.load(is);
                logger.info("default property file was successfully loaded");
            } catch (IOException e) {
                String message = "cannot load default properties";
                logger.log(Level.SEVERE, message, e);
                throw new CacheException("cannot load default properties");
            } finally {
                try {
                    is.close();
                } catch (NullPointerException | IOException ignored) {
                }
            }
        }
    }

    public AbstractCache() {
        properties = defaultProperties;
        init();
    }
    
    public abstract List<Entry<K, V>> getEntries();

    @Override
    public boolean isEmpty() {
        try {
            readLock.lock();
            return size() == 0;
        } finally {
            readLock.unlock();
        }
    }

    public AbstractCache(Properties properties) {
        if (properties == null) {
            throw new NullPointerException("properties argument cannot be null");
        }
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            properties.store(os, null);

            this.properties = new Properties(defaultProperties);
            this.properties.load(new ByteArrayInputStream(os.toByteArray()));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "cannot create properties", e);
        }
        init();
    }

    abstract protected void init();

    public class SimpleEntry<K, V> extends Cache.Entry<K, V> {

        protected long lastAccessTime = System.nanoTime();

        public SimpleEntry(K key, V value) {
            super(key, value);
        }

        @Override
        public V getValue(boolean quite) {
            if (!quite) {
                lastAccessTime = System.nanoTime();
            }
            return value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public V setValue(V value) {
            V _value = this.value;
            this.value = value;
            return _value;
        }

        public Long getLastAccessTime() {
            return lastAccessTime;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Cache.Entry)) return false;

            Cache.Entry that = (Cache.Entry) o;

            if (key != null ? !key.equals(that.key) : that.key != null) return false;
            if (value != null ? !value.equals(that.value) : that.value != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = key != null ? key.hashCode() : 0;
            result = 31 * result + (value != null ? value.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "SimpleEntry{key=" + key + ", value=" + value + "}";
        }

        public void setLastAccessTime(long lastAccessTime) {
            this.lastAccessTime = lastAccessTime;
        }
    }

    @Override
    public int getLimit() {
        return limit;
    }
}
