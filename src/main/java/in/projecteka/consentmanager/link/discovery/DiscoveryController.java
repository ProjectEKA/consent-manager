package in.projecteka.consentmanager.link.discovery;

import in.projecteka.consentmanager.common.Caller;
import in.projecteka.consentmanager.link.discovery.model.patient.request.DiscoveryRequest;
import in.projecteka.consentmanager.link.discovery.model.patient.response.DiscoveryResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.UUID;

@RestController
@AllArgsConstructor
public class DiscoveryController {

    private final Discovery discovery;

    @GetMapping("/providers")
    public Flux<ProviderRepresentation> getProvidersByName(@RequestParam String name) {
        return discovery.providersFrom(name);
    }

    @PostMapping("/patients/discover/carecontexts")
    public Mono<DiscoveryResponse> findPatient(@RequestBody @Valid DiscoveryRequest discoveryRequest) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getUserName)
                .flatMap(user -> discovery.patientFor(user, discoveryRequest.getUnverifiedIdentifiers(), discoveryRequest.getHip().getId(),
                        newTransaction()));
    }

    private String newTransaction() {
        return UUID.randomUUID().toString();
    }
}
