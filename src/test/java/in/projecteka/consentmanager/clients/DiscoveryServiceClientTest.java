package in.projecteka.consentmanager.clients;

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
import java.util.UUID;

import static in.projecteka.consentmanager.clients.TestBuilders.patientInRequest;
import static in.projecteka.consentmanager.clients.TestBuilders.patientRequest;
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
        var serviceProperties = new GatewayServiceProperties("http://ncg-gateway.com/v1", 1000, false, "", "", "");
        discoveryServiceClient = new DiscoveryServiceClient(webClientBuilder.build(),
                () -> Mono.just(string()),
                serviceProperties);
    }

    @Test
    public void shouldPostDiscoverPatientRequestToGateway() throws IOException {
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
                .assertNext(response -> assertThat(response).isTrue())
                .verifyComplete();
        assertThat(captor.getValue().url().toString()).hasToString("http://ncg-gateway.com/v1/care-contexts/discover");
    }
}
