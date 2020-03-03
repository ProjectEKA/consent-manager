package in.projecteka.consentmanager.dataflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.DataRequestNotifier;
import in.projecteka.consentmanager.consent.HipConsentNotificationListener;
import in.projecteka.consentmanager.dataflow.model.AccessPeriod;
import in.projecteka.consentmanager.consent.HiuConsentNotificationListener;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequest;
import in.projecteka.consentmanager.dataflow.model.ConsentArtefactRepresentation;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequestResponse;
import in.projecteka.consentmanager.dataflow.model.HIDataRange;
import in.projecteka.consentmanager.dataflow.model.HIUReference;
import in.projecteka.consentmanager.link.link.model.Error;
import in.projecteka.consentmanager.link.link.model.ErrorCode;
import in.projecteka.consentmanager.link.link.model.ErrorRepresentation;
import okhttp3.mockwebserver.MockResponse;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.stream.Stream;

import static in.projecteka.consentmanager.dataflow.TestBuilders.consentArtefactRepresentation;
import static in.projecteka.consentmanager.dataflow.TestBuilders.dataFlowRequest;
import static in.projecteka.consentmanager.dataflow.TestBuilders.dataFlowRequestMessage;
import static in.projecteka.consentmanager.dataflow.TestBuilders.string;
import static in.projecteka.consentmanager.dataflow.Utils.toDate;
import static in.projecteka.consentmanager.link.TestBuilders.provider;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "300000")
@ContextConfiguration(initializers = DataFlowRequesterUserJourneyTest.ContextInitializer.class)
public class DataFlowRequesterUserJourneyTest {
    private static MockWebServer consentManagerServer = new MockWebServer();
    private static MockWebServer identityServer = new MockWebServer();

    @SuppressWarnings("unused")
    @MockBean
    private DestinationsConfig destinationsConfig;

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private DataFlowRequestRepository dataFlowRequestRepository;

    @MockBean
    private PostDataFlowRequestApproval postDataFlowRequestApproval;

    @MockBean
    private HiuConsentNotificationListener hiuConsentNotificationListener;

    @MockBean
    private HipConsentNotificationListener hipConsentNotificationListener;

    @MockBean
    private DataFlowBroadcastListener dataFlowBroadcastListener;

    @MockBean
    private ClientRegistryClient clientRegistryClient;

    @MockBean
    private DataRequestNotifier dataRequestNotifier;

    @AfterAll
    public static void tearDown() throws IOException {
        consentManagerServer.shutdown();
        identityServer.shutdown();
    }

