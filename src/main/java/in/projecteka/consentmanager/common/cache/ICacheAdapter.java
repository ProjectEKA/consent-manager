package in.projecteka.consentmanager.common.cache;

public interface ICacheAdapter<K,V> {
    V get(K key);

    void put(K key, V value);

    V getIfPresent(K value);

    void invalidate(K key);
}
