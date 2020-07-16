package in.projecteka.consentmanager.consent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.clients.ConsentManagerClient;
import in.projecteka.consentmanager.clients.model.Provider;
import in.projecteka.consentmanager.common.Authenticator;
import in.projecteka.consentmanager.common.Caller;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.common.GatewayTokenVerifier;
import in.projecteka.consentmanager.common.RequestValidator;
import in.projecteka.consentmanager.common.ServiceCaller;
import in.projecteka.consentmanager.consent.model.AccessPeriod;
import in.projecteka.consentmanager.consent.model.ConsentPermission;
import in.projecteka.consentmanager.consent.model.ConsentPurpose;
import in.projecteka.consentmanager.consent.model.ConsentRequest;
import in.projecteka.consentmanager.consent.model.ConsentRequestDetail;
import in.projecteka.consentmanager.consent.model.HIPReference;
import in.projecteka.consentmanager.consent.model.HIType;
import in.projecteka.consentmanager.consent.model.HIUReference;
import in.projecteka.consentmanager.consent.model.ListResult;
import in.projecteka.consentmanager.consent.model.PatientReference;
import in.projecteka.consentmanager.consent.model.Requester;
import in.projecteka.consentmanager.consent.model.request.RequestedDetail;
import in.projecteka.consentmanager.consent.model.response.ConsentApprovalResponse;
import in.projecteka.consentmanager.consent.model.response.ConsentRequestsRepresentation;
import in.projecteka.consentmanager.consent.model.response.RequestCreatedRepresentation;
import in.projecteka.consentmanager.consent.policies.NhsPolicyCheck;
import in.projecteka.consentmanager.dataflow.DataFlowBroadcastListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static in.projecteka.consentmanager.common.Role.GATEWAY;
import static in.projecteka.consentmanager.consent.TestBuilders.OBJECT_MAPPER;
import static in.projecteka.consentmanager.consent.TestBuilders.consentRequestDetail;
import static in.projecteka.consentmanager.consent.TestBuilders.consentRequest;
import static in.projecteka.consentmanager.consent.TestBuilders.notificationMessage;
import static in.projecteka.consentmanager.consent.TestBuilders.string;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.DENIED;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.EXPIRED;
import static in.projecteka.consentmanager.consent.model.ConsentStatus.REQUESTED;
import static java.lang.String.format;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@AutoConfigureWebTestClient
@ContextConfiguration(initializers = ConsentRequestUserJourneyTest.PropertyInitializer.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConsentRequestUserJourneyTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private DestinationsConfig destinationsConfig;

    @MockBean
    private HipConsentNotificationListener hipConsentNotificationListener;

    @MockBean
    private HiuConsentNotificationListener hiuConsentNotificationListener;

    @MockBean
    private DataFlowBroadcastListener dataFlowBroadcastListener;

    @MockBean
    private ConsentRequestRepository repository;

    @MockBean
    private ConsentArtefactRepository consentArtefactRepository;

    @MockBean
    private ConsentRequestNotificationListener consentRequestNotificationListener;

    @MockBean
    private PostConsentRequest postConsentRequestNotification;

    @MockBean
    private ConsentNotificationPublisher consentNotificationPublisher;

    @MockBean
    private PinVerificationTokenService pinVerificationTokenService;

    @Mock
    private CentralRegistry centralRegistry;

    @SuppressWarnings("unused")
    @MockBean(name = "centralRegistryJWKSet")
    private JWKSet centralRegistryJWKSet;

    @SuppressWarnings("unused")
    @MockBean(name = "identityServiceJWKSet")
    private JWKSet identityServiceJWKSet;

    @MockBean
    private Authenticator authenticator;

    @MockBean
    private GatewayTokenVerifier gatewayTokenVerifier;

    @MockBean
    private ConceptValidator conceptValidator;

    @MockBean
    private RequestValidator validator;

    @MockBean
    private ConsentManagerClient consentManagerClient;

    @Captor
    private ArgumentCaptor<ConsentRequest> captor;

    private static final MockWebServer clientRegistryServer = new MockWebServer();
    private static final MockWebServer userServer = new MockWebServer();
    private static final MockWebServer identityServer = new MockWebServer();
    private static final MockWebServer patientLinkServer = new MockWebServer();
    private static final MockWebServer gatewayServer = new MockWebServer();
    private static final String CONSENT_GRANT_JSON = "{\n" +
            "    \"consents\": [\n" +
            "        {\n" +
            "            \"hip\": {\n" +
            "                \"id\": \"10000005\"\n" +
            "            },\n" +
            "            \"hiTypes\": [\n" +
            "                \"DiagnosticReport\", \"Observation\"\n" +
            "                ],\n" +
            "            \"careContexts\": [\n" +
            "                {\n" +
            "                    \"patientReference\": \"ashokkumar@max\",\n" +
            "                    \"careContextReference\": \"ashokkumar.opdcontext\"\n" +
            "                }\n" +
            "            ],\n" +
            "            \"permission\": {\n" +
            "                \"accessMode\": \"VIEW\",\n" +
            "                \"dateRange\": {\n" +
            "                    \"from\": \"2020-01-16T08:47:48.000\",\n" +
            "                    \"to\": \"2020-04-29T08:47:48.000\"\n" +
            "                },\n" +
            "                \"dataEraseAt\": \"2020-05-29T08:47:48.000\",\n" +
            "                \"frequency\": {\n" +
            "                    \"unit\": \"HOUR\",\n" +
            "                    \"value\": 1,\n" +
            "                    \"repeats\": 0\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    @AfterAll
    static void tearDown() throws IOException {
        clientRegistryServer.shutdown();
        userServer.shutdown();
        identityServer.shutdown();
        patientLinkServer.shutdown();
        gatewayServer.shutdown();
    }

    private final String requestedConsentJson = "{\n" +
            "            \"status\": \"REQUESTED\",\n" +
            "            \"createdAt\": \"2020-03-14T10:51:05.466\",\n" +
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
            "                \"id\": \"10000005\"\n" +
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
            "                    \"from\": \"2020-03-14T10:50:45.032\",\n" +
            "                    \"to\": \"2020-03-14T10:50:45.032\"\n" +
            "                },\n" +
            "                \"dataEraseAt\": \"2020-03-18T10:50:00.000\",\n" +
            "                \"frequency\": {\n" +
            "                    \"unit\": \"HOUR\",\n" +
            "                    \"value\": 1,\n" +
            "                    \"repeats\": 0\n" +
            "                }\n" +
            "            },\n" +
            "            \"consentNotificationUrl\": \"http://hiu:8003\",\n" +
            "            \"lastUpdated\": \"2020-03-14T12:00:52.091\",\n" +
            "            \"id\": \"30d02f6d-de17-405e-b4ab-d31b2bb799d7\"\n" +
            "        }";

    @Test
    void shouldAcceptConsentRequest() {
        var authToken = string();
        var session = "{\"accessToken\": \"eyJhbGc\", \"refreshToken\": \"eyJhbGc\"}";
        when(gatewayTokenVerifier.verify(authToken))
                .thenReturn(Mono.just(new ServiceCaller("MAX-ID", List.of())));
        when(repository.insert(any(), any())).thenReturn(Mono.empty());
        when(postConsentRequestNotification.broadcastConsentRequestNotification(captor.capture()))
                .thenReturn(Mono.empty());
        when(conceptValidator.validatePurpose("CAREMGT")).thenReturn(Mono.just(true));
        when(conceptValidator.validateHITypes(any())).thenReturn(Mono.just(true));
        when(repository.requestOf(anyString())).thenReturn(Mono.empty());

        // TODO: Two calls being made to CR to get token within one single request, have to make it single.
        load(clientRegistryServer, session);
        load(clientRegistryServer, session);
        load(clientRegistryServer, session);
        load(clientRegistryServer, session);
        load(identityServer, "{}");
        load(userServer, "{}");

        String consentRequestJson = "{\n" +
                "\"requestId\": \"9e1228c3-0d2b-47cb-9ae2-c0eb95aed950\",\n" +
                "  \"consent\": {\n" +
                "    \"purpose\": {\n" +
                "      \"text\": \"Care Management\",\n" +
                "      \"code\": \"CAREMGT\",\n" +
                "      \"refUri\": \"http://projecteka.in/ValueSet/purpose-of-use.json\"\n" +
                "    },\n" +
                "    \"patient\": {\n" +
                "      \"id\": \"batman@ncg\"\n" +
                "    },\n" +
                "    \"hip\": {\n" +
                "      \"id\": \"TMH-ID\"\n" +
                "    },\n" +
                "    \"hiu\": {\n" +
                "      \"id\": \"MAX-ID\"\n" +
                "    },\n" +
                "    \"requester\": {\n" +
                "      \"name\": \"Dr Ramandeep\",\n" +
                "      \"identifier\": {\n" +
                "        \"value\": \"MCI-10\",\n" +
                "        \"type\": \"Oncologist\",\n" +
                "        \"system\": \"http://mci.org/\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"hiTypes\": [\n" +
                "      \"Condition\",\n" +
                "      \"Observation\"\n" +
                "    ],\n" +
                "    \"permission\": {\n" +
                "      \"accessMode\": \"VIEW\",\n" +
                "      \"dateRange\": {\n" +
                "        \"from\": \"2021-01-16T07:23:41.305Z\",\n" +
                "        \"to\": \"2021-01-16T07:35:41.305Z\"\n" +
                "      },\n" +
                "      \"dataEraseAt\": \"2022-01-16T07:23:41.305Z\",\n" +
                "      \"frequency\": {\n" +
                "        \"unit\": \"DAY\",\n" +
                "        \"value\": 1\n" +
                "      }\n" +
                "    },\n" +
                "    \"consentNotificationUrl\": \"https://tmh-hiu/notify\"\n" +
                "  }\n" +
                "}";
        webTestClient.post()
                .uri("/consent-requests")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", authToken)
                .body(BodyInserters.fromValue(consentRequestJson))
                .exchange()
                .expectStatus().isOk()
                .expectBody(RequestCreatedRepresentation.class)
                .value(RequestCreatedRepresentation::getConsentRequestId, Matchers.notNullValue());
    }

    @Test
    void shouldGetConsentRequests() {
        var token = string();
        List<ConsentRequestDetail> requests = new ArrayList<>();
        ListResult<List<ConsentRequestDetail>> result = new ListResult<>(requests, 0);
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller("Ganesh@ncg", true)));
        when(repository.requestsForPatient("Ganesh@ncg", 20, 0, null)).
                thenReturn(Mono.just(result));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/consent-requests").queryParam("limit", "20").build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ConsentRequestsRepresentation.class)
                .value(ConsentRequestsRepresentation::getLimit, is(20))
                .value(ConsentRequestsRepresentation::getOffset, is(0))
                .value(ConsentRequestsRepresentation::getRequests, is(requests))
                .value(ConsentRequestsRepresentation::getSize, is(0));
    }

    @Test
    void shouldGetConsentRequestsForStatus() {
        var token = string();
        List<ConsentRequestDetail> requests = new ArrayList<>();
        ConsentRequestDetail detail = ConsentRequestDetail.builder().build();
        detail.setStatus(EXPIRED);
        requests.add(detail);
        ListResult<List<ConsentRequestDetail>> result = new ListResult<>(requests, 1);
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller("Ganesh@ncg", true)));
        when(repository.requestsForPatient("Ganesh@ncg", 20, 0, "EXPIRED")).
                thenReturn(Mono.just(result));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/consent-requests")
                        .queryParam("limit", "20")
                        .queryParam("status", "EXPIRED")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ConsentRequestsRepresentation.class)
                .value(ConsentRequestsRepresentation::getLimit, is(20))
                .value(ConsentRequestsRepresentation::getOffset, is(0))
                .value(ConsentRequestsRepresentation::getSize, is(1));
    }

    @Test
    void shouldSendNotificationMessage() {
        var notificationMessage = notificationMessage().build();
        consentRequestNotificationListener.notifyUserWith(notificationMessage);
        verify(consentRequestNotificationListener).notifyUserWith(notificationMessage);
    }

    @Test
    void shouldApproveConsentGrant() throws JsonProcessingException {
        var token = string();
        String patientId = "ashok.kumar@ncg";
        var consentRequestDetail = OBJECT_MAPPER.readValue(requestedConsentJson, ConsentRequestDetail.class);
        load(userServer, "{}");
        load(identityServer, "{}");
        load(identityServer, "{}");
        String linkedPatientContextsJson = "{\n" +
                "    \"patient\": {\n" +
                "        \"id\": \"ashok.kumar@ncg\",\n" +
                "        \"firstName\": \"ashok\",\n" +
                "        \"lastName\": \"kumar\",\n" +
                "        \"links\": [\n" +
                "            {\n" +
                "                \"hip\": {\n" +
                "                    \"id\": \"10000005\"\n" +
                "                },\n" +
                "                \"referenceNumber\": \"ashokkumar@max\",\n" +
                "                \"display\": \"Ashok Kumar\",\n" +
                "                \"careContexts\": [\n" +
                "                    {\n" +
                "                        \"referenceNumber\": \"ashokkumar.opdcontext\",\n" +
                "                        \"display\": \"National Cancer program - OPD\"\n" +
                "                    }\n" +
                "                ]\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";
        load(patientLinkServer, linkedPatientContextsJson);
        String scope = "consentrequest.approve";

        when(repository.insert(any(), any())).thenReturn(Mono.empty());
        when(postConsentRequestNotification.broadcastConsentRequestNotification(captor.capture()))
                .thenReturn(Mono.empty());
        when(repository.requestOf("30d02f6d-de17-405e-b4ab-d31b2bb799d7", "REQUESTED", patientId))
                .thenReturn(Mono.just(consentRequestDetail));
        when(pinVerificationTokenService.validateToken(token, scope))
                .thenReturn(Mono.just(new Caller(patientId, false, "randomSessionId")));
        when(consentArtefactRepository.process(any())).thenReturn(Mono.empty());
        when(consentNotificationPublisher.publish(any())).thenReturn(Mono.empty());
        when(conceptValidator.validateHITypes(anyList())).thenReturn(Mono.just(true));
        when(centralRegistry.providerWith(eq("10000005"))).thenReturn(Mono.just(Provider.builder().build()));

        webTestClient.post()
                .uri("/consent-requests/30d02f6d-de17-405e-b4ab-d31b2bb799d7/approve")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .body(BodyInserters.fromValue(CONSENT_GRANT_JSON))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ConsentApprovalResponse.class)
                .value(ConsentApprovalResponse::getConsents, Matchers.notNullValue());
    }

    @Test
    void shouldNotApproveConsentGrantForInvalidCareContext() throws JsonProcessingException {
        var token = string();
        var session = "{\"accessToken\": \"eyJhbGc\", \"refreshToken\": \"eyJhbGc\"}";
        when(repository.insert(any(), any())).thenReturn(Mono.empty());
        when(postConsentRequestNotification.broadcastConsentRequestNotification(captor.capture()))
                .thenReturn(Mono.empty());
        // TODO: Two calls being made to CR to get token within one single request, have to make it single.
        load(clientRegistryServer, session);
        load(clientRegistryServer, session);
        load(clientRegistryServer, session);
        load(clientRegistryServer, session);
        load(userServer, "{}");
        load(identityServer, "{}");
        load(identityServer, "{}");
        //NOTE: referenceNumber of linked CareContext is different. ashokkumar.ipdContext
        //while the grant is for ashokkumar.opdcontext
        String linkedPatientContextsJson = "{\n" +
                "    \"patient\": {\n" +
                "        \"id\": \"ashok.kumar@ncg\",\n" +
                "        \"firstName\": \"ashok\",\n" +
                "        \"lastName\": \"kumar\",\n" +
                "        \"links\": [\n" +
                "            {\n" +
                "                \"hip\": {\n" +
                "                    \"id\": \"10000005\"\n" +
                "                },\n" +
                "                \"referenceNumber\": \"ashokkumar@max\",\n" +
                "                \"display\": \"Ashok Kumar\",\n" +
                "                \"careContexts\": [\n" +
                "                    {\n" +
                "                        \"referenceNumber\": \"ashokkumar.ipdcontext\",\n" +
                "                        \"display\": \"National Cancer program - OPD\"\n" +
                "                    }\n" +
                "                ]\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}";
        load(patientLinkServer, linkedPatientContextsJson);
        var consentRequestDetail = OBJECT_MAPPER.readValue(requestedConsentJson, ConsentRequestDetail.class);
        String patientId = "ashok.kumar@ncg";

        String scope = "consentrequest.approve";
        when(pinVerificationTokenService.validateToken(token, scope))
                .thenReturn(Mono.just(new Caller(patientId, false)));
        when(repository.requestOf("30d02f6d-de17-405e-b4ab-d31b2bb799d7", "REQUESTED", patientId))
                .thenReturn(Mono.just(consentRequestDetail));
        when(consentArtefactRepository.process(any())).thenReturn(Mono.empty());
        when(consentNotificationPublisher.publish(any())).thenReturn(Mono.empty());
        when(conceptValidator.validateHITypes(anyList())).thenReturn(Mono.just(true));

        webTestClient.post()
                .uri("/consent-requests/30d02f6d-de17-405e-b4ab-d31b2bb799d7/approve")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .body(BodyInserters.fromValue(CONSENT_GRANT_JSON))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(in.projecteka.consentmanager.clients.model.Error.class);
    }

    @Test
    void shouldDenyConsentRequest() {
        var token = string();
        var requestId = string();
        var patientId = string();
        var consentRequestDetail = consentRequestDetail()
                .requestId(requestId)
                .patient(new PatientReference(patientId))
                .status(REQUESTED);
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller(patientId, false)));
        when(repository.updateStatus(requestId, DENIED)).thenReturn(Mono.empty());
        when(repository.requestOf(requestId))
                .thenReturn(Mono.just(consentRequestDetail.build()),
                        Mono.just(consentRequestDetail.status(DENIED).build()));
        when(consentNotificationPublisher.publish(any())).thenReturn(Mono.empty());

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder.path(format("/consent-requests/%s/deny", requestId)).build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .exchange()
                .expectStatus()
                .isNoContent();
    }

    @Test
    void shouldThrowErrorForInvalidPurposeInConsentRequest() {
        var authToken = string();
        when(gatewayTokenVerifier.verify(authToken)).thenReturn(Mono.just(new ServiceCaller("MAX-ID", List.of())));
        when(repository.insert(any(), any())).thenReturn(Mono.empty());
        when(postConsentRequestNotification.broadcastConsentRequestNotification(captor.capture()))
                .thenReturn(Mono.empty());
        when(conceptValidator.validatePurpose("INVALID-CODE")).thenReturn(Mono.just(false));
        when(conceptValidator.validateHITypes(any())).thenReturn(Mono.just(true));
        when(repository.requestOf(any())).thenReturn(Mono.empty());
        // TODO: Two calls being made to CR to get token within one single request, have to make it single.
        load(clientRegistryServer, "{}");
        load(clientRegistryServer, "{}");
        load(clientRegistryServer, "{}");
        load(clientRegistryServer, "{}");
        load(identityServer, "{}");
        load(userServer, "{}");

        String consentRequestJson = "{\n" +
                "  \"requestId\": \"5e812965-671c-4b8d-9696-31a024777d37\",\n" +
                "  \"consent\": {\n" +
                "    \"purpose\": {\n" +
                "      \"text\": \"Care Management\",\n" +
                "      \"code\": \"INVALID-CODE\",\n" +
                "      \"refUri\": \"http://projecteka.in/ValueSet/purpose-of-use.json\"\n" +
                "    },\n" +
                "    \"patient\": {\n" +
                "      \"id\": \"batman@ncg\"\n" +
                "    },\n" +
                "    \"hip\": {\n" +
                "      \"id\": \"TMH-ID\",\n" +
                "      \"name\": \"TMH\"\n" +
                "    },\n" +
                "    \"hiu\": {\n" +
                "      \"id\": \"MAX-ID\",\n" +
                "      \"name\": \"MAX\"\n" +
                "    },\n" +
                "    \"requester\": {\n" +
                "      \"name\": \"Dr Ramandeep\",\n" +
                "      \"identifier\": {\n" +
                "        \"value\": \"MCI-10\",\n" +
                "        \"type\": \"Oncologist\",\n" +
                "        \"system\": \"http://mci.org/\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"hiTypes\": [\n" +
                "      \"Condition\",\n" +
                "      \"Observation\"\n" +
                "    ],\n" +
                "    \"permission\": {\n" +
                "      \"accessMode\": \"VIEW\",\n" +
                "      \"dateRange\": {\n" +
                "        \"from\": \"2021-01-16T07:23:41.305Z\",\n" +
                "        \"to\": \"2021-01-16T07:35:41.305Z\"\n" +
                "      },\n" +
                "      \"dataEraseAt\": \"2022-01-16T07:23:41.305Z\",\n" +
                "      \"frequency\": {\n" +
                "        \"unit\": \"DAY\",\n" +
                "        \"value\": 1\n" +
                "      }\n" +
                "    },\n" +
                "    \"consentNotificationUrl\": \"https://tmh-hiu/notify\"\n" +
                "  }\n" +
                "}";
        webTestClient.post()
                .uri("/consent-requests")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", authToken)
                .body(BodyInserters.fromValue(consentRequestJson))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(in.projecteka.consentmanager.clients.model.Error.class);
    }

    @Test
    void shouldAcceptInitConsentRequest() {
        var authToken = string();
        var session = "{\"accessToken\": \"eyJhbGc\", \"refreshToken\": \"eyJhbGc\"}";
        String HIUId = "MAX-ID";
        LocalDateTime fromDate = LocalDateTime.now();
        LocalDateTime toDate = LocalDateTime.now().plusMinutes(1);
        AccessPeriod dateRange = AccessPeriod.builder().fromDate(fromDate).toDate(toDate).build();
        ConsentPermission permission = ConsentPermission.builder().dateRange(dateRange).build();
        RequestedDetail requestedDetail = RequestedDetail.builder()
                .purpose(new ConsentPurpose())
                .patient(PatientReference.builder().build())
                .permission(permission)
                .hiu(HIUReference.builder().build())
                .hip(HIPReference.builder().build())
                .hiTypes(HIType.values())
                .build();
        in.projecteka.consentmanager.consent.model.request.ConsentRequest consentRequest = consentRequest()
                .timestamp(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(2))
                .consent(requestedDetail)
                .build();
        var caller = ServiceCaller.builder().clientId("Client_ID").roles(List.of(GATEWAY)).build();

        when(authenticator.verify(authToken)).thenReturn(Mono.just(new Caller("user-id", false)));
        when(gatewayTokenVerifier.verify(authToken))
                .thenReturn(Mono.just(caller));
        when(validator.put(anyString(), anyString())).thenReturn(Mono.empty());
        when(validator.validate(anyString(), anyString())).thenReturn(Mono.just(Boolean.TRUE));
        when(repository.insert(any(), any())).thenReturn(Mono.empty());
        when(repository.requestOf(anyString())).thenReturn(Mono.empty());
        when(conceptValidator.validatePurpose(any())).thenReturn(Mono.just(true));
        when(conceptValidator.validateHITypes(any())).thenReturn(Mono.just(true));
        when(consentManagerClient.sendInitResponseToGateway(any(), eq(HIUId)))
                .thenReturn(Mono.empty());
        when(postConsentRequestNotification.broadcastConsentRequestNotification(captor.capture()))
                .thenReturn(Mono.empty());
        load(clientRegistryServer, session);
        load(clientRegistryServer, session);
        load(clientRegistryServer, session);
        load(clientRegistryServer, session);
        load(identityServer, "{}");
        load(identityServer, "{}");
        load(userServer, "{}");
        load(gatewayServer, "{}");
        gatewayServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody("{}"));

        webTestClient.post()
                .uri(Constants.PATH_CONSENT_REQUESTS_INIT)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, authToken)
                .body(BodyInserters.fromValue(consentRequest))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isAccepted();
    }

    @Test
    void shouldReturnTrueOnPolicyCheck(){
        var hiuId = "10000002";
        var patientName = "xyz@xyz";
        var requestedDetail = RequestedDetail.builder()
                                .hiu(HIUReference.builder().id(hiuId).build())
                                .patient(PatientReference.builder().id(patientName).build())
                                .requester(Requester.builder().name(patientName).build())
                                .build();
        assertEquals(new NhsPolicyCheck().checkPolicyFor(ConsentRequest.builder().detail(requestedDetail).build(), hiuId),true);
    }

    @Test
    void shouldReturnFalseOnPolicyCheck(){
        var hiuId = string();
        var patientName = "xyz@xyz";
        var requestedDetail = RequestedDetail.builder()
                .hiu(HIUReference.builder().id(hiuId).build())
                .patient(PatientReference.builder().id(patientName).build())
                .requester(Requester.builder().name(hiuId).build())
                .build();
        assertEquals(new NhsPolicyCheck().checkPolicyFor(ConsentRequest.builder().detail(requestedDetail).build(), hiuId),false);
    }


    static class PropertyInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues values = TestPropertyValues.of(
                    Stream.of("consentmanager.clientregistry.url=" + clientRegistryServer.url(""),
                            "consentmanager.userservice.url=" + userServer.url(""),
                            "consentmanager.consentservice.maxPageSize=50",
                            "consentmanager.keycloak.baseUrl=" + identityServer.url(""),
                            "consentmanager.linkservice.url=" + patientLinkServer.url(""),
                            "consentmanager.gatewayservice.baseUrl=" + gatewayServer.url("")));
            values.applyTo(applicationContext);
        }
    }

    private static void load(MockWebServer server, String body) {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(body)
                .setHeader("content-type", "application/json"));
    }
}
