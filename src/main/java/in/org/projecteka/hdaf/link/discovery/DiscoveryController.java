package in.org.projecteka.hdaf.link.discovery;

import in.org.projecteka.hdaf.link.discovery.model.patient.Patient;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
public class DiscoveryController {

    private Discovery discovery;

    @GetMapping("/providers")
    public Flux<ProviderRepresentation> getProvidersByName(@RequestParam String name) {
        return discovery.providersFrom(name);
    }

    @GetMapping("/patient")
    public Mono<Patient> findPatient(@RequestParam String providerId, @RequestHeader String patientId) {
        return discovery.patientFor(providerId, patientId);
    }
}