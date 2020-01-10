package in.projecteka.hdaf.link;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.hdaf.link.link.model.PatientLinkRequest;
import in.projecteka.hdaf.link.link.model.hip.PatientLinkReferenceRequest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class HIPClientTest {
    private HIPClient hipClient;
    private MockWebServer mockWebServer;
    private String baseUrl;

    @BeforeEach
    public void init() {
        mockWebServer = new MockWebServer();
        baseUrl = mockWebServer.url("/").toString();
        WebClient.Builder webClientBuilder = WebClient.builder().baseUrl(baseUrl);
        hipClient = new HIPClient(webClientBuilder);
    }

    @Test
    public void shouldCreateLinkReference() throws IOException, InterruptedException {
        var linkReference = TestBuilders.patientLinkReferenceResponse().build();
        var linkReferenceJson = new ObjectMapper().writeValueAsString(linkReference);

        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(linkReferenceJson));

        PatientLinkReferenceRequest request = TestBuilders.patientLinkReferenceRequestForHIP().build();
        StepVerifier.create(hipClient.linkPatientCareContext(request, baseUrl))
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

        assertThat(recordedRequest.getRequestUrl().toString()).isEqualTo(baseUrl + "patients/link");
        assertThat(recordedRequest.getBody().readUtf8())
                .isEqualTo(new ObjectMapper().writeValueAsString(request));
    }

    @Test
    public void shouldReturnPatientNotFoundError() throws IOException, InterruptedException {
        var errorResponse = TestBuilders.errorRepresentation().build();
        var errorResponseJson = new ObjectMapper().writeValueAsString(errorResponse);

        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(404)
                        .setHeader("Content-Type", "application/json")
                        .setBody(errorResponseJson));

        PatientLinkReferenceRequest request = TestBuilders.patientLinkReferenceRequestForHIP().build();
        StepVerifier.create(hipClient.linkPatientCareContext(request, baseUrl))
                .expectErrorSatisfies(
                        errorRes -> {
                            Assertions.assertThat(((ClientError) errorRes).getError().getError().getCode().getValue())
                                    .isEqualTo(errorResponse.getError().getCode().getValue());
                            Assertions.assertThat(((ClientError) errorRes).getError().getError().getMessage())
                                    .isEqualTo(errorResponse.getError().getMessage());
                        })
                .verify();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();

        assertThat(recordedRequest.getRequestUrl().toString()).isEqualTo(baseUrl + "patients/link");
        assertThat(recordedRequest.getBody().readUtf8())
                .isEqualTo(new ObjectMapper().writeValueAsString(request));
    }

    @Test
    public void shouldLinkCareContexts() throws IOException, InterruptedException {
        var linkRes = TestBuilders.patientLinkResponse().build();
        var linkResJson = new ObjectMapper().writeValueAsString(linkRes);

        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json")
                        .setBody(linkResJson));

        PatientLinkRequest request = TestBuilders.patientLinkRequest().build();
        String linkRefNumber = "test-ref-number";
        StepVerifier.create(hipClient.validateToken(linkRefNumber, request, baseUrl))
                .assertNext(
                        patientLinkResponse -> {
                            Assertions.assertThat(patientLinkResponse.getPatient().getReferenceNumber())
                                    .isEqualTo(linkRes.getPatient().getReferenceNumber());
                            Assertions.assertThat(patientLinkResponse.getPatient().getDisplay())
                                    .isEqualTo(linkRes.getPatient().getDisplay());
                            Assertions.assertThat(
                                    patientLinkResponse
                                            .getPatient()
                                            .getCareContexts()
                                            .get(0)
                                            .getReferenceNumber())
                                    .isEqualTo(linkRes.getPatient().getCareContexts().get(0).getReferenceNumber());
                            Assertions.assertThat(patientLinkResponse.getPatient().getCareContexts().get(0).getDisplay())
                                    .isEqualTo(linkRes.getPatient().getCareContexts().get(0).getDisplay());
                        })
                .verifyComplete();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();

        assertThat(recordedRequest.getRequestUrl().toString())
                .isEqualTo(baseUrl + "patients/link/test-ref-number");
        assertThat(recordedRequest.getBody().readUtf8())
                .isEqualTo(new ObjectMapper().writeValueAsString(request));
    }

    @Test
    public void shouldReturnOTPInvalidError() throws IOException, InterruptedException {
        var errorResponse = TestBuilders.errorRepresentation().build();
        var errorResponseJson = new ObjectMapper().writeValueAsString(errorResponse);
        mockWebServer.enqueue(
                new MockResponse()
                        .setResponseCode(404)
                        .setHeader("Content-Type", "application/json")
                        .setBody(errorResponseJson));

        PatientLinkRequest request = TestBuilders.patientLinkRequest().build();
        String linkRefNumber = "test-ref-number";
        StepVerifier.create(hipClient.validateToken(linkRefNumber, request, baseUrl))
                .expectErrorSatisfies(
                        errorRes -> {
                            Assertions.assertThat(((ClientError) errorRes).getError().getError().getCode().getValue())
                                    .isEqualTo(errorResponse.getError().getCode().getValue());
                            Assertions.assertThat(((ClientError) errorRes).getError().getError().getMessage())
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
