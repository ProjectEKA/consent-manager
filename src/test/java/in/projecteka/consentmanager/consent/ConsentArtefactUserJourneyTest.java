package in.projecteka.consentmanager.consent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.repository.ConsentArtefactRepository;
import in.projecteka.consentmanager.dataflow.DataFlowBroadcastListener;
import in.projecteka.consentmanager.link.link.model.Error;
import in.projecteka.consentmanager.link.link.model.ErrorCode;
import in.projecteka.consentmanager.link.link.model.ErrorRepresentation;
import okhttp3.mockwebserver.MockWebServer;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

import static in.projecteka.consentmanager.consent.TestBuilders.consentArtefactRepresentation;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


@ExtendWith(SpringExtension.class)
@AutoConfigureWebTestClient
@ContextConfiguration(initializers =
        in.projecteka.consentmanager.consent.ConsentRequestUserJourneyTest.PropertyInitializer.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ConsentArtefactUserJourneyTest {
    private static MockWebServer clientRegistryServer = new MockWebServer();
    private static MockWebServer userServer = new MockWebServer();
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ConsentArtefactRepository consentArtefactRepository;

    @MockBean
    private DestinationsConfig destinationsConfig;

    @MockBean
    private ConsentArtefactBroadcastListener consentArtefactBroadcastListener;

    @MockBean
    private DataFlowBroadcastListener dataFlowBroadcastListener;

    @MockBean
    private ConsentRequestNotificationListener consentRequestNotificationListener;

    @AfterAll
    public static void tearDown() throws IOException {
        clientRegistryServer.shutdown();
        userServer.shutdown();
    }

    public static String encode(String authorizationHeader) {
        Base64.Encoder encoder = Base64.getEncoder();
        return new String(encoder.encode(authorizationHeader.getBytes()));
    }

    @Test
    public void shouldListConsentArtifacts() {
        ConsentArtefactRepresentation consentArtefact = consentArtefactRepresentation().build();
        String encodedPatientId = encode(consentArtefact.getConsentDetail().getPatient().getId());

        String consentRequestId = "request-id";
        when(consentArtefactRepository.getConsentArtefacts(eq(consentRequestId)))
                .thenReturn(Flux.just(consentArtefact.getConsentDetail().getConsentId()));
        when(consentArtefactRepository.getConsentArtefact(eq(consentArtefact.getConsentDetail().getConsentId())))
                .thenReturn(Mono.just(consentArtefact));

        webTestClient.get()
                .uri("/consent-requests/" + consentRequestId + "/consent-artefacts")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", encodedPatientId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<ConsentArtefactRepresentation>>() {
                })
                .value(value -> value.get(0).getConsentDetail(), Matchers.equalTo(consentArtefact.getConsentDetail()))
                .value(value -> value.get(0).getStatus(), Matchers.is(consentArtefact.getStatus()))
                .value(value -> value.get(0).getSignature(), Matchers.is(consentArtefact.getSignature()));
    }

    @Test
    public void shouldThrowConsentArtifactNotFound() throws JsonProcessingException {
        ConsentArtefactRepresentation consentArtefact = consentArtefactRepresentation().build();
        String encodedPatientId = encode(consentArtefact.getConsentDetail().getPatient().getId());
        var errorResponse = new ErrorRepresentation(new Error(ErrorCode.CONSENT_ARTEFACT_NOT_FOUND, "Cannot find the " +
                "consent artefact"));
        var errorResponseJson = new ObjectMapper().writeValueAsString(errorResponse);
        String consentRequestId = "request-id";
        when(consentArtefactRepository.getConsentArtefacts(eq(consentRequestId)))
                .thenReturn(Flux.empty());
        when(consentArtefactRepository.getConsentArtefact(eq(consentArtefact.getConsentDetail().getConsentId())))
                .thenReturn(Mono.just(consentArtefact));

        webTestClient.get()
                .uri("/consent-requests/" + consentRequestId + "/consent-artefacts")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", encodedPatientId)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .json(errorResponseJson);
    }

    @Test
    public void shouldThrowInvalidRequester() throws JsonProcessingException {
        ConsentArtefactRepresentation consentArtefact = consentArtefactRepresentation().build();
        String encodedPatientId = encode(consentArtefact.getConsentDetail().getPatient().getId());
        consentArtefact.getConsentDetail().getPatient().setId(encode("abc"));
        var errorResponse = new ErrorRepresentation(new Error(ErrorCode.CONSENT_ARTEFACT_FORBIDDEN, "Cannot retrieve " +
                "Consent " +
                "artefact. Forbidden"));
        var errorResponseJson = new ObjectMapper().writeValueAsString(errorResponse);
        String consentRequestId = "request-id";
        when(consentArtefactRepository.getConsentArtefacts(eq(consentRequestId)))
                .thenReturn(Flux.just(consentArtefact.getConsentDetail().getConsentId()));
        when(consentArtefactRepository.getConsentArtefact(eq(consentArtefact.getConsentDetail().getConsentId())))
                .thenReturn(Mono.just(consentArtefact));

        webTestClient.get()
                .uri("/consent-requests/" + consentRequestId + "/consent-artefacts")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", encodedPatientId)
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .json(errorResponseJson);
    }
}