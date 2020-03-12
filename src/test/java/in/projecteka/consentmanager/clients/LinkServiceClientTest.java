package in.projecteka.consentmanager.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.clients.model.PatientLinkReferenceRequest;
import in.projecteka.consentmanager.clients.model.PatientLinkRequest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.Objects;

import static in.projecteka.consentmanager.clients.TestBuilders.errorRepresentation;
import static in.projecteka.consentmanager.clients.TestBuilders.patientLinkReferenceRequestForHIP;
import static in.projecteka.consentmanager.clients.TestBuilders.patientLinkReferenceResponse;
import static in.projecteka.consentmanager.clients.TestBuilders.patientLinkRequest;
import static in.projecteka.consentmanager.clients.TestBuilders.patientLinkResponse;
import static in.projecteka.consentmanager.clients.TestBuilders.string;
import static org.assertj.core.api.Assertions.assertThat;

public class LinkServiceClientTest {
    private LinkServiceClient linkServiceClient;
    private MockWebServer mockWebServer;
    private String baseUrl;

    @BeforeEach
    public void init() {
        mockWebServer = new MockWebServer();
        baseUrl = mockWebServer.url("/").toString();
        WebClient.Builder webClientBuilder = WebClient.builder().baseUrl(baseUrl);
        linkServiceClient = new LinkServiceClient(webClientBuilder);
    }

    @Test
    public void shouldCreateLinkReference() throws IOException, InterruptedException {
        var linkReference = patientLinkReferenceResponse().build();
        var linkReferenceJson = new ObjectMapper().writeValueAsString(linkReference);

        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(linkReferenceJson));

        PatientLinkReferenceRequest request = patientLinkReferenceRequestForHIP().build();
        StepVerifier.create(linkServiceClient.linkPatientEnquiry(request, baseUrl, string()))
                .assertNext(
                        patientLinkReferenceResponse -> {
                            assertThat(patientLinkReferenceResponse.getLink().getReferenceNumber())
                                    .isEqualTo(linkReference.getLink().getReferenceNumber());
                            assertThat(patientLinkReferenceResponse.getLink().getAuthenticationType())
                                    .isEqualTo(linkReference.getLink().getAuthenticationType());
                            assertThat(patientLinkReferenceResponse.getLink().getMeta().getCommunicationMedium())
                                    .isEqualTo(linkReference.getLink().getMeta().getCommunicationMedium());
                            assertThat(patientLinkReferenceResponse.getLink().getMeta().getCommunicationExpiry())
                                    .isEqualTo(linkReference.getLink().getMeta().getCommunicationExpiry());
                            assertThat(patientLinkReferenceResponse.getLink().getMeta().getCommunicationHint())
                                    .isEqualTo(linkReference.getLink().getMeta().getCommunicationHint());
                        })
                .verifyComplete();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();

        assertThat(Objects.requireNonNull(recordedRequest.getRequestUrl()).toString())
                .isEqualTo(baseUrl + "patients/link");
        assertThat(recordedRequest.getBody().readUtf8())
                .isEqualTo(new ObjectMapper().writeValueAsString(request));
    }

    @Test
    public void shouldReturnPatientNotFoundError() throws IOException, InterruptedException {
        var errorResponse = errorRepresentation().build();
        var errorResponseJson = new ObjectMapper().writeValueAsString(errorResponse);

        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(404)
                        .setHeader("Content-Type", "application/json")
                        .setBody(errorResponseJson));

        PatientLinkReferenceRequest request = patientLinkReferenceRequestForHIP().build();
        StepVerifier.create(linkServiceClient.linkPatientEnquiry(request, baseUrl, string()))
                .expectErrorSatisfies(
                        errorRes -> {
                            assertThat(((ClientError) errorRes).getError().getError().getCode().getValue())
                                    .isEqualTo(errorResponse.getError().getCode().getValue());
                            assertThat(((ClientError) errorRes).getError().getError().getMessage())
                                    .isEqualTo(errorResponse.getError().getMessage());
                        })
                .verify();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();

        assertThat(Objects.requireNonNull(recordedRequest.getRequestUrl()).toString())
                .isEqualTo(baseUrl + "patients/link");
        assertThat(recordedRequest.getBody().readUtf8())
                .isEqualTo(new ObjectMapper().writeValueAsString(request));
    }

    @Test
    public void shouldLinkCareContexts() throws IOException, InterruptedException {
        var linkRes = patientLinkResponse().build();
        var linkResJson = new ObjectMapper().writeValueAsString(linkRes);

        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(linkResJson));

        PatientLinkRequest request = patientLinkRequest().build();
        String linkRefNumber = "test-ref-number";
        StepVerifier.create(linkServiceClient.linkPatientConfirmation(linkRefNumber, request, baseUrl, string()))
                .assertNext(
                        patientLinkResponse -> {
                            assertThat(patientLinkResponse.getPatient().getReferenceNumber())
                                    .isEqualTo(linkRes.getPatient().getReferenceNumber());
                            assertThat(patientLinkResponse.getPatient().getDisplay())
                                    .isEqualTo(linkRes.getPatient().getDisplay());
                            assertThat(
                                    patientLinkResponse
                                            .getPatient()
                                            .getCareContexts()
                                            .get(0)
                                            .getReferenceNumber())
                                    .isEqualTo(linkRes.getPatient().getCareContexts().get(0).getReferenceNumber());
                            assertThat(patientLinkResponse.getPatient().getCareContexts().get(0).getDisplay())
                                    .isEqualTo(linkRes.getPatient().getCareContexts().get(0).getDisplay());
                        })
                .verifyComplete();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();

        assertThat(Objects.requireNonNull(recordedRequest.getRequestUrl()).toString())
                .isEqualTo(baseUrl + "patients/link/test-ref-number");
        assertThat(recordedRequest.getBody().readUtf8())
                .isEqualTo(new ObjectMapper().writeValueAsString(request));
    }

    @Test
    public void shouldReturnOTPInvalidError() throws IOException, InterruptedException {
        var errorResponse = errorRepresentation().build();
        var errorResponseJson = new ObjectMapper().writeValueAsString(errorResponse);
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(404)
                        .setHeader("Content-Type", "application/json")
                        .setBody(errorResponseJson));

        PatientLinkRequest request = patientLinkRequest().build();
        String linkRefNumber = "test-ref-number";
        StepVerifier.create(linkServiceClient.linkPatientConfirmation(linkRefNumber, request, baseUrl, string()))
                .expectErrorSatisfies(
                        errorRes -> {
                            assertThat(((ClientError) errorRes).getError().getError().getCode().getValue())
                                    .isEqualTo(errorResponse.getError().getCode().getValue());
                            assertThat(((ClientError) errorRes).getError().getError().getMessage())
                                    .isEqualTo(errorResponse.getError().getMessage());
                        })
                .verify();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();

        assertThat(recordedRequest.getRequestUrl().toString())
                .isEqualTo(baseUrl + "patients/link/test-ref-number");
        assertThat(recordedRequest.getBody().readUtf8())
                .isEqualTo(new ObjectMapper().writeValueAsString(request));
    }
}
