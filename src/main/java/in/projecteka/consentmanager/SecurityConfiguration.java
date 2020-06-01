package in.projecteka.consentmanager;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import in.projecteka.consentmanager.common.Authenticator;
import in.projecteka.consentmanager.common.CentralRegistryTokenVerifier;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.consent.PinVerificationTokenService;
import in.projecteka.consentmanager.user.SignUpService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
import java.util.Optional;

import static in.projecteka.consentmanager.common.Constants.SCOPE_CONSENT_APPROVE;
import static in.projecteka.consentmanager.common.Constants.SCOPE_CONSENT_REVOKE;
import static in.projecteka.consentmanager.common.Constants.SCOPE_CHANGE_PIN;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    private static final List<Map.Entry<String, HttpMethod>> SERVICE_ONLY_URLS = new ArrayList<>();
    private static final List<RequestMatcher> PIN_VERIFICATION_MATCHERS = new ArrayList<>();

    @RequiredArgsConstructor
    @AllArgsConstructor
    @Getter
    private static class RequestMatcher {
        private final String pattern;
        private final HttpMethod httpMethod;
        private String scope;
    }

    static {
        SERVICE_ONLY_URLS.add(Map.entry("/users/**", HttpMethod.GET));
        SERVICE_ONLY_URLS.add(Map.entry("/consents/**", HttpMethod.GET));
        SERVICE_ONLY_URLS.add(Map.entry("/health-information/notification", HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry("/health-information/request", HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry("/consent-requests", HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry("/v1/care-contexts/on-discover", HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry("/v1/links/link/on-init", HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry("/v1/links/link/on-confirm", HttpMethod.POST));
        RequestMatcher approveMatcher = new RequestMatcher("/consent-requests/**/approve", HttpMethod.POST, SCOPE_CONSENT_APPROVE);
        RequestMatcher revokeMatcher = new RequestMatcher("/consents/revoke", HttpMethod.POST, SCOPE_CONSENT_REVOKE);
        RequestMatcher changePinMatcher = new RequestMatcher("/patients/change-pin", HttpMethod.POST, SCOPE_CHANGE_PIN);
        PIN_VERIFICATION_MATCHERS.add(approveMatcher);
        PIN_VERIFICATION_MATCHERS.add(revokeMatcher);
        PIN_VERIFICATION_MATCHERS.add(changePinMatcher);
    }


    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity httpSecurity,
            ReactiveAuthenticationManager authenticationManager,
            ServerSecurityContextRepository securityContextRepository) {

        final String[] whitelistedUrls = {"/**.json",
                                          "/ValueSet/**.json",
                                          "/patients/generateotp",
                                          "/patients/verifyotp",
                                          "/patients/profile/loginmode",
                                          "/patients/profile/recovery-init",
                                          "/patients/profile/recovery-confirm",
                                          "/users/verify",
                                          "/users/permit",
                                          "/otpsession/verify",
                                          "/otpsession/permit",
                                          "/sessions",
                                          "/**.html",
                                          "/**.js",
                                          "/**.yaml",
                                          "/**.css",
                                          "/**.png"};
        httpSecurity.authorizeExchange().pathMatchers(whitelistedUrls).permitAll();
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
    public Authenticator authenticator(@Qualifier("identityServiceJWKSet") JWKSet jwkSet, CacheAdapter<String, String> blacklistedTokens, ConfigurableJWTProcessor<com.nimbusds.jose.proc.SecurityContext> jwtProcessor) {
        return new Authenticator(jwkSet, blacklistedTokens, jwtProcessor);
    }

    @Bean({"jwtProcessor"})
    public ConfigurableJWTProcessor<com.nimbusds.jose.proc.SecurityContext> getJWTProcessor() {
        return new DefaultJWTProcessor<>();
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
            String requestPath = exchange.getRequest().getPath().toString();
            HttpMethod requestMethod = exchange.getRequest().getMethod();
            if (isSignUpRequest(requestPath, requestMethod)) {
                return checkSignUp(token);
            }
            if (isPinVerificationRequest(requestPath, requestMethod)) {
                Optional<String> validScope = getScope(requestPath,requestMethod);
                if(validScope.isEmpty()) {
                    return Mono.empty();//TODO handle better?
                }
                return validatePinVerificationRequest(token, validScope.get());
            }
            if (isCentralRegistryAuthenticatedOnlyRequest(
                    requestPath,
                    requestMethod)) {
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

        private Mono<SecurityContext> validatePinVerificationRequest(String token, String validScope) {
            return pinVerificationTokenService.validateToken(token, validScope)
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

        private boolean isPinVerificationRequest(String url, HttpMethod method) {
            AntPathMatcher antPathMatcher = new AntPathMatcher();
            return PIN_VERIFICATION_MATCHERS.stream()
                    .anyMatch(matcher ->
                            antPathMatcher.match(matcher.getPattern(), url) && matcher.getHttpMethod().equals(method));
        }

        private Optional<String> getScope(String url, HttpMethod method) {
            AntPathMatcher antPathMatcher = new AntPathMatcher();
            return PIN_VERIFICATION_MATCHERS.stream()
                    .filter(matcher ->
                            antPathMatcher.match(matcher.getPattern(), url) && matcher.getHttpMethod().equals(method))
                    .findFirst()
                    .map(RequestMatcher::getScope);
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
            boolean isSignUp = (("/patients/profile").equals(url) && HttpMethod.POST.equals(httpMethod)) ||
                    (("/patients/profile/reset-password").equals(url) && HttpMethod.PUT.equals(httpMethod));
            return isSignUp;
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
