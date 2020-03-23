package in.projecteka.consentmanager.consent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.model.Error;
import in.projecteka.consentmanager.clients.model.ErrorCode;
import in.projecteka.consentmanager.clients.model.ErrorRepresentation;
import in.projecteka.consentmanager.common.Authenticator;
import in.projecteka.consentmanager.common.Caller;
import in.projecteka.consentmanager.consent.model.ConsentRepresentation;
import in.projecteka.consentmanager.consent.model.ConsentRequestDetail;
import in.projecteka.consentmanager.consent.model.ConsentStatus;
import in.projecteka.consentmanager.consent.model.RevokeRequest;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactRepresentation;
import in.projecteka.consentmanager.dataflow.DataFlowBroadcastListener;
import okhttp3.mockwebserver.MockWebServer;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static in.projecteka.consentmanager.consent.TestBuilders.consentArtefactRepresentation;
import static in.projecteka.consentmanager.consent.TestBuilders.string;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@AutoConfigureWebTestClient
@ContextConfiguration(initializers =
        in.projecteka.consentmanager.consent.ConsentArtefactUserJourneyTest.ContextInitializer.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ConsentArtefactUserJourneyTest {
    private static MockWebServer clientRegistryServer = new MockWebServer();
    private static MockWebServer userServer = new MockWebServer();

    @Autowired
    private WebTestClient webTestClient;

    @SuppressWarnings("unused")
    @MockBean
    private DestinationsConfig destinationsConfig;

    @SuppressWarnings("unused")
    @MockBean
    private HiuConsentNotificationListener hiuConsentNotificationListener;

    @MockBean
    private PinVerificationTokenService pinVerificationTokenService;

    @MockBean
    private HipConsentNotificationListener hipConsentNotificationListener;

    @SuppressWarnings("unused")
    @MockBean
    private DataFlowBroadcastListener dataFlowBroadcastListener;

    @SuppressWarnings("unused")
    @MockBean
    private ConsentRequestNotificationListener consentRequestNotificationListener;

    @MockBean
    private ConsentArtefactRepository consentArtefactRepository;

    @MockBean
    private ConsentRepresentation consentRepresentation;

    @MockBean
    private ConsentNotificationPublisher consentNotificationPublisher;

    @MockBean
    private ConsentRequestRepository repository;

    @MockBean
    private Authenticator authenticator;

    @SuppressWarnings("unused")
    @MockBean(name = "centralRegistryJWKSet")
    private JWKSet centralRegistryJWKSet;

    @SuppressWarnings("unused")
    @MockBean(name = "identityServiceJWKSet")
    private JWKSet identityServiceJWKSet;
    public static final String REVOKE_CONSENT_JSON = "{\n" +
            "  \"status\": \"GRANTED\",\n" +
            "  \"consentDetail\": {\n" +
            "    \"consentId\": \"10000005\",\n" +
            "    \"createdAt\": \"2020-03-14T10:51:05.466+0000\",\n" +
            "    \"purpose\": {\n" +
            "      \"text\": \"EPISODE_OF_CARE\",\n" +
            "      \"code\": \"EpisodeOfCare\",\n" +
            "      \"refUri\": null\n" +
            "    },\n" +
            "    \"patient\": {\n" +
            "      \"id\": \"ashok.kumar@ncg\"\n" +
            "    },\n" +
            "    \"hip\": null,\n" +
            "    \"hiu\": {\n" +
            "      \"id\": \"10000005\",\n" +
            "      \"name\": \"Max Health Care\"\n" +
            "    },\n" +
            "    \"requester\": {\n" +
            "      \"name\": \"Dr. Lakshmi\",\n" +
            "      \"identifier\": null\n" +
            "    },\n" +
            "    \"hiTypes\": [\"Observation\"],\n" +
            "    \"permission\": null,\n" +
            "    \"careContexts\": [\n" +
            "        {\n" +
            "          \"patientReference\": \"ashokkumar@max\",\n" +
            "          \"careContextReference\": \"ashokkumar.opdcontext\"\n" +
            "        }      \n" +
            "      ]\n" +
            "  },\n" +
            "  \"consentRequestId\": \"30d02f6d-de17-405e-b4ab-d31b2bb799d7\",\n" +
            "  \"dateModified\": \"\"\n" +
            "}";

    @AfterAll
    public static void tearDown() throws IOException {
        clientRegistryServer.shutdown();
        userServer.shutdown();
    }

    @Test
    public void shouldListConsentArtifacts() {
        var consentArtefact = consentArtefactRepresentation().build();
        var token = string();
        var patientId = consentArtefact.getConsentDetail().getPatient().getId();
        var consentRequestId = "request-id";
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller(patientId, false)));
        when(consentArtefactRepository.getConsentArtefacts(eq(consentRequestId)))
                .thenReturn(Flux.just(consentArtefact.getConsentDetail().getConsentId()));
        when(consentArtefactRepository.getConsentArtefact(eq(consentArtefact.getConsentDetail().getConsentId())))
                .thenReturn(Mono.just(consentArtefact));

        webTestClient.get()
                .uri("/consent-requests/" + consentRequestId + "/consent-artefacts")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<ConsentArtefactRepresentation>>() {
                })
                .value(value -> value.get(0).getConsentDetail(), Matchers.equalTo(consentArtefact.getConsentDetail()))
                .value(value -> value.get(0).getStatus(), Matchers.is(consentArtefact.getStatus()))
                .value(value -> value.get(0).getSignature(), Matchers.is(consentArtefact.getSignature()));
    }

    @Test
    public void shouldRevokeConsentArtefact() throws JsonProcessingException {
        var requestedConsentJson = "{\n" +
                "            \"status\": \"GRANTED\",\n" +
                "            \"createdAt\": \"2020-03-14T10:51:05.466+0000\",\n" +
                "            \"purpose\": {\n" +
                "                \"text\": \"EPISODE_OF_CARE\",\n" +
                "                \"code\": \"EpisodeOfCare\",\n" +
                "                \"refUri\": null\n" +
                "            },\n" +
                "            \"patient\": {\n" +
                "                \"id\": \"ashok.kumar@ncg\"\n" +
                "            },\n" +
                "            \"hip\": null,\n" +
                "            \"hiu\": {\n" +
                "                \"id\": \"10000005\",\n" +
                "                \"name\": \"Max Health Care\"\n" +
                "            },\n" +
                "            \"requester\": {\n" +
                "                \"name\": \"Dr. Lakshmi\",\n" +
                "                \"identifier\": null\n" +
                "            },\n" +
                "            \"hiTypes\": [\n" +
                "                \"Observation\"\n" +
                "            ],\n" +
                "            \"permission\": {\n" +
                "                \"accessMode\": \"VIEW\",\n" +
                "                \"dateRange\": {\n" +
                "                    \"from\": \"2020-03-14T10:50:45.032+0000\",\n" +
                "                    \"to\": \"2020-03-14T10:50:45.032+0000\"\n" +
                "                },\n" +
                "                \"dataExpiryAt\": \"2020-03-18T10:50:00.000+0000\",\n" +
                "                \"frequency\": {\n" +
                "                    \"unit\": \"HOUR\",\n" +
                "                    \"value\": 1,\n" +
                "                    \"repeats\": 0\n" +
                "                }\n" +
                "            },\n" +
                "            \"callBackUrl\": \"http://hiu:8003\",\n" +
                "            \"lastUpdated\": \"2020-03-14T12:00:52.091+0000\",\n" +
                "            \"id\": \"30d02f6d-de17-405e-b4ab-d31b2bb799d7\"\n" +
                "        }";
        var token = string();
        var consentRepresentation = new ObjectMapper().readValue(REVOKE_CONSENT_JSON, ConsentRepresentation.class);
        var consentRequestDetail = new ObjectMapper().readValue(requestedConsentJson, ConsentRequestDetail.class);
        String patientId = consentRepresentation.getConsentDetail().getPatient().getId();
        String consentId = "10000005";
        String requestBody = "{\"consents\": [\"10000005\"]}";

        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller("user", false)));
        when(pinVerificationTokenService.validateToken(token))
                .thenReturn(Mono.just(new Caller(patientId, false)));
        when(consentArtefactRepository.getConsentWithRequest(eq(consentId)))
                .thenReturn(Mono.just(consentRepresentation));
        when(repository.requestOf("30d02f6d-de17-405e-b4ab-d31b2bb799d7", ConsentStatus.GRANTED.toString(), patientId))
                .thenReturn(Mono.just(consentRequestDetail));
        when(consentArtefactRepository.updateStatus(
                "10000005",
                "30d02f6d-de17-405e-b4ab-d31b2bb799d7",
                ConsentStatus.REVOKED)).thenReturn(Mono.empty());
        when(consentNotificationPublisher.broadcastConsentArtefacts(any())).thenReturn(Mono.empty());

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/consents/revoke").build())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .body(BodyInserters.fromValue(requestBody))
                .exchange()
                .expectStatus().isOk()
                .expectBody(RevokeRequest.class);
    }

    @Test
    public void shouldNotRevokeConsentArtefactWhenTheRequestisNotInGrantedState() throws JsonProcessingException {
        var token = string();
        var consentRepresentation = new ObjectMapper().readValue(REVOKE_CONSENT_JSON, ConsentRepresentation.class);
        String patientId = consentRepresentation.getConsentDetail().getPatient().getId();
        String consentId = "10000005";
        String requestBody = "{\"consents\": [\"10000005\"]}";

        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller("user", false)));
        when(pinVerificationTokenService.validateToken(token))
                .thenReturn(Mono.just(new Caller(patientId, false)));
        when(consentArtefactRepository.getConsentWithRequest(eq(consentId)))
                .thenReturn(Mono.just(consentRepresentation));
        when(repository.requestOf("30d02f6d-de17-405e-b4ab-d31b2bb799d7", ConsentStatus.GRANTED.toString(), patientId))
                .thenReturn(Mono.error(ClientError.consentArtefactNotFound()));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path("/consents/revoke").build())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .body(BodyInserters.fromValue(requestBody))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(in.projecteka.consentmanager.clients.model.Error.class);
        verify(consentArtefactRepository, times(0)).updateStatus(any(), any(), any());
        verifyNoInteractions(consentNotificationPublisher);
    }

    @Test
    public void shouldThrowConsentArtifactNotFound() throws JsonProcessingException {
        var token = string();
        var consentArtefact = consentArtefactRepresentation().build();
        var patientId = consentArtefact.getConsentDetail().getPatient().getId();
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller(patientId, false)));
        var errorResponse = new ErrorRepresentation(new Error(ErrorCode.CONSENT_ARTEFACT_NOT_FOUND, "Cannot find the " +
                "consent artefact"));
        var errorResponseJson = new ObjectMapper().writeValueAsString(errorResponse);
        var consentRequestId = "request-id";
        when(consentArtefactRepository.getConsentArtefacts(eq(consentRequestId)))
                .thenReturn(Flux.empty());
        when(consentArtefactRepository.getConsentArtefact(eq(consentArtefact.getConsentDetail().getConsentId())))
                .thenReturn(Mono.just(consentArtefact));

        webTestClient.get()
                .uri("/consent-requests/" + consentRequestId + "/consent-artefacts")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .json(errorResponseJson);
    }

    @Test
    public void shouldThrowInvalidRequester() throws JsonProcessingException {
        var token = string();
        var anotherUser = string();
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller(anotherUser, false)));
        var consentArtefact = consentArtefactRepresentation().build();
        var errorResponse = new ErrorRepresentation(new Error(ErrorCode.CONSENT_ARTEFACT_FORBIDDEN,
                "Cannot retrieve Consent artefact. Forbidden"));
        var errorResponseJson = new ObjectMapper().writeValueAsString(errorResponse);
        var consentRequestId = "request-id";
        when(consentArtefactRepository.getConsentArtefacts(eq(consentRequestId)))
                .thenReturn(Flux.just(consentArtefact.getConsentDetail().getConsentId()));
        when(consentArtefactRepository.getConsentArtefact(eq(consentArtefact.getConsentDetail().getConsentId())))
                .thenReturn(Mono.just(consentArtefact));

        webTestClient.get()
                .uri("/consent-requests/" + consentRequestId + "/consent-artefacts")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody()
                .json(errorResponseJson);
    }

    public static class ContextInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues values =
                    TestPropertyValues.of(
                            Stream.of("consentmanager.clientregistry.url=" + clientRegistryServer.url(""),
                                    "consentmanager.userservice.url=" + userServer.url(""),
                                    "consentmanager.consentservice.maxPageSize=50"));
            values.applyTo(applicationContext);
        }
    }
}