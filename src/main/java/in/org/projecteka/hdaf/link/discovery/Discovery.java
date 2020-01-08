package in.org.projecteka.hdaf.link.discovery;

import in.org.projecteka.hdaf.link.ClientRegistryClient;
import in.org.projecteka.hdaf.link.discovery.model.Identifier;
import in.org.projecteka.hdaf.link.discovery.model.Provider;
import reactor.core.publisher.Flux;

public class Discovery {

    private final ClientRegistryClient client;

    public Discovery(ClientRegistryClient client) {
        this.client = client;
    }

    public Flux<ProviderRepresentation> providersFrom(String name) {
        return client.providersOf(name)
                .filter(this::isValid)
                .map(Transformer::to);
    }

    private boolean isValid(Provider provider) {
        return provider.getIdentifiers().stream().anyMatch(Identifier::isOfficial);
    }
}
