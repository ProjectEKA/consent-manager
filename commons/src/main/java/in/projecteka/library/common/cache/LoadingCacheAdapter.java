package in.projecteka.library.common.cache;

import com.google.common.cache.LoadingCache;
import reactor.core.publisher.Mono;

import java.util.concurrent.ExecutionException;

public class LoadingCacheAdapter implements CacheAdapter<String, String> {
    private final LoadingCache<String, String> loadingCache;

    public LoadingCacheAdapter(LoadingCache<String, String> loadingCache) {
        this.loadingCache = loadingCache;
    }

    @Override
    public Mono<String> get(String key) {
        try {
            String value = loadingCache.get(key);
            if (!value.isEmpty()) {
                return Mono.just(value);
            }
            return Mono.empty();
        } catch (ExecutionException e) {
            return Mono.error(new CacheNotAccessibleException("cache.not.accessible"));
        }
    }

    @Override
    public Mono<Void> put(String key, String value) {
        loadingCache.put(key, value);
        return Mono.empty();
    }

    @Override
    public Mono<String> getIfPresent(String key) {
        String value = loadingCache.getIfPresent(key);
        if (value != null && !value.isEmpty()) {
            return Mono.just(value);
        }
        return Mono.empty();
    }

    @Override
    public Mono<Void> invalidate(String key) {
        loadingCache.invalidate(key);
        return Mono.empty();
    }

    @Override
    public Mono<Boolean> exists(String key) {
        String value = loadingCache.getIfPresent(key);
        return Mono.just(value != null);
    }

    @Override
    public Mono<Long> increment(String key) {//TODO test
        String value = loadingCache.getIfPresent(key);
        if (value == null || value.isEmpty()) {
            value = "1";
            loadingCache.put(key, value);
            return Mono.just(1L);
        }
        long increment = Long.parseLong(value) + 1;
        loadingCache.put(key, String.valueOf(increment));
        return Mono.just(increment);
    }
}
