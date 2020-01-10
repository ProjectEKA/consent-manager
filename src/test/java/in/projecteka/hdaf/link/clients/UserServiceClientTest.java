package in.projecteka.hdaf.link.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.hdaf.clients.UserServiceClient;
import in.projecteka.hdaf.clients.properties.UserServiceProperties;
import in.projecteka.hdaf.link.discovery.model.User;
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

import static in.projecteka.hdaf.link.TestBuilders.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


public class UserServiceClientTest {
    @Captor
    private ArgumentCaptor<ClientRequest> captor;
    private UserServiceClient userServiceClient;
    @Mock
    private ExchangeFunction exchangeFunction;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        WebClient.Builder webClientBuilder = WebClient.builder()
                .exchangeFunction(exchangeFunction);
        UserServiceProperties userServiceProperties = new UserServiceProperties("http://user-service/");
        userServiceClient = new UserServiceClient(webClientBuilder, userServiceProperties);
    }


    @Test
    public void shouldGetUser() throws IOException {
        User user = user().firstName("first name").build();
        String patientResponseBody = new ObjectMapper().writeValueAsString(user);
        when(exchangeFunction.exchange(captor.capture())).thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(patientResponseBody).build()));

        StepVerifier.create(userServiceClient.userOf("1"))
                .assertNext(response -> {
                    assertThat(response.getFirstName()).isEqualTo(user.getFirstName());
                })
                .verifyComplete();

        assertThat(captor.getValue().url().toString()).isEqualTo("http://user-service/users/1/");
    }
}
