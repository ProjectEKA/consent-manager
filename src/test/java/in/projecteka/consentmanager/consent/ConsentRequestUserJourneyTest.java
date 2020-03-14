package in.projecteka.consentmanager.consent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.consent.model.ConsentRequest;
import in.projecteka.consentmanager.consent.model.ConsentRequestDetail;
import in.projecteka.consentmanager.consent.model.response.ConsentApprovalResponse;
import in.projecteka.consentmanager.consent.model.response.ConsentRequestsRepresentation;
import in.projecteka.consentmanager.consent.model.response.RequestCreatedRepresentation;
import in.projecteka.consentmanager.dataflow.DataFlowBroadcastListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static in.projecteka.consentmanager.consent.TestBuilders.notificationMessage;
import static in.projecteka.consentmanager.consent.TestBuilders.string;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@AutoConfigureWebTestClient(timeout = "300000")
@ContextConfiguration(initializers = ConsentRequestUserJourneyTest.PropertyInitializer.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ConsentRequestUserJourneyTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private DestinationsConfig destinationsConfig;

    @MockBean
    private ConsentRequestRepository repository;

    @MockBean
    private HiuConsentNotificationListener hiuConsentNotificationListener;

    @MockBean
    private HipConsentNotificationListener hipConsentNotificationListener;

    @MockBean
    private DataFlowBroadcastListener dataFlowBroadcastListener;

    @MockBean
    private ConsentArtefactRepository consentArtefactRepository;

    @MockBean
    private ConsentRequestNotificationListener consentRequestNotificationListener;

    @MockBean
    private PostConsentRequest postConsentRequestNotification;

    @MockBean
    private PostConsentApproval postConsentApproval;

    @Captor
    private ArgumentCaptor<ConsentRequest> captor;


    private static MockWebServer clientRegistryServer = new MockWebServer();
    private static MockWebServer userServer = new MockWebServer();
    private static MockWebServer identityServer = new MockWebServer();
    private static MockWebServer patientLinkServer = new MockWebServer();
    private static final String CONSENT_GRANT_JSON = "{\n" +
            "    \"consents\": [\n" +
            "        {\n" +
            "            \"hip\": {\n" +
            "                \"id\": \"10000005\",\n" +
            "                \"name\": \"Max Health Care\"\n" +
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
            "                    \"from\": \"2020-01-16T08:47:48.000+0000\",\n" +
            "                    \"to\": \"2020-04-29T08:47:48.000+0000\"\n" +
            "                },\n" +
            "                \"dataExpiryAt\": \"2020-05-29T08:47:48.000+0000\",\n" +
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
    public static void tearDown() throws IOException {
        clientRegistryServer.shutdown();
        userServer.shutdown();
        identityServer.shutdown();
        patientLinkServer.shutdown();
    }

    private String consentRequestJson =
            "{\n" +
            "  \"consent\": {\n" +
            "    \"purpose\": {\n" +
            "      \"text\": \"For Clinical Reference\",\n" +
            "      \"code\": \"CLINICAL\",\n" +
            "      \"refUri\": \"http://nha.gov.in/value-set/purpose.txt\"\n" +
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
            "      \"dataExpiryAt\": \"2022-01-16T07:23:41.305Z\",\n" +
            "      \"frequency\": {\n" +
            "        \"unit\": \"DAY\",\n" +
            "        \"value\": 1\n" +
            "      }\n" +
            "    },\n" +
            "    \"callBackUrl\": \"https://tmh-hiu/notify\"\n" +
            "  }\n" +
            "}";

    private String requestedConsentJson = "{\n" +
            "            \"status\": \"REQUESTED\",\n" +
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

    @Test
    public void shouldAcceptConsentRequest() {
        when(repository.insert(any(), any())).thenReturn(Mono.empty());
        when(postConsentRequestNotification.broadcastConsentRequestNotification(captor.capture()))
                .thenReturn(Mono.empty());
        // TODO: Two calls being made to CR to get token within one single request, have to make it single.
        load(clientRegistryServer, "{}");
        load(clientRegistryServer, "{}");
        load(clientRegistryServer, "{}");
        load(clientRegistryServer, "{}");
        load(userServer, "{}");
        var user = "{\"preferred_username\": \"patient@ncg\"}";
        load(identityServer, user);


        webTestClient.post()
                .uri("/consent-requests")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", string())
                .body(BodyInserters.fromValue(consentRequestJson))
                .exchange()
                .expectStatus().isOk()
                .expectBody(RequestCreatedRepresentation.class)
                .value(RequestCreatedRepresentation::getConsentRequestId, Matchers.notNullValue());
    }

    @Test
    public void shouldGetConsentRequests() {
        List<ConsentRequestDetail> requests = new ArrayList<>();
        var user = "{\"preferred_username\": \"Ganesh@ncg\"}";
        load(identityServer, user);
        load(identityServer, user);
        when(repository.requestsForPatient("Ganesh@ncg", 20, 0)).thenReturn(Mono.just(requests));
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/consent-requests").queryParam("limit", "20").build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", string())
                .exchange()
                .expectStatus().isOk()
                .expectBody(ConsentRequestsRepresentation.class)
                .value(ConsentRequestsRepresentation::getLimit, Matchers.is(20))
                .value(ConsentRequestsRepresentation::getOffset, Matchers.is(0))
                .value(response -> response.getRequests().size(), Matchers.is(0));
    }

    @Test
    public void shouldSendNotificationMessage() {
        var notificationMessage = notificationMessage().build();
        consentRequestNotificationListener.notifyUserWith(notificationMessage);
        verify(consentRequestNotificationListener).notifyUserWith(notificationMessage);
    }

    @Test
    public void shouldApproveConsentGrant() throws JsonProcessingException {
        when(repository.insert(any(), any())).thenReturn(Mono.empty());
        when(postConsentRequestNotification.broadcastConsentRequestNotification(captor.capture()))
                .thenReturn(Mono.empty());
        // TODO: Two calls being made to CR to get token within one single request, have to make it single.
        load(clientRegistryServer, "{}");
        load(clientRegistryServer, "{}");
        load(clientRegistryServer, "{}");
        load(clientRegistryServer, "{}");
        load(userServer, "{}");
        var user = "{\"preferred_username\": \"patient@ncg\"}";
        load(identityServer, user);
        load(identityServer, user);

        String linkedPatientContextsJson = "{\n" +
                "    \"patient\": {\n" +
                "        \"id\": \"ashok.kumar@ncg\",\n" +
                "        \"firstName\": \"ashok\",\n" +
                "        \"lastName\": \"kumar\",\n" +
                "        \"links\": [\n" +
                "            {\n" +
                "                \"hip\": {\n" +
                "                    \"id\": \"10000005\",\n" +
                "                    \"name\": \"Max Health Care\"\n" +
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
        ConsentRequestDetail consentRequestDetail = new ObjectMapper().readValue(requestedConsentJson, ConsentRequestDetail.class);
        when(repository.requestOf("30d02f6d-de17-405e-b4ab-d31b2bb799d7", "REQUESTED")).thenReturn(Mono.just(consentRequestDetail));
        when(consentArtefactRepository.addConsentArtefactAndUpdateStatus(any(), eq("30d02f6d-de17-405e-b4ab-d31b2bb799d7"), any(), any(), any())).thenReturn(Mono.empty());
        when(postConsentApproval.broadcastConsentArtefacts(any())).thenReturn(Mono.empty());
        webTestClient.post()
                .uri("/consent-requests/30d02f6d-de17-405e-b4ab-d31b2bb799d7/approve")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", string())
                .body(BodyInserters.fromValue(CONSENT_GRANT_JSON))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ConsentApprovalResponse.class)
                .value(ConsentApprovalResponse::getConsents, Matchers.notNullValue());
    }

    @Test
    public void shouldNotApproveConsentGrantForInvalidCareContext() throws JsonProcessingException {
        when(repository.insert(any(), any())).thenReturn(Mono.empty());
        when(postConsentRequestNotification.broadcastConsentRequestNotification(captor.capture()))
                .thenReturn(Mono.empty());
        // TODO: Two calls being made to CR to get token within one single request, have to make it single.
        load(clientRegistryServer, "{}");
        load(clientRegistryServer, "{}");
        load(clientRegistryServer, "{}");
        load(clientRegistryServer, "{}");
        load(userServer, "{}");
        var user = "{\"preferred_username\": \"patient@ncg\"}";
        load(identityServer, user);
        load(identityServer, user);
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
                "                    \"id\": \"10000005\",\n" +
                "                    \"name\": \"Max Health Care\"\n" +
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
        ConsentRequestDetail consentRequestDetail = new ObjectMapper().readValue(requestedConsentJson, ConsentRequestDetail.class);
        when(repository.requestOf("30d02f6d-de17-405e-b4ab-d31b2bb799d7", "REQUESTED")).thenReturn(Mono.just(consentRequestDetail));
        when(consentArtefactRepository.addConsentArtefactAndUpdateStatus(any(), eq("30d02f6d-de17-405e-b4ab-d31b2bb799d7"), any(), any(), any())).thenReturn(Mono.empty());
        when(postConsentApproval.broadcastConsentArtefacts(any())).thenReturn(Mono.empty());
        webTestClient.post()
                .uri("/consent-requests/30d02f6d-de17-405e-b4ab-d31b2bb799d7/approve")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", string())
                .body(BodyInserters.fromValue(CONSENT_GRANT_JSON))
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody(in.projecteka.consentmanager.clients.model.Error.class);
    }

    public static class PropertyInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues values = TestPropertyValues.of(
                    Stream.of("consentmanager.clientregistry.url=" + clientRegistryServer.url(""),
                            "consentmanager.userservice.url=" + userServer.url(""),
                            "consentmanager.consentservice.maxPageSize=50",
                            "consentmanager.keycloak.baseUrl=" + identityServer.url(""),
                            "consentmanager.linkservice.url=" + patientLinkServer.url("")));
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
