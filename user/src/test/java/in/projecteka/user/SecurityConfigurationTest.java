package in.projecteka.user;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.library.common.GatewayTokenVerifier;
import in.projecteka.library.common.RequestValidator;
import in.projecteka.library.common.ServiceCaller;
import in.projecteka.user.properties.UserServiceProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static in.projecteka.library.common.Role.GATEWAY;
import static in.projecteka.user.Constants.PATH_FIND_PATIENT;
import static in.projecteka.user.TestBuilders.patientRequest;
import static in.projecteka.user.TestBuilders.string;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "60000")
class SecurityConfigurationTest {
    @MockBean(name = "gatewayJWKSet")
    JWKSet centralRegistryJWKSet;

    @MockBean(name = "identityServiceJWKSet")
    JWKSet identityServiceJWKSet;

    @MockBean
    GatewayTokenVerifier gatewayTokenVerifier;

    @MockBean
    private PinVerificationTokenService pinVerificationTokenService;

    @MockBean
    private RequestValidator validator;

    @MockBean
    private UserServiceProperties userServiceProperties;

    @Autowired
    WebTestClient webTestClient;

    @Test
    void return401UnAuthorized() {
        webTestClient
                .post()
                .uri(PATH_FIND_PATIENT)
                .contentType(APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isEqualTo(UNAUTHORIZED);
    }

    @Test
    void returnForbiddenError() {
        var token = string();
        var caller = ServiceCaller.builder().roles(List.of()).build();
        when(gatewayTokenVerifier.verify(token)).thenReturn(Mono.just(caller));

        webTestClient
                .post()
                .uri(PATH_FIND_PATIENT)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .bodyValue("{}")
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void return202Accepted() {
        var token = string();
        var caller = ServiceCaller.builder().roles(List.of(GATEWAY)).build();
        var patientRequest = patientRequest().build();
        when(validator.validate(anyString(), any(LocalDateTime.class))).thenReturn(Mono.just(Boolean.TRUE));
        when(validator.put(anyString(), any(LocalDateTime.class))).thenReturn(Mono.empty());
        when(gatewayTokenVerifier.verify(token)).thenReturn(Mono.just(caller));

        webTestClient
                .post()
                .uri(PATH_FIND_PATIENT)
                .contentType(APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .bodyValue(patientRequest)
                .exchange()
                .expectStatus()
                .isAccepted();
    }
}