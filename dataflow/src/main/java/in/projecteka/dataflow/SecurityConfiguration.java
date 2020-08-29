package in.projecteka.dataflow;

import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import in.projecteka.library.common.GatewayTokenVerifier;
import lombok.AllArgsConstructor;
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

import static in.projecteka.dataflow.Constants.PATH_HEALTH_INFORMATION_NOTIFY;
import static in.projecteka.dataflow.Constants.PATH_HEALTH_INFORMATION_ON_REQUEST;
import static in.projecteka.dataflow.Constants.PATH_HEALTH_INFORMATION_REQUEST;
import static in.projecteka.library.clients.model.ClientError.unAuthorized;
import static in.projecteka.library.common.Role.GATEWAY;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.error;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    private static final List<Map.Entry<String, HttpMethod>> SERVICE_ONLY_URLS = new ArrayList<>();
    private static final String[] GATEWAY_APIS = new String[]{
            PATH_HEALTH_INFORMATION_ON_REQUEST,
            PATH_HEALTH_INFORMATION_REQUEST,
            PATH_HEALTH_INFORMATION_NOTIFY,
    };

    static {
        SERVICE_ONLY_URLS.add(Map.entry(PATH_HEALTH_INFORMATION_REQUEST, HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry(PATH_HEALTH_INFORMATION_NOTIFY, HttpMethod.POST));
        SERVICE_ONLY_URLS.add(Map.entry(PATH_HEALTH_INFORMATION_ON_REQUEST, HttpMethod.POST));
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity httpSecurity,
            ReactiveAuthenticationManager authenticationManager,
            ServerSecurityContextRepository securityContextRepository) {
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

    @Bean({"jwtProcessor"})
    public ConfigurableJWTProcessor<com.nimbusds.jose.proc.SecurityContext> getJWTProcessor() {
        return new DefaultJWTProcessor<>();
    }

    @Bean
    public SecurityContextRepository contextRepository(GatewayTokenVerifier gatewayTokenVerifier) {
        return new SecurityContextRepository(gatewayTokenVerifier);
    }

    @AllArgsConstructor
    private static class SecurityContextRepository implements ServerSecurityContextRepository {
        private final GatewayTokenVerifier gatewayTokenVerifier;

        @Override
        public Mono<Void> save(ServerWebExchange exchange, SecurityContext context) {
            throw new UnsupportedOperationException("No need right now!");
        }

        @Override
        public Mono<SecurityContext> load(ServerWebExchange exchange) {
            var requestPath = exchange.getRequest().getPath().toString();
            var requestMethod = exchange.getRequest().getMethod();
            if (isGatewayAuthenticationOnly(requestPath, requestMethod)) {
                return checkGateway(exchange.getRequest().getHeaders().getFirst(AUTHORIZATION))
                        .switchIfEmpty(error(unAuthorized()));
            }
            return empty();
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

        private boolean isGatewayAuthenticationOnly(String url, HttpMethod method) {
            AntPathMatcher antPathMatcher = new AntPathMatcher();
            return SERVICE_ONLY_URLS.stream()
                    .anyMatch(pattern ->
                            antPathMatcher.match(pattern.getKey(), url) && pattern.getValue().equals(method));
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
