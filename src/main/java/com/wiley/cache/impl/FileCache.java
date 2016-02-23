package com.wiley.cache.impl;

import com.wiley.cache.AbstractCache;
import com.wiley.cache.CacheException;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileCache<K extends Serializable, V extends Serializable> extends AbstractCache<K, V> {

    private final static Logger logger = Logger.getLogger(FileCache.class.getName());
    
    private final static String fileSeparator = System.getProperty("file.separator");
    public static final String FILES_LIMIT = "cache.file.limit.files";
    public static final String SIZE_LIMIT = "cache.file.limit.size";
    public static final String CACHE_PATH = "cache.file.path";

    private File cacheDirectory;

    private int sizeLimit;

    private String cacheId;
    private int filesSize;
    private int size;

    public FileCache() {
    }

    public FileCache(String cacheId) {
        this.cacheId = cacheId;
    }

    public FileCache(Properties props) {
        super(props);
    }

    public FileCache(Properties props, String cacheId) {
        super(props);
        this.cacheId = cacheId;
    }

    protected void init() {
        try {
            this.limit = Math.max(0, Integer.parseInt(properties.getProperty(FILES_LIMIT)));
        } catch(NumberFormatException e) {
            logger.log(Level.WARNING, "wrong value for files limit property", e);
            this.limit = Integer.valueOf(defaultProperties.getProperty(FILES_LIMIT));
        }
        logger.info("Files limit set to " + limit);

        try {
            this.sizeLimit = Math.max(0, Integer.parseInt(properties.getProperty(SIZE_LIMIT)));
        } catch(NumberFormatException e) {
            logger.log(Level.WARNING, "wrong value for size limit property", e);
            this.sizeLimit = Integer.valueOf(defaultProperties.getProperty(SIZE_LIMIT));
        }
        logger.info("Size limit set to " + sizeLimit);

        File pathDirectory;

        pathDirectory = getCacheDirectory(properties.getProperty(CACHE_PATH));
        if (pathDirectory == null) {
            pathDirectory = getCacheDirectory(defaultProperties.getProperty(CACHE_PATH));
            if (pathDirectory == null) {
                pathDirectory = getTempDirectory();
            }
        }

        if (cacheId == null) {
            cacheId = UUID.randomUUID().toString();
        }

        cacheDirectory = new File(pathDirectory.getPath() + fileSeparator + "cache" + fileSeparator + cacheId);
        if (!cacheDirectory.exists()) {
            if (!cacheDirectory.mkdirs()) {
                throw new CacheException("Cannot create cache directory");
            }
        }
        logger.info("File cache path set to " + cacheDirectory.getPath());

        if (cacheId != null) {
            int _size = 0;
            for (File dir : cacheDirectory.listFiles()) {
                if (dir.isDirectory()) {
                    for (File cacheEntry : dir.listFiles()) {
                        _size += cacheEntry.length();
                        size++;
                    }
                }
            }
            filesSize = _size;
        }
    }
    
    private File getCacheDirectory(String path) {
        if (!path.isEmpty()) {
            File directory = new File(path);
            if (!directory.exists() && !directory.mkdir()) {
                logger.log(Level.SEVERE, "cannot create path directory");
            } else if (!(directory.canRead() && directory.canWrite())) {
                logger.log(Level.SEVERE, "cache path should be readable and writable");
            } else {
                return directory;
            }
        }
        return null;
    }
    
    private File getTempDirectory() {
        File tempDirectory = new File(System.getProperty("java.io.tmpdir"));
        if (!(tempDirectory.exists() && tempDirectory.canRead() && tempDirectory.canWrite())) {
            throw new CacheException("temp folder should be readable and writable");
        }
        return tempDirectory;
    }

    public int filesSize() {
        try {
            readLock.lock();
            return filesSize;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int size() {
        try {
            readLock.lock();
            return size;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean containsKey(Object key) {
        try {
            readLock.lock();
            return getFile((K) key).exists();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException("This operation is not implemented yet");
    }

    public SimpleEntry<K, V> getEntry(Class<?> cls, int keyHash) {
        ObjectInputStream is = null;
        SimpleEntry<K, V> entry;
        try {
            readLock.lock();
            File file = getFile(cls, keyHash);
            if (!(file.exists() && file.canRead())) {
                return null;
            }
            is = new ObjectInputStream(
                    new BufferedInputStream(
                            new FileInputStream(file)));

            Object value = is.readObject();
            Object key = is.readObject();
            entry = new SimpleEntry<>((K) key, (V) value);
            entry.setLastAccessTime(is.readLong());
            return entry;
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Cannot read value", e);
        } finally {
            readLock.unlock();
            try {
                is.close();
            } catch (NullPointerException | IOException ignored) {}
        }
        return null;
    }
    
    private boolean writeCache(Entry<K, V> entry) {
        File file = getFile(entry.getKey());
        ObjectOutputStream os = null;
        try {
            os = new ObjectOutputStream(
                    new BufferedOutputStream(
                            new FileOutputStream(file)));
    
            os.writeObject(entry.getValue());
            os.writeObject(entry.getKey());
            os.writeLong(entry.getLastAccessTime());
            os.flush();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "");
            return false;
        } finally {
            try {
                os.close();
            } catch (NullPointerException | IOException ignored) {}
        }
        return true;
    }

    public V get(Class<?> cls, int keyHash) {
        try {
            SimpleEntry<K, V> entry = getEntry(cls, keyHash);
            entry.setLastAccessTime(System.nanoTime());
            writeCache(entry);
            return entry.getValue();
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Override
    public V get(Object key) {
        return get(key.getClass(), key.hashCode());
    }

    @Override
    public V put(K key, V value) {
        if (key == null  || value == null) {
            throw new NullPointerException("Key and value cannot be null");
        }
        try {
            writeLock.lock();

            if (size() >= limit) {
                throw new CacheException("Files limit is exceeded");
            }

            if (filesSize >= sizeLimit) {
                throw new CacheException("Files size is exceeded");
            }

            V currentValue = null;

            File file = getFile(key);
            if (file.exists()) {
                currentValue = get(key);
            }

            SimpleEntry<K, V> entry = new SimpleEntry<>(key, value);
            entry.setLastAccessTime(System.nanoTime());
            writeCache(entry);
            filesSize += file.length();
            size++;
            return currentValue;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public V remove(Object key) {
        try {
            writeLock.lock();
            V value = get(key);
            File entryFile = getFile((K) key);
            filesSize -= entryFile.length();
            size--;
            if (!entryFile.delete()) {
                throw new CacheException("Cannot delete cache entry");
            }
            return value;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        try {
            writeLock.lock();
            boolean success = true;
            for (File entryClassDir : cacheDirectory.listFiles()) {
                for (File file : entryClassDir.listFiles()) {
                    if (!file.delete()) {
                        success = false;
                    }
                }
            }
            if (!success) throw new CacheException("Cannot delete cache files");
            filesSize = 0;
            size = 0;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Set<K> keySet() {
        try {
            readLock.lock();
            return createMap().keySet();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Collection<V> values() {
        try {
            readLock.lock();
            return createMap().values();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        try {
            readLock.lock();
            return createMap().entrySet();
        } finally {
            readLock.unlock();
        }
    }

    private Map<K, V> createMap() {
        final Map<K, V> map = new HashMap<>();

        for (File dir : cacheDirectory.listFiles()) {
            if (dir.isDirectory()) {
                for (File key : dir.listFiles()) {
                    try {
                        Map.Entry<K, V> entry = getEntry(Class.forName(dir.getName()), Integer.valueOf(key.getName()));
                        map.put(entry.getKey(), entry.getValue());
                    } catch(ClassNotFoundException ignored) {}
                }
            }
        }
        return map;
    }

    private File getFile(Class cls, int keyHash) {
        File keyClassDir = new File(cacheDirectory, cls.getName());
        if (!keyClassDir.exists()) {
            if (!keyClassDir.mkdir()) {
                throw new CacheException("Cannot create cache key directory");
            }
        }
        return new File(keyClassDir, String.valueOf(keyHash));
    }

    private File getFile(K key) {
        return getFile(key.getClass(), key.hashCode());
    }

    public String getCacheId() {
        return cacheId;
    }

    @Override
    public List<Entry<K, V>> getEntries() {
        try {
            readLock.lock();
            List<Entry<K, V>> entries = new ArrayList<>();
            for (Map.Entry<K, V> mapEntry : entrySet()) {
                entries.add(getEntry(mapEntry.getKey().getClass(), mapEntry.getKey().hashCode()));
            }
            return entries;
        } finally {
            readLock.unlock();
        }
    }
}
