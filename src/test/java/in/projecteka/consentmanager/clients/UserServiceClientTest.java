package in.projecteka.consentmanager.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.clients.model.User;
import in.projecteka.consentmanager.clients.properties.GatewayServiceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

import static in.projecteka.consentmanager.clients.TestBuilders.string;
import static in.projecteka.consentmanager.clients.TestBuilders.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class UserServiceClientTest {
    @Captor
    private ArgumentCaptor<ClientRequest> captor;
    @Mock
    private ExchangeFunction exchangeFunction;

    private UserServiceClient userServiceClient;
    private String token;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        WebClient.Builder webClientBuilder = WebClient.builder().exchangeFunction(exchangeFunction);
        GatewayServiceProperties serviceProperties = new GatewayServiceProperties("http://example.com", 1000);
        token = string();
        userServiceClient = new UserServiceClient(webClientBuilder, "http://user-service/", () -> Mono.just(token), serviceProperties);
    }

    @Test
    public void shouldGetUser() throws IOException {
        User user = user().name("first name").build();
        String patientResponseBody = new ObjectMapper().writeValueAsString(user);
        when(exchangeFunction.exchange(captor.capture()))
                .thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body(patientResponseBody).build()));

        StepVerifier.create(userServiceClient.userOf("1"))
                .assertNext(response -> assertThat(response.getName()).isEqualTo(user.getName()))
                .verifyComplete();

        assertThat(captor.getValue().url().toString()).isEqualTo("http://user-service/internal/users/1/");
        assertThat(captor.getValue().headers().get(HttpHeaders.AUTHORIZATION).get(0)).isEqualTo(token);
    }
}
