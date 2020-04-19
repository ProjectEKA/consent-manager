package in.projecteka.consentmanager.user;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.consent.ConceptValidator;
import in.projecteka.consentmanager.consent.ConsentRequestNotificationListener;
import in.projecteka.consentmanager.consent.HipConsentNotificationListener;
import in.projecteka.consentmanager.consent.HiuConsentNotificationListener;
import in.projecteka.consentmanager.dataflow.DataFlowBroadcastListener;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.user.TestBuilders.session;
import static in.projecteka.consentmanager.user.TestBuilders.signUpRequest;
import static in.projecteka.consentmanager.user.TestBuilders.string;
import static java.time.LocalDate.now;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@SuppressWarnings("unused")
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureWebTestClient
public class PatientControllerTest {

    @MockBean
    private UserService userService;

    @MockBean
    private DestinationsConfig destinationsConfig;

    @MockBean
    private HiuConsentNotificationListener hiuConsentNotificationListener;

    @MockBean
    private HipConsentNotificationListener hipConsentNotificationListener;

    @MockBean
    private DataFlowBroadcastListener dataFlowBroadcastListener;

    @SuppressWarnings("unused")
    @MockBean
    private ConsentRequestNotificationListener consentRequestNotificationListener;

    @MockBean
    private SignUpService signupService;

    @Autowired
    private WebTestClient webClient;

    @SuppressWarnings("unused")
    @MockBean(name = "centralRegistryJWKSet")
    private JWKSet centralRegistryJWKSet;

    @SuppressWarnings("unused")
    @MockBean(name = "identityServiceJWKSet")
    private JWKSet identityServiceJWKSet;

    @SuppressWarnings("unused")
    @MockBean
    private ConceptValidator conceptValidator;

    @Test
    public void createUser() {
        var signUpRequest = signUpRequest()
                .username("username@ncg")
                .name("RandomName")
                .password("@2Abaafasfas")
                .yearOfBirth(now().getYear())
                .build();
        var token = string();
        var sessionId = string();
        var session = session().build();
        when(signupService.sessionFrom(token)).thenReturn(sessionId);
        when(userService.create(signUpRequest, sessionId)).thenReturn(Mono.just(session));
        when(userService.getUserIdSuffix()).thenReturn("@ncg");
        when(signupService.validateToken(token)).thenReturn(Mono.just(true));
        when(signupService.removeOf(sessionId)).thenReturn(Mono.empty());

        webClient.post()
                .uri("/patients/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .body(BodyInserters.fromValue(signUpRequest))
                .exchange().expectStatus().isOk();
    }

    @Test
    public void returnBadRequestForUserCreation() {
        var signUpRequest = signUpRequest()
                .name("RandomName")
                .yearOfBirth(now().plusDays(1).getYear())
                .build();
        var token = string();
        var sessionId = string();
        var session = session().build();
        when(signupService.sessionFrom(token)).thenReturn(sessionId);
        when(userService.create(signUpRequest, sessionId)).thenReturn(Mono.just(session));
        when(userService.getUserIdSuffix()).thenReturn("@ncg");
        when(signupService.validateToken(token)).thenReturn(Mono.just(true));

        webClient.post()
                .uri("/patients/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, token)
                .body(BodyInserters.fromValue(signUpRequest))
                .exchange()
                .expectStatus()
                .is4xxClientError();
    }
}
