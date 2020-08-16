package common.heartbeat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.clients.model.Error;
import in.projecteka.consentmanager.clients.model.ErrorCode;
import in.projecteka.consentmanager.common.heartbeat.Heartbeat;
import in.projecteka.consentmanager.common.heartbeat.model.HeartbeatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebFlux;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static common.TestBuilders.OBJECT_MAPPER;
import static in.projecteka.consentmanager.common.Constants.PATH_HEARTBEAT;
import static in.projecteka.consentmanager.common.Constants.SERVICE_DOWN;
import static in.projecteka.consentmanager.common.heartbeat.model.Status.DOWN;
import static in.projecteka.consentmanager.common.heartbeat.model.Status.UP;
import static java.time.LocalDateTime.now;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "6000")
class HeartbeatControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @SuppressWarnings("unused")
    @MockBean(name = "centralRegistryJWKSet")
    private JWKSet centralRegistryJWKSet;

    @SuppressWarnings("unused")
    @MockBean(name = "identityServiceJWKSet")
    private JWKSet identityServiceJWKSet;

    @MockBean
    private Heartbeat heartbeat;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    @Disabled
    void shouldGiveCMStatusAsUp() throws JsonProcessingException {
        var heartbeatResponse = HeartbeatResponse.builder().timeStamp(now()).status(UP).build();
        var heartbeatResponseJson = OBJECT_MAPPER.writeValueAsString(heartbeatResponse);
        when(heartbeat.getStatus()).thenReturn(Mono.just(heartbeatResponse));

        webTestClient.get()
                .uri(PATH_HEARTBEAT)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json(heartbeatResponseJson);
    }

    @Disabled
    @Test
    void shouldGiveCMStatusAsDown() throws JsonProcessingException {
        var heartbeatResponse = HeartbeatResponse.builder()
                .timeStamp(now())
                .status(DOWN)
                .error(Error.builder().code(ErrorCode.SERVICE_DOWN).message(SERVICE_DOWN).build())
                .build();
        var heartbeatResponseJson = OBJECT_MAPPER.writeValueAsString(heartbeatResponse);
        when(heartbeat.getStatus()).thenReturn(Mono.just(heartbeatResponse));

        webTestClient.get()
                .uri(PATH_HEARTBEAT)
                .exchange()
                .expectStatus()
                .is5xxServerError()
                .expectBody()
                .json(heartbeatResponseJson);
    }
}

