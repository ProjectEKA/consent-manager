package in.projecteka.hdaf.link.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.hdaf.clients.ClientRegistryClient;
import in.projecteka.hdaf.clients.properties.ClientRegistryProperties;
import in.projecteka.hdaf.link.discovery.model.Identifier;
import in.projecteka.hdaf.link.discovery.model.Provider;
import org.assertj.core.api.Assertions;
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

import static in.projecteka.hdaf.link.TestBuilders.address;
import static in.projecteka.hdaf.link.TestBuilders.coding;
import static in.projecteka.hdaf.link.TestBuilders.identifier;
import static in.projecteka.hdaf.link.TestBuilders.provider;
import static in.projecteka.hdaf.link.TestBuilders.telecom;
import static in.projecteka.hdaf.link.TestBuilders.type;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ClientRegistryClientTest {
  private @Captor ArgumentCaptor<ClientRequest> captor;
  private ClientRegistryClient clientRegistryClient;
  @Mock private ExchangeFunction exchangeFunction;

  @BeforeEach
  public void init() {
    MockitoAnnotations.initMocks(this);
    WebClient.Builder webClientBuilder = WebClient.builder().exchangeFunction(exchangeFunction);
    ClientRegistryProperties clientRegistryProperties =
        new ClientRegistryProperties("localhost:8000", "", "");
    clientRegistryClient = new ClientRegistryClient(webClientBuilder, clientRegistryProperties);
  }

  @Test
  public void getProvidersByGivenName() throws IOException {
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
              Assertions.assertThat(provider.getName()).isEqualTo(source.getName());
              Assertions.assertThat(provider.getAddresses().get(0).getCity())
                  .isEqualTo(source.getAddresses().get(0).getCity());
              Assertions.assertThat(provider.getTelecoms().get(0).getValue())
                  .isEqualTo(source.getTelecoms().get(0).getValue());
              Assertions.assertThat(provider.getTypes().get(0).getCoding().get(0).getCode())
                  .isEqualTo(source.getTypes().get(0).getCoding().get(0).getCode());
            })
        .verifyComplete();

    assertThat(captor.getValue().url().toString()).isEqualTo("localhost:8000/providers?name=Max");
  }

  @Test
  public void getProviderById() throws IOException {
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
              Assertions.assertThat(provider.getName()).isEqualTo(source.getName());
              Assertions.assertThat(provider.getAddresses().get(0).getCity())
                  .isEqualTo(source.getAddresses().get(0).getCity());
              Assertions.assertThat(provider.getTelecoms().get(0).getValue())
                  .isEqualTo(source.getTelecoms().get(0).getValue());
              Assertions.assertThat(provider.getTypes().get(0).getCoding().get(0).getCode())
                  .isEqualTo(source.getTypes().get(0).getCoding().get(0).getCode());
            })
        .verifyComplete();

    assertThat(captor.getValue().url().toString()).isEqualTo("localhost:8000/providers/10000005");
  }

  @Test
  public void getProviderByGivenId() throws IOException {
    Provider provider =
        provider()
            .name("Provider")
            .addresses(of(address().city("Provider City").build()))
            .telecoms(of(telecom().value("Telecom").build()))
            .types(of(type().coding(of(coding().code("Code").build())).build()))
            .build();

    String providerJsonResponse = new ObjectMapper().writeValueAsString(provider);
    when(exchangeFunction.exchange(captor.capture()))
        .thenReturn(
            Mono.just(
                ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(providerJsonResponse)
                    .build()));

    StepVerifier.create(clientRegistryClient.providerWith("10000003"))
        .assertNext(
            providerResponse -> {
              Assertions.assertThat(providerResponse.getName()).isEqualTo(provider.getName());
              Assertions.assertThat(providerResponse.getAddresses().get(0).getCity())
                  .isEqualTo(provider.getAddresses().get(0).getCity());
              Assertions.assertThat(providerResponse.getTelecoms().get(0).getValue())
                  .isEqualTo(provider.getTelecoms().get(0).getValue());
              Assertions.assertThat(providerResponse.getTypes().get(0).getCoding().get(0).getCode())
                  .isEqualTo(provider.getTypes().get(0).getCoding().get(0).getCode());
            })
        .verifyComplete();

    assertThat(captor.getValue().url().toString()).isEqualTo("localhost:8000/providers/10000003");
  }
}
