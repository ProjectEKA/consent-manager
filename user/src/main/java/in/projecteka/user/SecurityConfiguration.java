package in.projecteka.user;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import in.projecteka.library.common.Authenticator;
import in.projecteka.library.common.GatewayTokenVerifier;
import in.projecteka.library.common.cache.CacheAdapter;
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

import static in.projecteka.library.clients.model.ClientError.unAuthorized;
import static in.projecteka.library.common.Constants.PATH_HEARTBEAT;
import static in.projecteka.library.common.Constants.SCOPE_CHANGE_PIN;
import static in.projecteka.library.common.Role.GATEWAY;
import static in.projecteka.user.Constants.APP_PATH_CREATE_USER;
import static in.projecteka.user.Constants.APP_PATH_RESET_PASSWORD;
import static in.projecteka.user.Constants.APP_PATH_RESET_PIN;
import static in.projecteka.user.Constants.BASE_PATH_PATIENTS_APIS;
import static in.projecteka.user.Constants.PATH_FIND_PATIENT;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.error;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    private static final List<Map.Entry<String, HttpMethod>> SERVICE_ONLY_URLS = new ArrayList<>();
    private static final List<Map.Entry<String, HttpMethod>> TEMP_TOKEN_URLS = new ArrayList<>();
    private static final List<RequestMatcher> PIN_VERIFICATION_MATCHERS = new ArrayList<>();
    private static final String[] GATEWAY_APIS = new String[]{
            PATH_FIND_PATIENT,
//            PATH_HIP_LINK_USER_AUTH_INIT,
//            USERS_AUTH_CONFIRM
    };

    static {

        SERVICE_ONLY_URLS.add(Map.entry(PATH_FIND_PATIENT, HttpMethod.POST));
//        SERVICE_ONLY_URLS.add(Map.entry(PATH_HIP_ADD_CONTEXTS, HttpMethod.POST));
//        SERVICE_ONLY_URLS.add(Map.entry(USERS_AUTH_CONFIRM, HttpMethod.POST));
        RequestMatcher changePinMatcher = new RequestMatcher("/patients/change-pin",
                HttpMethod.POST,
                SCOPE_CHANGE_PIN);

        PIN_VERIFICATION_MATCHERS.add(changePinMatcher);

        TEMP_TOKEN_URLS.add(Map.entry(BASE_PATH_PATIENTS_APIS + APP_PATH_CREATE_USER, HttpMethod.POST));
        TEMP_TOKEN_URLS.add(Map.entry(BASE_PATH_PATIENTS_APIS + APP_PATH_RESET_PASSWORD, HttpMethod.PUT));
        TEMP_TOKEN_URLS.add(Map.entry(BASE_PATH_PATIENTS_APIS + APP_PATH_RESET_PIN, HttpMethod.PUT));
    }

    private static final String[] ALLOWED_LIST_URLS = new String[]{"/patients/generateotp",
                                                                   "/patients/verifyotp",
                                                                   "/patients/profile/loginmode",
                                                                   "/patients/profile/recovery-init",
                                                                   "/patients/profile/recovery-confirm",
                                                                   "/users/verify",
                                                                   "/users/permit",
                                                                   "/otpsession/verify",
                                                                   "/otpsession/permit",
                                                                   "/sessions",
                                                                   PATH_HEARTBEAT};

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
                                       ConfigurableJWTProcessor<SecurityContext> jwtProcessor) {
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
            @Value("${user.authorization.header}") String authorizationHeader) {
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
        public Mono<Void> save(ServerWebExchange exchange, org.springframework.security.core.context.SecurityContext context) {
            throw new UnsupportedOperationException("No need right now!");
        }

        @Override
        public Mono<org.springframework.security.core.context.SecurityContext> load(ServerWebExchange exchange) {
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
            if (isPostOTPVerificationRequest(requestPath, requestMethod)) {
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

        private Mono<org.springframework.security.core.context.SecurityContext> checkGateway(String token) {
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

        private Mono<org.springframework.security.core.context.SecurityContext> validatePinVerificationRequest(
                String token,
                String validScope) {
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

        private Mono<org.springframework.security.core.context.SecurityContext> check(String authToken) {
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

        private Mono<org.springframework.security.core.context.SecurityContext> checkSignUp(String authToken) {
            return Mono.just(authToken)
                    .filterWhen(signupService::validateToken)
                    .flatMap(token -> Mono.just(new UsernamePasswordAuthenticationToken(
                            token,
                            token,
                            new ArrayList<>()))
                            .map(SecurityContextImpl::new));
        }

        private boolean isPostOTPVerificationRequest(String url, HttpMethod httpMethod) {
            var antPathMatcher = new AntPathMatcher();
            return TEMP_TOKEN_URLS.stream()
                    .anyMatch(pattern ->
                            antPathMatcher.match(pattern.getKey(), url) && pattern.getValue().equals(httpMethod));
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
