package in.projecteka.consentmanager.link.discovery;

import in.projecteka.consentmanager.common.TokenUtils;
import in.projecteka.consentmanager.link.discovery.model.patient.request.DiscoveryRequest;
import in.projecteka.consentmanager.link.discovery.model.patient.response.DiscoveryResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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

    @PostMapping("/patients/discover")
    public Mono<DiscoveryResponse> findPatient(@RequestHeader(value = "Authorization") String authorization,
                                               @RequestBody DiscoveryRequest discoveryRequest) {
        String patientId = TokenUtils.getCallerId(authorization);
        return discovery.patientFor(discoveryRequest.getHip().getId(), patientId, generateNewTransaction());
    }

    private String generateNewTransaction() {
        return UUID.randomUUID().toString();
    }
}
