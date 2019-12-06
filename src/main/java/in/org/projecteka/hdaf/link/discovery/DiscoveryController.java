package in.org.projecteka.hdaf.link.discovery;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/providers")
@AllArgsConstructor
public class DiscoveryController {

    private Discovery discovery;

    @GetMapping
    public Flux<ProviderRepresentation> getProvidersByName(@RequestParam String name) {
        return discovery.providersFrom(name);
    }
}
