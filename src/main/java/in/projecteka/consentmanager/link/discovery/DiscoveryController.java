package in.projecteka.consentmanager.link.discovery;

import in.projecteka.consentmanager.common.Authenticator;
import in.projecteka.consentmanager.common.Caller;
import in.projecteka.consentmanager.link.discovery.model.patient.request.DiscoveryRequest;
import in.projecteka.consentmanager.link.discovery.model.patient.response.DiscoveryResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
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

    private final Discovery discovery;
    private final Authenticator authenticator;

    @GetMapping("/providers")
    public Flux<ProviderRepresentation> getProvidersByName(@RequestParam String name) {
        return discovery.providersFrom(name);
    }

    @PostMapping("/patients/discover")
    public Mono<DiscoveryResponse> findPatient(@RequestHeader(value = HttpHeaders.AUTHORIZATION) String token,
                                               @RequestBody DiscoveryRequest discoveryRequest) {
        return authenticator.userFrom(token)
                .map(Caller::getUserName)
                .flatMap(user -> discovery.patientFor(discoveryRequest.getHip().getId(), user, newTransaction())
                        .subscriberContext(context -> context.put(HttpHeaders.AUTHORIZATION, token)));
    }

    private String newTransaction() {
        return UUID.randomUUID().toString();
    }
}
