package in.projecteka.consentmanager.common.cache;

import com.google.common.cache.LoadingCache;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static in.projecteka.consentmanager.common.Constants.DEFAULT_CACHE_VALUE;
import static reactor.core.publisher.Mono.fromCallable;
import static reactor.core.publisher.Mono.fromRunnable;

@AllArgsConstructor
public class LoadingCacheGenericAdapter<T> implements CacheAdapter<String, T> {
    private final LoadingCache<String, T> loadingCache;

    @Override
    public Mono<T> get(String key) {
        return fromCallable(() -> loadingCache.get(key) == DEFAULT_CACHE_VALUE ? null : loadingCache.get(key));
    }

    @Override
    public Mono<Void> put(String key, T value) {
        return fromRunnable(() -> loadingCache.put(key, value));
    }

    @Override
    public Mono<T> getIfPresent(String key) {
        return get(key);
    }

    @Override
    public Mono<Void> invalidate(String key) {
        return fromRunnable(() -> loadingCache.invalidate(key));
    }

    @Override
    public Mono<Boolean> exists(String key) {
        return fromCallable(() -> loadingCache.getIfPresent(key) != LocalDateTime.MIN);
    }

    @Override
    public Mono<Long> increment(String key) {
        throw new UnsupportedOperationException();
    }
}
