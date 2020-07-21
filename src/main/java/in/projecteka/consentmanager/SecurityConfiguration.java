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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

import static in.projecteka.consentmanager.clients.ClientError.unAuthorized;
import static in.projecteka.consentmanager.common.Constants.SCOPE_CHANGE_PIN;
import static in.projecteka.consentmanager.common.Constants.PATH_HEARTBEAT;
import static in.projecteka.consentmanager.common.Constants.SCOPE_CONSENT_APPROVE;
import static in.projecteka.consentmanager.common.Constants.SCOPE_CONSENT_REVOKE;
import static in.projecteka.consentmanager.common.Role.GATEWAY;
import static in.projecteka.consentmanager.consent.Constants.PATH_CONSENTS_FETCH;
import static in.projecteka.consentmanager.consent.Constants.PATH_CONSENT_REQUESTS_INIT;
import static in.projecteka.consentmanager.consent.Constants.PATH_HIP_CONSENT_ON_NOTIFY;
import static in.projecteka.consentmanager.dataflow.Constants.PATH_HEALTH_INFORMATION_NOTIFY;
import static in.projecteka.consentmanager.dataflow.Constants.PATH_HEALTH_INFORMATION_ON_REQUEST;
import static in.projecteka.consentmanager.dataflow.Constants.PATH_HEALTH_INFORMATION_REQUEST;
import static in.projecteka.consentmanager.link.Constants.PATH_CARE_CONTEXTS_ON_DISCOVER;
import static in.projecteka.consentmanager.link.Constants.PATH_LINK_ON_CONFIRM;
import static in.projecteka.consentmanager.link.Constants.PATH_LINK_ON_INIT;
import static in.projecteka.consentmanager.user.Constants.PATH_FIND_PATIENT;
import static in.projecteka.consentmanager.user.Constants.HAS_ACCOUNT_UPDATE;
import static in.projecteka.consentmanager.user.Constants.GENERATE_AADHAR_OTP;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.error;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    private static final List<Map.Entry<String, HttpMethod>> SERVICE_ONLY_URLS = new ArrayList<>();
    private static final List<RequestMatcher> PIN_VERIFICATION_MATCHERS = new ArrayList<>();
    private static final String[] GATEWAY_APIS = new String[]{
            PATH_CARE_CONTEXTS_ON_DISCOVER,
            PATH_CONSENT_REQUESTS_INIT,
            PATH_CONSENTS_FETCH,
            PATH_FIND_PATIENT,
            PATH_LINK_ON_INIT,
            PATH_LINK_ON_CONFIRM,
            PATH_HEALTH_INFORMATION_ON_REQUEST,
            PATH_HEALTH_INFORMATION_REQUEST,
            PATH_HEALTH_INFORMATION_NOTIFY,
            PATH_HIP_CONSENT_ON_NOTIFY
    };

    static {
        // Deprecated
        SERVICE_ONLY_URLS.add(Map.entry("/users/**", HttpMethod.GET));
        SERVICE_ONLY_URLS.add(Map.entry("/consents/**", HttpMethod.GET));
        SERVICE_ONLY_URLS.add(Map.entry("/health-information/notification", HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry("/health-information/request", HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry("/consent-requests", HttpMethod.POST));
        // Deprecated

        SERVICE_ONLY_URLS.add(Map.entry(PATH_CONSENT_REQUESTS_INIT, HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry(PATH_CARE_CONTEXTS_ON_DISCOVER, HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry(PATH_LINK_ON_INIT, HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry(PATH_LINK_ON_CONFIRM, HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry(PATH_FIND_PATIENT, HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry(PATH_CONSENTS_FETCH, HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry(PATH_HEALTH_INFORMATION_REQUEST, HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry(PATH_HEALTH_INFORMATION_NOTIFY, HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry(PATH_HEALTH_INFORMATION_ON_REQUEST, HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry(PATH_HIP_CONSENT_ON_NOTIFY, HttpMethod.POST));

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

    private static final String[] ALLOWED_LIST_URLS = new String[]{"/**.json",
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
                                                                   PATH_HEARTBEAT,
                                                                   "/patients/profile/update-login-details",
                                                                   GENERATE_AADHAR_OTP,
                                                                   "/**.html",
                                                                   "/**.js",
                                                                   "/**.yaml",
                                                                   "/**.css",
                                                                   "/**.png"};

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity httpSecurity,
            ReactiveAuthenticationManager authenticationManager,
            ServerSecurityContextRepository securityContextRepository) {

        httpSecurity.authorizeExchange().pathMatchers(ALLOWED_LIST_URLS).permitAll();
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
    public SecurityContextRepository contextRepository(
            SignUpService signupService,
            Authenticator authenticator,
            PinVerificationTokenService pinVerificationTokenService,
            GatewayTokenVerifier gatewayTokenVerifier,
            @Value("${consentmanager.authorization.header}") String authorizationHeader) {
        return new SecurityContextRepository(signupService,
                authenticator,
                pinVerificationTokenService,
                gatewayTokenVerifier,
                authorizationHeader);
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
        private final String authorizationHeader;

        @Override
        public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
            throw new UnsupportedOperationException("No need right now!");
        }

        @Override
        public Mono<SecurityContext> load(ServerWebExchange exchange) {
            var requestPath = exchange.getRequest().getPath().toString();
            if (isAllowedList(requestPath)) {
                return empty();
            }

            var requestMethod = exchange.getRequest().getMethod();
            if (isGatewayAuthenticationOnly(requestPath, requestMethod)) {
                return checkGateway(exchange.getRequest().getHeaders().getFirst(AUTHORIZATION))
                        .switchIfEmpty(error(unAuthorized()));
            }

            var token = exchange.getRequest().getHeaders().getFirst(authorizationHeader);
            if (isEmpty(token)) {
                return error(unAuthorized());
            }
            if (isSignUpRequest(requestPath, requestMethod)) {
                return checkSignUp(token).switchIfEmpty(error(unAuthorized()));
            }
            if (isPinVerificationRequest(requestPath, requestMethod)) {
                Optional<String> validScope = getScope(requestPath, requestMethod);
                if (validScope.isEmpty()) {
                    return empty();//TODO handle better?
                }
                return validatePinVerificationRequest(token, validScope.get()).switchIfEmpty(error(unAuthorized()));
            }
            return check(token).switchIfEmpty(error(unAuthorized()));
        }

        private Mono<SecurityContext> checkGateway(String token) {
            return Mono.justOrEmpty(token)
                    .flatMap(gatewayTokenVerifier::verify)
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
                            new ArrayList<>()))
                    .map(SecurityContextImpl::new);
        }

        private boolean isAllowedList(String url) {
            AntPathMatcher antPathMatcher = new AntPathMatcher();
            return of(ALLOWED_LIST_URLS)
                    .anyMatch(pattern -> antPathMatcher.match(pattern, url));
        }

        private boolean isGatewayAuthenticationOnly(String url, HttpMethod method) {
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
                            new ArrayList<>()))
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
                            new ArrayList<>()))
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
                    new ArrayList<>());
            return Mono.just(auth);
        }
    }
}
