package in.projecteka.consentmanager.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.clients.properties.GatewayServiceProperties;
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
import java.time.LocalDateTime;
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
        GatewayServiceProperties serviceProperties = new GatewayServiceProperties("http://ncg-gateway.com/v1", 1000,false);
        discoveryServiceClient = new DiscoveryServiceClient(webClientBuilder, () -> Mono.just(string()), serviceProperties);
    }

    @Test
    public void shouldPostDiscoverPatientRequestToGateway() throws IOException {
        var expectedPatient = patientInResponse()
                .display("Patient Name")
                .careContexts(List.of(careContext().display("Care context 1").build()))
                .build();
        var patientResponse = patientResponse().patient(expectedPatient).build();
        var patientResponseBody = new ObjectMapper().writeValueAsString(patientResponse);
        when(exchangeFunction.exchange(captor.capture())).thenReturn(Mono.just(
                ClientResponse.create(HttpStatus.ACCEPTED)
                        .header("Content-Type", "application/json")
                        .build()));
        var patientRequest = patientRequest()
                .patient(patientInRequest().build())
                .requestId(UUID.randomUUID())
                .timestamp(LocalDateTime.now().toString())
                .build();

        StepVerifier.create(
                discoveryServiceClient.requestPatientFor(patientRequest, "hipId"))
                .assertNext(response -> {
                    assertThat(response).isEqualTo(true);
                })
                .verifyComplete();
        assertThat(captor.getValue().url().toString()).isEqualTo("http://ncg-gateway.com/v1/care-contexts/discover");
    }
}
