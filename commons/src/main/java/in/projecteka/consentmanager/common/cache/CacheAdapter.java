package in.projecteka.consentmanager.common.cache;

import reactor.core.publisher.Mono;

public interface CacheAdapter<K,V> {
    Mono<V> get(K key);

    Mono<Void> put(K key, V value);

    Mono<V> getIfPresent(K key);

    Mono<Void> invalidate(K key);

    Mono<Boolean> exists(K key);

    Mono<Long> increment(String key);
}
