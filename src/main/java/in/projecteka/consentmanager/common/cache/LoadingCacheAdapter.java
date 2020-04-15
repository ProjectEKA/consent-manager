package in.projecteka.consentmanager.common.cache;

import com.google.common.cache.LoadingCache;
import in.projecteka.consentmanager.user.exception.CacheNotAccessibleException;
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
}
