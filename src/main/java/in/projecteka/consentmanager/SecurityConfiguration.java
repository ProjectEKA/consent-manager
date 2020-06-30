package in.projecteka.consentmanager;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import in.projecteka.consentmanager.common.Authenticator;
import in.projecteka.consentmanager.common.GatewayTokenVerifier;
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

import static in.projecteka.consentmanager.common.Constants.SCOPE_CHANGE_PIN;
import static in.projecteka.consentmanager.common.Constants.SCOPE_CONSENT_APPROVE;
import static in.projecteka.consentmanager.common.Constants.SCOPE_CONSENT_REVOKE;
import static in.projecteka.consentmanager.common.Constants.V_1_CARE_CONTEXTS_ON_DISCOVER;
import static in.projecteka.consentmanager.common.Constants.V_1_CONSENT_REQUESTS_INIT;
import static in.projecteka.consentmanager.common.Constants.V_1_CONSENTS_FETCH;
import static in.projecteka.consentmanager.common.Constants.V_1_HEALTH_INFORMATION_REQUEST;
import static in.projecteka.consentmanager.common.Constants.V_1_LINKS_LINK_ON_CONFIRM;
import static in.projecteka.consentmanager.common.Constants.V_1_LINKS_LINK_ON_INIT;
import static in.projecteka.consentmanager.common.Constants.V_1_PATIENTS_FIND;
import static in.projecteka.consentmanager.common.Constants.V_1_HEALTH_INFORMATION_NOTIFY;
import static in.projecteka.consentmanager.common.Constants.V_1_HEALTH_INFORMATION_ON_REQUEST;
import static in.projecteka.consentmanager.common.Constants.V_1_HIP_CONSENT_ON_NOTIFY;
import static in.projecteka.consentmanager.common.Role.GATEWAY;
import static java.util.stream.Collectors.toList;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    private static final List<Map.Entry<String, HttpMethod>> SERVICE_ONLY_URLS = new ArrayList<>();
    private static final List<RequestMatcher> PIN_VERIFICATION_MATCHERS = new ArrayList<>();
    private static final String[] GATEWAY_APIS = new String[]{
            V_1_CARE_CONTEXTS_ON_DISCOVER,
            V_1_CONSENT_REQUESTS_INIT,
            V_1_CONSENTS_FETCH,
            V_1_PATIENTS_FIND,
            V_1_LINKS_LINK_ON_INIT,
            V_1_LINKS_LINK_ON_CONFIRM,
            V_1_HEALTH_INFORMATION_ON_REQUEST,
            V_1_LINKS_LINK_ON_CONFIRM,
            V_1_HEALTH_INFORMATION_REQUEST,
            V_1_HEALTH_INFORMATION_NOTIFY,
            V_1_HIP_CONSENT_ON_NOTIFY
    };

    static {
        SERVICE_ONLY_URLS.add(Map.entry("/users/**", HttpMethod.GET));
        SERVICE_ONLY_URLS.add(Map.entry("/consents/**", HttpMethod.GET));
        SERVICE_ONLY_URLS.add(Map.entry("/health-information/notification", HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry("/health-information/request", HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry("/consent-requests", HttpMethod.POST));

        SERVICE_ONLY_URLS.add(Map.entry(V_1_CONSENT_REQUESTS_INIT, HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry(V_1_CARE_CONTEXTS_ON_DISCOVER, HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry(V_1_LINKS_LINK_ON_INIT, HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry(V_1_LINKS_LINK_ON_CONFIRM, HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry(V_1_PATIENTS_FIND, HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry(V_1_CONSENTS_FETCH, HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry(V_1_HEALTH_INFORMATION_REQUEST, HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry(V_1_HEALTH_INFORMATION_NOTIFY, HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry(V_1_HEALTH_INFORMATION_ON_REQUEST, HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry(V_1_HIP_CONSENT_ON_NOTIFY, HttpMethod.POST));

        RequestMatcher approveMatcher = new RequestMatcher("/consent-requests/**/approve",
                HttpMethod.POST,
                SCOPE_CONSENT_APPROVE);
        RequestMatcher revokeMatcher = new RequestMatcher("/consents/revoke",
                HttpMethod.POST,
                SCOPE_CONSENT_REVOKE);
        RequestMatcher changePinMatcher = new RequestMatcher("/patients/change-pin",
                HttpMethod.POST,
                SCOPE_CHANGE_PIN);

        PIN_VERIFICATION_MATCHERS.add(approveMatcher);
        PIN_VERIFICATION_MATCHERS.add(revokeMatcher);
        PIN_VERIFICATION_MATCHERS.add(changePinMatcher);
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity httpSecurity,
            ReactiveAuthenticationManager authenticationManager,
            ServerSecurityContextRepository securityContextRepository) {

        final String[] allowedListUrls = {"/**.json",
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
                "/v1/heartbeat",
                "/**.html",
                "/**.js",
                "/**.yaml",
                "/**.css",
                "/**.png"};
        httpSecurity.authorizeExchange().pathMatchers(allowedListUrls).permitAll();
        httpSecurity.httpBasic().disable().formLogin().disable().csrf().disable().logout().disable();
        httpSecurity
                .authorizeExchange()
                .pathMatchers(GATEWAY_APIS).hasAnyRole(GATEWAY.name())
                .pathMatchers("/**")
                .authenticated();
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
    public Authenticator authenticator(@Qualifier("identityServiceJWKSet") JWKSet jwkSet,
                                       CacheAdapter<String, String> blockListedTokens,
                                       ConfigurableJWTProcessor<com.nimbusds.jose.proc.SecurityContext> jwtProcessor) {
        return new Authenticator(jwkSet, blockListedTokens, jwtProcessor);
    }

    @Bean({"jwtProcessor"})
    public ConfigurableJWTProcessor<com.nimbusds.jose.proc.SecurityContext> getJWTProcessor() {
        return new DefaultJWTProcessor<>();
    }

    @Bean
    public SecurityContextRepository contextRepository(SignUpService signupService,
                                                       Authenticator authenticator,
                                                       PinVerificationTokenService pinVerificationTokenService,
                                                       GatewayTokenVerifier gatewayTokenVerifier) {
        return new SecurityContextRepository(signupService,
                authenticator,
                pinVerificationTokenService,
                gatewayTokenVerifier);
    }

    @RequiredArgsConstructor
    @AllArgsConstructor
    @Getter
    private static class RequestMatcher {
        private final String pattern;
        private final HttpMethod httpMethod;
        private String scope;
    }

    @AllArgsConstructor
    private static class SecurityContextRepository implements ServerSecurityContextRepository {
        private final SignUpService signupService;
        private final Authenticator identityServiceClient;
        private final PinVerificationTokenService pinVerificationTokenService;
        private final GatewayTokenVerifier gatewayTokenVerifier;

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
                Optional<String> validScope = getScope(requestPath, requestMethod);
                if (validScope.isEmpty()) {
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
            return gatewayTokenVerifier.verify(token)
                    .map(serviceCaller -> {
                        var authorities = serviceCaller.getRoles()
                                .stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name().toUpperCase()))
                                .collect(toList());
                        return new UsernamePasswordAuthenticationToken(serviceCaller, token, authorities);
                    })
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
            return (("/patients/profile").equals(url) && HttpMethod.POST.equals(httpMethod)) ||
                    (("/patients/profile/reset-password").equals(url) && HttpMethod.PUT.equals(httpMethod));
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
