package in.projecteka.consentmanager.common.cache;

import com.google.common.cache.LoadingCache;
import in.projecteka.consentmanager.user.exception.CacheNotAccessibleException;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class LoadingCacheAdapter implements ICacheAdapter<String, Optional<String>> {
    private final LoadingCache<String, Optional<String>> loadingCache;

    public LoadingCacheAdapter(LoadingCache<String,Optional<String>> loadingCache) {
        this.loadingCache = loadingCache;
    }

    @Override
    public Optional<String> get(String key) {
        try {
            return loadingCache.get(key);
        } catch (ExecutionException e) {
            throw new CacheNotAccessibleException("cache.not.accessible");
        }
    }

    @Override
    public void put(String key, Optional<String> value) {
        loadingCache.put(key, value);
    }

    @Override
    public Optional<String> getIfPresent(String key) {
        return loadingCache.getIfPresent(key);
    }

    @Override
    public void invalidate(String key) {
        loadingCache.invalidate(key);
    }
}
