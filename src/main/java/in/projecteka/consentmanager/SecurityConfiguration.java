package in.projecteka.consentmanager;

import in.projecteka.consentmanager.clients.properties.IdentityServiceProperties;
import in.projecteka.consentmanager.common.Authenticator;
import in.projecteka.consentmanager.user.SignUpService;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity httpSecurity,
            ReactiveAuthenticationManager authenticationManager,
            ServerSecurityContextRepository securityContextRepository) {
        final String[] WHITELISTED_URLS = {"/**.json",
                                           "/users/verify",
                                           "/users/permit",
                                           "/sessions",
                                           "/**.html",
                                           "/**.js",
                                           "/**.yaml",
                                           "/**.css",
                                           "/**.png"};
        httpSecurity.authorizeExchange().pathMatchers(WHITELISTED_URLS).permitAll();
        httpSecurity.httpBasic().disable().formLogin().disable().csrf().disable().logout().disable();
        httpSecurity.authorizeExchange().pathMatchers("/**").authenticated();
        return httpSecurity
                .authenticationManager(authenticationManager)
                .securityContextRepository(securityContextRepository)
                .build();
    }

    @Bean
    public ReactiveAuthenticationManager authenticationManager() {
        return new AuthenticationManager();
    }

    @Bean
    public Authenticator authenticator(WebClient.Builder builder, IdentityServiceProperties identityServiceProperties) {
        return new Authenticator(builder, identityServiceProperties);
    }

    @Bean
    public SecurityContextRepository contextRepository(ReactiveAuthenticationManager manager,
                                                       SignUpService signupService,
                                                       Authenticator authenticator) {
        return new SecurityContextRepository(manager, signupService, authenticator);
    }

    @AllArgsConstructor
    private static class SecurityContextRepository implements ServerSecurityContextRepository {
        private ReactiveAuthenticationManager manager;
        private SignUpService signupService;
        private Authenticator identityServiceClient;

        @Override
        public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
            throw new UnsupportedOperationException("No need right now!");
        }

        @Override
        public Mono<SecurityContext> load(ServerWebExchange exchange) {
            var token = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (isEmpty(token)) {
                return Mono.empty();
            }

            if (isSignUpRequest(exchange.getRequest().getPath().toString(), exchange.getRequest().getMethod())) {
                return checkSignUp(token);
            }

            if (isCentralRegistryAuthenticatedOnlyRequest(
                    exchange.getRequest().getPath().toString(),
                    exchange.getRequest().getMethod())) {
                return checkCentralRegistry(token);
            }

            return check(token);
        }

        private Mono<SecurityContext> checkCentralRegistry(String token) {
            return identityServiceClient.verify(token)
                    .flatMap(doesNotMatter -> {
                        var authToken = new UsernamePasswordAuthenticationToken(
                                token,
                                token,
                                new ArrayList<SimpleGrantedAuthority>());
                        return manager.authenticate(authToken).map(SecurityContextImpl::new);
                    });
        }

        private boolean isCentralRegistryAuthenticatedOnlyRequest(String url, HttpMethod method) {
            List<Map.Entry<String, HttpMethod>> patterns = new ArrayList<>();
            patterns.add(Map.entry("/consent-requests", HttpMethod.GET));
            patterns.add(Map.entry("/health-information/request", HttpMethod.POST));
            patterns.add(Map.entry("/consents/**", HttpMethod.GET));
            AntPathMatcher antPathMatcher = new AntPathMatcher();
            for (var pattern : patterns) {
                if (antPathMatcher.match(pattern.getKey(), url) && pattern.getValue().equals(method)) {
                    return true;
                }
            }
            return false;
        }

        private Mono<SecurityContext> check(String authToken) {
            return identityServiceClient.verify(authToken)
                    .flatMap(doesNotMatter -> {
                        var token = new UsernamePasswordAuthenticationToken(
                                authToken,
                                authToken,
                                new ArrayList<SimpleGrantedAuthority>());
                        return manager.authenticate(token).map(SecurityContextImpl::new);
                    });
        }

        private boolean isEmpty(String authToken) {
            return authToken == null || authToken.trim().equals("");
        }

        private Mono<SecurityContext> checkSignUp(String authToken) {
            if (!signupService.validateToken(authToken)) {
                return Mono.empty();
            }
            return Mono.just(new UsernamePasswordAuthenticationToken(
                    authToken,
                    authToken,
                    new ArrayList<SimpleGrantedAuthority>()))
                    .map(SecurityContextImpl::new);
        }

        private boolean isSignUpRequest(String url, HttpMethod httpMethod) {
            return ("/users").equals(url) && HttpMethod.POST.equals(httpMethod);
        }
    }

    private static class AuthenticationManager implements ReactiveAuthenticationManager {
        @Override
        public Mono<Authentication> authenticate(Authentication authentication) {
            var token = authentication.getCredentials().toString();
            var auth = new UsernamePasswordAuthenticationToken(token, token, new ArrayList<SimpleGrantedAuthority>());
            return Mono.just(auth);
        }
    }
}
