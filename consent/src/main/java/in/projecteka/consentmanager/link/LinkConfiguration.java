package in.projecteka.consentmanager.link;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import in.projecteka.consentmanager.clients.DiscoveryServiceClient;
import in.projecteka.consentmanager.clients.LinkServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.properties.GatewayServiceProperties;
import in.projecteka.consentmanager.clients.properties.LinkServiceProperties;
import in.projecteka.consentmanager.common.ServiceAuthentication;
import in.projecteka.consentmanager.common.cache.RedisOptions;
import in.projecteka.consentmanager.link.discovery.Discovery;
import in.projecteka.consentmanager.link.discovery.DiscoveryRepository;
import in.projecteka.consentmanager.link.hiplink.UserAuthInitAction;
import in.projecteka.consentmanager.link.hiplink.UserAuthentication;
import in.projecteka.consentmanager.link.link.Link;
import in.projecteka.consentmanager.link.link.LinkRepository;
import in.projecteka.consentmanager.link.link.LinkTokenVerifier;
import in.projecteka.library.common.CentralRegistry;
import in.projecteka.library.common.cache.CacheAdapter;
import in.projecteka.library.common.cache.LoadingCacheAdapter;
import in.projecteka.library.common.cache.RedisCacheAdapter;
import io.vertx.pgclient.PgPool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.PublicKey;
import java.util.concurrent.TimeUnit;

@Configuration
public class LinkConfiguration {

    @Bean
    public DiscoveryRepository discoveryRepository(PgPool pgPool) {
        return new DiscoveryRepository(pgPool);
    }

    @Bean
    public LinkRepository linkRepository(PgPool pgPool) {
        return new LinkRepository(pgPool);
    }

    @Bean
    public LinkTokenVerifier linkTokenVerifier(@Qualifier("keySigningPublicKey") PublicKey key,
                                               LinkRepository linkRepository) {
        return new LinkTokenVerifier(key, linkRepository);
    }

    @Bean
    public Link link(@Qualifier("customBuilder") WebClient.Builder builder,
                     LinkRepository linkRepository,
                     GatewayServiceProperties gatewayServiceProperties,
                     LinkServiceProperties serviceProperties,
                     @Qualifier("linkResults") CacheAdapter<String, String> linkResults,
                     ServiceAuthentication serviceAuthentication,
                     LinkTokenVerifier linkTokenVerifier) {
        return new Link(
                new LinkServiceClient(builder.build(), serviceAuthentication, gatewayServiceProperties),
                linkRepository,
                serviceAuthentication,
                serviceProperties,
                linkResults,
                linkTokenVerifier);
    }

    @Bean
    public DiscoveryServiceClient discoveryServiceClient(@Qualifier("customBuilder") WebClient.Builder builder,
                                                         ServiceAuthentication serviceAuthentication,
                                                         GatewayServiceProperties gatewayServiceProperties) {
        return new DiscoveryServiceClient(builder.build(),
                serviceAuthentication::authenticate,
                gatewayServiceProperties);
    }

    @Bean
    public Discovery discovery(DiscoveryRepository discoveryRepository,
                               CentralRegistry centralRegistry,
                               DiscoveryServiceClient discoveryServiceClient,
                               UserServiceClient userServiceClient,
                               LinkServiceProperties linkServiceProperties,
                               @Qualifier("linkResults") CacheAdapter<String, String> linkResults) {
        return new Discovery(userServiceClient,
                discoveryServiceClient,
                discoveryRepository,
                centralRegistry,
                linkServiceProperties,
                linkResults);
    }

    @Bean
    public UserAuthInitAction initAction() {
        return new UserAuthInitAction();
    }

    @Bean
    public UserAuthentication userAuthentication(
            UserAuthInitAction initAction
    ) {
        return new UserAuthentication(initAction);
    }

    @ConditionalOnProperty(value = "consentmanager.cacheMethod", havingValue = "guava", matchIfMissing = true)
    @Bean({"discoveryResults", "linkResults"})
    public CacheAdapter<String, String> createLoadingCacheAdapter() {
        return new LoadingCacheAdapter(createSessionCache(5));
    }

    public LoadingCache<String, String> createSessionCache(int duration) {
        return CacheBuilder
                .newBuilder()
                .expireAfterWrite(duration, TimeUnit.MINUTES)
                .build(new CacheLoader<String, String>() {
                    public String load(String key) {
                        return "";
                    }
                });
    }

    @ConditionalOnProperty(value = "consentmanager.cacheMethod", havingValue = "redis")
    @Bean({"discoveryResults", "linkResults"})
    public CacheAdapter<String, String> createRedisCacheAdapter(
            ReactiveRedisOperations<String, String> stringReactiveRedisOperations,
            RedisOptions redisOptions) {
        return new RedisCacheAdapter(stringReactiveRedisOperations, 5, redisOptions.getRetry());
    }
}