    @Test
    public void shouldAcknowledgeDataFlowRequest() throws IOException, ParseException {
        var hiuId = "10000005";
        var dataFlowRequest = dataFlowRequest().build();
        dataFlowRequest.setHiDataRange(HIDataRange.builder().from(toDate("2020-01-16T08:47:48Z")).to(toDate("2020" +
                "-01-20T08:47:48Z")).build());
        ConsentArtefactRepresentation consentArtefactRepresentation = consentArtefactRepresentation().build();
        consentArtefactRepresentation.getConsentDetail().getPermission().
                setDateRange(AccessPeriod.builder()
                        .fromDate(toDate("2020-01-15T08:47:48Z"))
                        .toDate(toDate("2020-01-29T08:47:48Z"))
                        .build());
        consentArtefactRepresentation.getConsentDetail().setHiu(HIUReference.builder().id(hiuId).name("MAX").build());
        var consentExpiryDate = new Date();
        consentExpiryDate.setTime(consentExpiryDate.getTime() + 9000000);
        consentArtefactRepresentation.getConsentDetail().getPermission().setDataExpiryAt(consentExpiryDate);
        var consentArtefactRepresentationJson = new ObjectMapper().writeValueAsString(consentArtefactRepresentation);
        consentManagerServer.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(consentArtefactRepresentationJson));
        var user = "{\"preferred_username\": \"service-account-10000005\"}";
        identityServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(user));
        identityServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(user));
        when(postDataFlowRequestApproval.broadcastDataFlowRequest(anyString(), any(DataFlowRequest.class)))
                .thenReturn(Mono.empty());
        when(dataFlowRequestRepository.addDataFlowRequest(anyString(), any(DataFlowRequest.class)))
                .thenReturn(Mono.create(MonoSink::success));

        webTestClient
                .post()
                .uri("/health-information/request")
                .header("Authorization", "MTAwMDAwMDU=")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dataFlowRequest)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(DataFlowRequestResponse.class)
                .value(DataFlowRequestResponse::getTransactionId, Matchers.notNullValue());
    }

    @Test
    public void shouldThrowConsentArtefactExpired() throws IOException, ParseException {
        var hiuId = "10000005";
        var dataFlowRequest = dataFlowRequest().build();
        var consentArtefactRepresentation = consentArtefactRepresentation().build();
        consentArtefactRepresentation.getConsentDetail().setHiu(HIUReference.builder().id(hiuId).name("MAX").build());
        consentArtefactRepresentation
                .getConsentDetail()
                .getPermission()
                .setDataExpiryAt(toDate("2020-01-15T08:47:48Z"));
        var consentArtefactRepresentationJson = new ObjectMapper().writeValueAsString(consentArtefactRepresentation);
        var errorResponse = new ErrorRepresentation(new Error(ErrorCode.CONSENT_ARTEFACT_EXPIRED,
                "Consent artefact expired"));
        var errorResponseJson = new ObjectMapper().writeValueAsString(errorResponse);
        consentManagerServer.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(consentArtefactRepresentationJson));
        var user = "{\"preferred_username\": \"service-account-10000005\"}";
        identityServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(user));
        identityServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(user));

        webTestClient
                .post()
                .uri("/health-information/request")
                .header("Authorization", "MTAwMDAwMDU=")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dataFlowRequest)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .json(errorResponseJson);
    }

    @Test
    public void shouldThrowHIUInvalid() throws IOException {
        var hiuId = "10000005";
        var dataFlowRequest = dataFlowRequest().build();
        var consentArtefactRepresentation = consentArtefactRepresentation().build();
        consentArtefactRepresentation.getConsentDetail().setHiu(HIUReference.builder().id(hiuId).name("MAX").build());
        var consentArtefactRepresentationJson = new ObjectMapper().writeValueAsString(consentArtefactRepresentation);
        var errorResponse = new ErrorRepresentation(new Error(ErrorCode.INVALID_HIU, "Not a valid HIU"));
        var errorResponseJson = new ObjectMapper().writeValueAsString(errorResponse);
        consentManagerServer.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(consentArtefactRepresentationJson));
        var user = "{\"preferred_username\": \"service-account-different-hiu\"}";
        identityServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(user));
        identityServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(user));

        webTestClient
                .post()
                .uri("/health-information/request")
                .header("Authorization", string())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dataFlowRequest)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .json(errorResponseJson);
    }

    @Test
    public void shouldThrowInvalidDateRange() throws IOException, ParseException {
        var hiuId = "10000005";
        var dataFlowRequest = dataFlowRequest().build();
        dataFlowRequest.setHiDataRange(HIDataRange.builder().from(toDate("2020-01-14T08:47:48Z")).to(toDate("2020" +
                "-01-20T08:47:48Z")).build());
        var consentArtefactRepresentation = consentArtefactRepresentation().build();
        consentArtefactRepresentation.getConsentDetail().getPermission().
                setDateRange(AccessPeriod.builder()
                        .fromDate(toDate("2020-01-15T08:47:48Z"))
                        .toDate(toDate("2020-01-29T08:47:48Z"))
                        .build());
        consentArtefactRepresentation.getConsentDetail().setHiu(HIUReference.builder().id(hiuId).name("MAX").build());
        var consentExpiryDate = new Date();
        consentExpiryDate.setTime(consentExpiryDate.getTime() + 9000000);
        consentArtefactRepresentation.getConsentDetail().getPermission().setDataExpiryAt(consentExpiryDate);
        var consentArtefactRepresentationJson = new ObjectMapper().writeValueAsString(consentArtefactRepresentation);
        var errorResponse = new ErrorRepresentation(new Error(ErrorCode.INVALID_DATE_RANGE, "Date Range given is " +
                "invalid"));
        var errorResponseJson = new ObjectMapper().writeValueAsString(errorResponse);
        consentManagerServer.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(consentArtefactRepresentationJson));
        var user = "{\"preferred_username\": \"service-account-10000005\"}";
        identityServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(user));
        identityServer.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(user));

        webTestClient
                .post()
                .uri("/health-information/request")
                .header("Authorization", "MTAwMDAwMDU=")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dataFlowRequest)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .json(errorResponseJson);
    }

    @Test
    public void shouldSendDataRequestToHip() {
        var dataFlowRequestMessage = dataFlowRequestMessage().build();
        var provider = provider().build();
        var dataFlowRequest = new in.projecteka.consentmanager.dataflow.model.hip.DataFlowRequest(
                dataFlowRequestMessage.getTransactionId(),
                dataFlowRequestMessage.getDataFlowRequest().getConsent(),
                dataFlowRequestMessage.getDataFlowRequest().getHiDataRange(),
                dataFlowRequestMessage.getDataFlowRequest().getCallBackUrl(),
                dataFlowRequestMessage.getDataFlowRequest().getKeyMaterial());
        when(dataFlowRequestRepository.getHipIdFor(dataFlowRequestMessage.getDataFlowRequest().getConsent().getId()))
                .thenReturn(Mono.just("10000005"));
        when(clientRegistryClient.providerWith("10000005")).thenReturn(Mono.just(provider));
        dataFlowBroadcastListener.configureAndSendDataRequestFor(dataFlowRequest);

        verify(dataFlowBroadcastListener).configureAndSendDataRequestFor(dataFlowRequest);
    }

    public static class ContextInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues values =
                    TestPropertyValues.of(
                            Stream.of("consentmanager.dataflow.authserver.url=" + consentManagerServer.url(""),
                                    "consentmanager.dataflow.authserver.clientId=1",
                                    "consentmanager.dataflow.authserver.clientSecret=NCG_CM",
                                    "consentmanager.dataflow.consentmanager.url=" + consentManagerServer.url(""),
                                    "consentmanager.keycloak.baseUrl=" + identityServer.url("")));
            values.applyTo(applicationContext);
        }
    }
}
