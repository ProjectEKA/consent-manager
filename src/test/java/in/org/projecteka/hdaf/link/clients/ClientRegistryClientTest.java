package in.org.projecteka.hdaf.link.clients;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.org.projecteka.hdaf.clients.ClientRegistryClient;
import in.org.projecteka.hdaf.clients.properties.ClientRegistryProperties;
import in.org.projecteka.hdaf.link.TestBuilders;
import in.org.projecteka.hdaf.link.discovery.model.Provider;
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

import static in.org.projecteka.hdaf.link.TestBuilders.*;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ClientRegistryClientTest {
    @Captor
    private ArgumentCaptor<ClientRequest> captor;
    private ClientRegistryClient clientRegistryClient;
    @Mock
    private ExchangeFunction exchangeFunction;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        WebClient.Builder webClientBuilder = WebClient.builder()
                .exchangeFunction(exchangeFunction);
        ClientRegistryProperties clientRegistryProperties = new ClientRegistryProperties("localhost:8000", "", "");
        clientRegistryClient = new ClientRegistryClient(webClientBuilder, clientRegistryProperties);
    }


    @Test
    public void getProvidersByGivenName() throws IOException {
        var source = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readValue(
                ClassLoader.getSystemClassLoader().getResource("provider.json"),
                new TypeReference<List<Provider>>() {
                });
        var jsonNode = new ObjectMapper().readValue(
                ClassLoader.getSystemClassLoader().getResource("provider.json"),
                new TypeReference<List<JsonNode>>() {
                });

        when(exchangeFunction.exchange(captor.capture())).thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(jsonNode.toString()).build()));

        StepVerifier.create(clientRegistryClient.providersOf("Max"))
                .assertNext(provider -> {
                    assertThat(provider.getName()).isEqualTo(source.get(0).getName());
                    assertThat(provider.getAddresses().get(0).getCity()).isEqualTo(source.get(0).getAddresses().get(0).getCity());
                    assertThat(provider.getTelecoms().get(0).getValue()).isEqualTo(source.get(0).getTelecoms().get(0).getValue());
                    assertThat(provider.getTypes().get(0).getCoding().get(0).getCode()).isEqualTo(source.get(0).getTypes().get(0).getCoding().get(0).getCode());
                })
                .verifyComplete();

        assertThat(captor.getValue().url().toString()).isEqualTo("localhost:8000/providers?name=Max");
    }

    @Test
    public void getProviderByGivenId() throws IOException {
        Provider provider = provider().name("Provider")
                .addresses(of(address().city("Provider City").build()))
                .telecoms(of(telecom().value("Telecom").build()))
                .types(of(type().coding(of(coding().code("Code").build())).build())).build();

        String providerJsonResponse = new ObjectMapper().writeValueAsString(provider);
        when(exchangeFunction.exchange(captor.capture())).thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(providerJsonResponse).build()));

        StepVerifier.create(clientRegistryClient.providerWith("10000003"))
                .assertNext(providerResponse -> {
                    assertThat(providerResponse.getName()).isEqualTo(provider.getName());
                    assertThat(providerResponse.getAddresses().get(0).getCity()).isEqualTo(provider.getAddresses().get(0).getCity());
                    assertThat(providerResponse.getTelecoms().get(0).getValue()).isEqualTo(provider.getTelecoms().get(0).getValue());
                    assertThat(providerResponse.getTypes().get(0).getCoding().get(0).getCode()).isEqualTo(provider.getTypes().get(0).getCoding().get(0).getCode());
                })
                .verifyComplete();

        assertThat(captor.getValue().url().toString()).isEqualTo("localhost:8000/providers/10000003");
    }
}
