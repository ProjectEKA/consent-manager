package in.org.projecteka.hdaf.link.discovery;

import in.org.projecteka.hdaf.link.discovery.model.patient.response.DiscoveryResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@AllArgsConstructor
public class DiscoveryController {

    private Discovery discovery;

    @GetMapping("/providers")
    public Flux<ProviderRepresentation> getProvidersByName(@RequestParam String name) {
        return discovery.providersFrom(name);
    }

    @PostMapping("/patient")
    public Mono<DiscoveryResponse> findPatient(@RequestParam String providerId, @RequestHeader String patientId) {
        return discovery.patientFor(providerId, patientId, UUID.randomUUID().toString());
    }
}