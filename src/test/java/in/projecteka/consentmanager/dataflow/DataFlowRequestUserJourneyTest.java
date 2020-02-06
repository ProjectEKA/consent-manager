package in.projecteka.consentmanager.dataflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.dataflow.model.AccessPeriod;
import in.projecteka.consentmanager.dataflow.model.ConsentArtefactRepresentation;
import in.projecteka.consentmanager.dataflow.model.DataFlowRequest;
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
import static in.projecteka.consentmanager.dataflow.Utils.toDate;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient(timeout = "300000")
@ContextConfiguration(initializers = DataFlowRequestUserJourneyTest.ContextInitializer.class)
public class DataFlowRequestUserJourneyTest {
    private static MockWebServer consentManagerServer = new MockWebServer();

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private DataFlowRequestRepository dataFlowRequestRepository;

    @AfterAll
    public static void tearDown() throws IOException {
        consentManagerServer.shutdown();
    }

    @Test
    public void shouldAcknowledgeDataFlowRequest() throws IOException, ParseException {
        String hiuId = "10000005";
        DataFlowRequest dataFlowRequest = dataFlowRequest().build();
        dataFlowRequest.setHiDataRange(HIDataRange.builder().from(toDate("2020-01-16T08:47:48Z")).to(toDate("2020" +
                "-01-20T08:47:48Z")).build());
        ConsentArtefactRepresentation consentArtefactRepresentation = consentArtefactRepresentation().build();
        consentArtefactRepresentation.getConsentDetail().getPermission().
                setDateRange(AccessPeriod.builder()
                        .fromDate(toDate("2020-01-15T08:47:48Z"))
                        .toDate(toDate("2020-01-29T08:47:48Z"))
                        .build());
        consentArtefactRepresentation.getConsentDetail().setHiu(HIUReference.builder().id(hiuId).name("MAX").build());
        Date consentExpiryDate = new Date();
        consentExpiryDate.setTime(consentExpiryDate.getTime() + 9000000);
        consentArtefactRepresentation.getConsentDetail().getPermission().setDataExpiryAt(consentExpiryDate);
        var consentArtefactRepresentationJson = new ObjectMapper().writeValueAsString(consentArtefactRepresentation);
        consentManagerServer.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(consentArtefactRepresentationJson));

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
        String hiuId = "10000005";
        DataFlowRequest dataFlowRequest = dataFlowRequest().build();
        ConsentArtefactRepresentation consentArtefactRepresentation = consentArtefactRepresentation().build();
        consentArtefactRepresentation.getConsentDetail().setHiu(HIUReference.builder().id(hiuId).name("MAX").build());
        consentArtefactRepresentation.getConsentDetail().getPermission().setDataExpiryAt(toDate("2020-01-15T08:47:48Z"
        ));
        var consentArtefactRepresentationJson = new ObjectMapper().writeValueAsString(consentArtefactRepresentation);

        var errorResponse = new ErrorRepresentation(new Error(ErrorCode.CONSENT_ARTEFACT_EXPIRED, "Consent artefact " +
                "expired"));
        var errorResponseJson = new ObjectMapper().writeValueAsString(errorResponse);
        consentManagerServer.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(consentArtefactRepresentationJson));

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
        String hiuId = "10000005";
        DataFlowRequest dataFlowRequest = dataFlowRequest().build();
        ConsentArtefactRepresentation consentArtefactRepresentation = consentArtefactRepresentation().build();
        consentArtefactRepresentation.getConsentDetail().setHiu(HIUReference.builder().id(hiuId).name("MAX").build());
        var consentArtefactRepresentationJson = new ObjectMapper().writeValueAsString(consentArtefactRepresentation);

        var errorResponse = new ErrorRepresentation(new Error(ErrorCode.INVALID_HIU, "Not a valid HIU"));
        var errorResponseJson = new ObjectMapper().writeValueAsString(errorResponse);
        consentManagerServer.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(consentArtefactRepresentationJson));

        webTestClient
                .post()
                .uri("/health-information/request")
                .header("Authorization", "MQ==")
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
        String hiuId = "10000005";
        DataFlowRequest dataFlowRequest = dataFlowRequest().build();
        dataFlowRequest.setHiDataRange(HIDataRange.builder().from(toDate("2020-01-14T08:47:48Z")).to(toDate("2020" +
                "-01-20T08:47:48Z")).build());
        ConsentArtefactRepresentation consentArtefactRepresentation = consentArtefactRepresentation().build();
        consentArtefactRepresentation.getConsentDetail().getPermission().
                setDateRange(AccessPeriod.builder()
                        .fromDate(toDate("2020-01-15T08:47:48Z"))
                        .toDate(toDate("2020-01-29T08:47:48Z"))
                        .build());
        consentArtefactRepresentation.getConsentDetail().setHiu(HIUReference.builder().id(hiuId).name("MAX").build());
        Date consentExpiryDate = new Date();
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

    public static class ContextInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues values =
                    TestPropertyValues.of(
                            Stream.of("consentmanager.dataflow.url=" + consentManagerServer.url(""),
                                    "consentmanager.dataflow.clientId=1",
                                    "consentmanager.dataflow.clientPassword=NCG_CM"));
            values.applyTo(applicationContext);
        }
    }
}
