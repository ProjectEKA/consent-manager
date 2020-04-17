package in.projecteka.consentmanager;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.common.Authenticator;
import in.projecteka.consentmanager.common.CentralRegistryTokenVerifier;
import in.projecteka.consentmanager.consent.PinVerificationTokenService;
import in.projecteka.consentmanager.user.SignUpService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    private static final List<Map.Entry<String, HttpMethod>> SERVICE_ONLY_URLS = new ArrayList<>() {
        {
            add(Map.entry("/consent-requests", HttpMethod.POST));
            add(Map.entry("/health-information/request", HttpMethod.POST));
            add(Map.entry("/health-information/notification", HttpMethod.POST));
            add(Map.entry("/consents/**", HttpMethod.GET));
            add(Map.entry("/users/**", HttpMethod.GET));
        }
    };

    private static final List<Map.Entry<String, HttpMethod>> PIN_VERIFICATION_URLS = new ArrayList<>() {
        {
            add(Map.entry("/consent-requests/**/approve", HttpMethod.POST));
            add(Map.entry("/consents/revoke", HttpMethod.POST));
        }
    };

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
    public Authenticator authenticator(@Qualifier("identityServiceJWKSet") JWKSet jwkSet) {
        return new Authenticator(jwkSet);
    }

    @Bean
    public SecurityContextRepository contextRepository(SignUpService signupService,
                                                       Authenticator authenticator,
                                                       PinVerificationTokenService pinVerificationTokenService,
                                                       CentralRegistryTokenVerifier centralRegistryTokenVerifier) {
        return new SecurityContextRepository(signupService,
                authenticator,
                pinVerificationTokenService,
                centralRegistryTokenVerifier);
    }

    @AllArgsConstructor
    private static class SecurityContextRepository implements ServerSecurityContextRepository {
        private final SignUpService signupService;
        private final Authenticator identityServiceClient;
        private final PinVerificationTokenService pinVerificationTokenService;
        private final CentralRegistryTokenVerifier centralRegistryTokenVerifier;

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
            if(isGrantOrRevokeConsentRequest(exchange.getRequest().getPath().toString(), exchange.getRequest().getMethod())){
                return validateGrantOrRevokeConsentRequest(token);
            }
            if (isCentralRegistryAuthenticatedOnlyRequest(
                    exchange.getRequest().getPath().toString(),
                    exchange.getRequest().getMethod())) {
                return checkCentralRegistry(token);
            }

            return check(token);
        }

        private Mono<SecurityContext> checkCentralRegistry(String token) {
            return centralRegistryTokenVerifier.verify(token)
                    .map(caller ->
                            new UsernamePasswordAuthenticationToken(
                                    caller,
                                    token,
                                    new ArrayList<SimpleGrantedAuthority>()))
                    .map(SecurityContextImpl::new);
        }

        private Mono<SecurityContext> validateGrantOrRevokeConsentRequest(String token) {
            return pinVerificationTokenService.validateToken(token)
                    .map(caller -> new UsernamePasswordAuthenticationToken(
                            caller,
                            token,
                            new ArrayList<SimpleGrantedAuthority>()))
                    .map(SecurityContextImpl::new);
        }

        private boolean isCentralRegistryAuthenticatedOnlyRequest(String url, HttpMethod method) {
            AntPathMatcher antPathMatcher = new AntPathMatcher();
            return SERVICE_ONLY_URLS.stream()
                    .anyMatch(pattern ->
                            antPathMatcher.match(pattern.getKey(), url) && pattern.getValue().equals(method));
        }

        private boolean isGrantOrRevokeConsentRequest(String url, HttpMethod method) {
            AntPathMatcher antPathMatcher = new AntPathMatcher();
            return PIN_VERIFICATION_URLS.stream()
                    .anyMatch(pattern ->
                            antPathMatcher.match(pattern.getKey(), url) && pattern.getValue().equals(method));
        }

        private Mono<SecurityContext> check(String authToken) {
            return identityServiceClient.verify(authToken)
                    .map(caller -> new UsernamePasswordAuthenticationToken(
                            caller,
                            authToken,
                            new ArrayList<SimpleGrantedAuthority>()))
                    .map(SecurityContextImpl::new);
        }

        private boolean isEmpty(String authToken) {
            return authToken == null || authToken.trim().equals("");
        }

        private Mono<SecurityContext> checkSignUp(String authToken) {
            return Mono.just(authToken)
                    .filterWhen(signupService::validateToken)
                    .flatMap(token -> Mono.just(new UsernamePasswordAuthenticationToken(
                            token,
                            token,
                            new ArrayList<SimpleGrantedAuthority>()))
                            .map(SecurityContextImpl::new));
        }

        private boolean isSignUpRequest(String url, HttpMethod httpMethod) {
            return ("/patients/profile").equals(url) && HttpMethod.POST.equals(httpMethod);
        }
    }

    private static class AuthenticationManager implements ReactiveAuthenticationManager {
        @Override
        public Mono<Authentication> authenticate(Authentication authentication) {
            var token = authentication.getCredentials().toString();
            var auth = new UsernamePasswordAuthenticationToken(
                    authentication.getPrincipal(),
                    token,
                    new ArrayList<SimpleGrantedAuthority>());
            return Mono.just(auth);
        }
    }
}
