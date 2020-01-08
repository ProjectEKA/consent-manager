package in.org.projecteka.hdaf.link.discovery;

import in.org.projecteka.hdaf.link.ClientRegistryClient;
import in.org.projecteka.hdaf.link.discovery.model.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;


import static in.org.projecteka.hdaf.link.discovery.TestBuilders.*;
import static java.util.List.of;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class DiscoveryTest {

    @Mock
    ClientRegistryClient clientRegistryClient;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void returnProvidersWithOfficial() {
        var discovery = new Discovery(clientRegistryClient);
        var address = address().use("work").build();
        var telecommunication = telecom().use("work").build();
        var identifier = identifier().use(Identifier.IdentifierType.OFFICIAL.toString()).build();
        var provider = provider()
                .addresses(of(address))
                .telecoms(of(telecommunication))
                .identifiers(of(identifier))
                .name("Max")
                .build();
        when(clientRegistryClient.providersOf(eq("Max"))).thenReturn(Flux.just(provider));

        StepVerifier.create(discovery.providersFrom("Max"))
                .expectNext(Transformer.to(provider))
                .verifyComplete();
    }

    @Test
    public void returnEmptyProvidersWhenOfficialIdentifierIsUnavailable() {
        var discovery = new Discovery(clientRegistryClient);
        var address = address().use("work").build();
        var telecommunication = telecom().use("work").build();
        var identifier = identifier().build();
        var provider = provider()
                .addresses(of(address))
                .telecoms(of(telecommunication))
                .identifiers(of(identifier))
                .name("Max")
                .build();
        when(clientRegistryClient.providersOf(eq("Max"))).thenReturn(Flux.just(provider));

        StepVerifier.create(discovery.providersFrom("Max"))
                .verifyComplete();
    }
}