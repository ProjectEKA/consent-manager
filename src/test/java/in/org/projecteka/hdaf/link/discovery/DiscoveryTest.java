package in.org.projecteka.hdaf.link.discovery;

import in.org.projecteka.hdaf.link.ClientRegistryClient;
import in.org.projecteka.hdaf.link.discovery.model.Address;
import in.org.projecteka.hdaf.link.discovery.model.Telecom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;

import java.util.List;

import static in.org.projecteka.hdaf.link.discovery.TestBuilders.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DiscoveryTest {

    private Discovery discovery;

    @Mock
    ClientRegistryClient clientRegistryClient;

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
        when(clientRegistryClient.providersOf("Max")).thenReturn(Flux.just(provider));

        discovery.providersFrom("Max");

        verify(clientRegistryClient).providersOf("Max");
        assertThat(clientRegistryClient.providersOf("Max").collectList().block().get(0)).isEqualTo(provider);

    }

}