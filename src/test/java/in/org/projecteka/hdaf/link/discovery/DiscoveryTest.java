package in.org.projecteka.hdaf.link.discovery;

import in.org.projecteka.hdaf.clients.ClientRegistryClient;
import in.org.projecteka.hdaf.link.discovery.model.Address;
import in.org.projecteka.hdaf.link.discovery.model.Telecom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static in.org.projecteka.hdaf.link.discovery.TestBuilders.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class DiscoveryTest {

    @Mock
    ClientRegistryClient clientRegistryClient;

    private Discovery discovery;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void providersOfCalledWithMax() {
        discovery = new Discovery(clientRegistryClient);
        Address address = address().use("work").build();
        Telecom telecom = telecom().use("work").build();
        var provider = provider()
                .addresses(List.of(address))
                .telecoms(List.of(telecom))
                .name("Max")
                .build();
        when(clientRegistryClient.providersOf(eq("Max"))).thenReturn(Flux.just(provider));

        StepVerifier.create(discovery.providersFrom("Max"))
                .expectNext(Transformer.to(provider))
                .verifyComplete();
    }
}