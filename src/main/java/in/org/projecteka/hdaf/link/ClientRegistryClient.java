package in.org.projecteka.hdaf.link;

import in.org.projecteka.hdaf.link.discovery.model.Provider;
import reactor.core.publisher.Flux;

public interface ClientRegistryClient {
    Flux<Provider> providersOf(String name);
}
