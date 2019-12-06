package in.org.projecteka.hdaf.link.discovery;

import in.org.projecteka.hdaf.link.ClientRegistryClient;
import reactor.core.publisher.Flux;

public class Discovery {

    private final ClientRegistryClient client;

    public Discovery(ClientRegistryClient client) {
        this.client = client;
    }

    public Flux<ProviderRepresentation> providersFrom(String name) {
       return client.providersOf(name)
               .map(Transformer::to);
    }
}
