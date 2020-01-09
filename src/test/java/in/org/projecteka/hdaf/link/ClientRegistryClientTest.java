package in.org.projecteka.hdaf.link;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.org.projecteka.hdaf.link.discovery.model.Identifier;
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

import static in.org.projecteka.hdaf.link.TestBuilders.*;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ClientRegistryClientTest {
  @Captor ArgumentCaptor<ClientRequest> captor;
  ClientRegistryClient clientRegistryClient;
  @Mock private ExchangeFunction exchangeFunction;

  @BeforeEach
  void init() {
    MockitoAnnotations.initMocks(this);
    WebClient.Builder webClientBuilder = WebClient.builder().exchangeFunction(exchangeFunction);
    ClientRegistryProperties clientRegistryProperties =
        new ClientRegistryProperties("localhost:8000", "", "");
    clientRegistryClient = new ClientRegistryClient(webClientBuilder, clientRegistryProperties);
  }

  @Test
  void getProvidersByGivenName() throws IOException {
    var address = address().use("work").build();
    var telecommunication = telecom().use("work").build();
    var identifier = identifier().use(Identifier.IdentifierType.OFFICIAL.toString()).build();
    var source =
        provider()
            .addresses(of(address))
            .telecoms(of(telecommunication))
            .identifiers(of(identifier))
            .name("Max")
            .build();
    var providerJson = new ObjectMapper().writeValueAsString(source);
    when(exchangeFunction.exchange(captor.capture()))
        .thenReturn(
            Mono.just(
                ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(providerJson)
                    .build()));

    StepVerifier.create(clientRegistryClient.providersOf("Max"))
        .assertNext(
            provider -> {
              assertThat(provider.getName()).isEqualTo(source.getName());
              assertThat(provider.getAddresses().get(0).getCity())
                  .isEqualTo(source.getAddresses().get(0).getCity());
              assertThat(provider.getTelecoms().get(0).getValue())
                  .isEqualTo(source.getTelecoms().get(0).getValue());
              assertThat(provider.getTypes().get(0).getCoding().get(0).getCode())
                  .isEqualTo(source.getTypes().get(0).getCoding().get(0).getCode());
            })
        .verifyComplete();

    assertThat(captor.getValue().url().toString()).isEqualTo("localhost:8000/providers?name=Max");
  }

  @Test
  void getProviderById() throws IOException {
    var address = address().use("work").build();
    var telecommunication = telecom().use("work").build();
    var identifier = identifier().use(Identifier.IdentifierType.OFFICIAL.toString()).build();
    var source =
        provider()
            .addresses(of(address))
            .telecoms(of(telecommunication))
            .identifiers(of(identifier))
            .name("Max")
            .build();
    var providerJson = new ObjectMapper().writeValueAsString(source);
    when(exchangeFunction.exchange(captor.capture()))
        .thenReturn(
            Mono.just(
                ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(providerJson)
                    .build()));

    StepVerifier.create(clientRegistryClient.providerWith("10000005"))
        .assertNext(
            provider -> {
              assertThat(provider.getName()).isEqualTo(source.getName());
              assertThat(provider.getAddresses().get(0).getCity())
                  .isEqualTo(source.getAddresses().get(0).getCity());
              assertThat(provider.getTelecoms().get(0).getValue())
                  .isEqualTo(source.getTelecoms().get(0).getValue());
              assertThat(provider.getTypes().get(0).getCoding().get(0).getCode())
                  .isEqualTo(source.getTypes().get(0).getCoding().get(0).getCode());
            })
        .verifyComplete();

    assertThat(captor.getValue().url().toString()).isEqualTo("localhost:8000/providers/10000005");
  }
}
