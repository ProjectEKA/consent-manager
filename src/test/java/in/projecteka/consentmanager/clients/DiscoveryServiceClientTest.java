package in.projecteka.consentmanager.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static in.projecteka.consentmanager.clients.TestBuilders.careContext;
import static in.projecteka.consentmanager.clients.TestBuilders.patientInRequest;
import static in.projecteka.consentmanager.clients.TestBuilders.patientInResponse;
import static in.projecteka.consentmanager.clients.TestBuilders.patientRequest;
import static in.projecteka.consentmanager.clients.TestBuilders.patientResponse;
import static in.projecteka.consentmanager.clients.TestBuilders.string;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class DiscoveryServiceClientTest {
    @Captor
    private ArgumentCaptor<ClientRequest> captor;
    private DiscoveryServiceClient discoveryServiceClient;
    @Mock
    private ExchangeFunction exchangeFunction;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        WebClient.Builder webClientBuilder = WebClient.builder().exchangeFunction(exchangeFunction);
        discoveryServiceClient = new DiscoveryServiceClient(webClientBuilder, () -> Mono.just(string()));
    }

    @Test
    public void shouldDiscoverPatients() throws IOException {
        var expectedPatient = patientInResponse()
                .display("Patient Name")
                .careContexts(List.of(careContext().display("Care context 1").build()))
                .build();
        var patientResponse = patientResponse().patient(expectedPatient).build();
        var patientResponseBody = new ObjectMapper().writeValueAsString(patientResponse);
        when(exchangeFunction.exchange(captor.capture())).thenReturn(Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body(patientResponseBody)
                        .build()));
        var patientRequest = patientRequest()
                .patient(patientInRequest().build())
                .requestId(UUID.randomUUID())
                .build();

        StepVerifier.create(discoveryServiceClient.patientFor(patientRequest, "http://hip-url/"))
                .assertNext(response -> {
                    assertThat(response.getPatient().getDisplay()).isEqualTo(expectedPatient.getDisplay());
                    assertThat(response.getPatient().getReferenceNumber())
                            .isEqualTo(expectedPatient.getReferenceNumber());
                    assertThat(response.getPatient().getMatchedBy()).isEqualTo(expectedPatient.getMatchedBy());
                    assertThat(response.getPatient().getCareContexts().get(0).getDisplay())
                            .isEqualTo(expectedPatient.getCareContexts().get(0).getDisplay());
                })
                .verifyComplete();
        assertThat(captor.getValue().url().toString()).isEqualTo("http://hip-url/patients/discover/carecontexts");
    }
}
