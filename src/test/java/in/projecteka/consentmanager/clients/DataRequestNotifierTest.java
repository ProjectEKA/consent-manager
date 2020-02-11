package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.dataflow.model.hip.DataFlowRequest;
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

import static in.projecteka.consentmanager.dataflow.TestBuilders.dataFlowRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class DataRequestNotifierTest {
    @Captor
    private ArgumentCaptor<ClientRequest> captor;
    private DataRequestNotifier dataRequestNotifier;
    @Mock
    private ExchangeFunction exchangeFunction;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        WebClient.Builder webClientBuilder = WebClient.builder()
                .exchangeFunction(exchangeFunction);
        dataRequestNotifier = new DataRequestNotifier(webClientBuilder);
    }

    @Test
    public void shouldDiscoverPatients() throws IOException {
        when(exchangeFunction.exchange(captor.capture())).thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .build()));
        DataFlowRequest dataFlowRequest = dataFlowRequestBuilder().build();

        StepVerifier.create(dataRequestNotifier.notifyHip(dataFlowRequest, "http://hip-url/"))
                .verifyComplete();

        assertThat(captor.getValue().url().toString()).isEqualTo("http://hip-url/health-information/request");
    }
}
