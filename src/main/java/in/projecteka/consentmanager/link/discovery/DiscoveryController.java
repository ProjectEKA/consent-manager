package in.projecteka.consentmanager.link.discovery;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.common.Caller;
import in.projecteka.consentmanager.common.RequestValidator;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.link.discovery.model.patient.request.DiscoveryRequest;
import in.projecteka.consentmanager.link.discovery.model.patient.response.DiscoveryResponse;
import in.projecteka.consentmanager.link.discovery.model.patient.response.DiscoveryResult;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.UUID;

import static in.projecteka.consentmanager.link.Constants.APP_PATH_CARE_CONTEXTS_DISCOVER;
import static in.projecteka.consentmanager.link.Constants.PATH_CARE_CONTEXTS_ON_DISCOVER;

@RestController
@AllArgsConstructor
public class DiscoveryController {

    private static final String APP_PATH_GET_PROVIDER_BY_ID = "/providers/{provider-id}";
    private static final String APP_PATH_SEARCH_PROVIDERS = "/providers";
    private final Discovery discovery;
    private final CacheAdapter<String, String> cacheForReplayAttack;
    private final RequestValidator validator;

    @GetMapping(APP_PATH_SEARCH_PROVIDERS)
    public Flux<ProviderRepresentation> getProvidersByName(@RequestParam String name) {
        return discovery.providersFrom(name);
    }

    @GetMapping(APP_PATH_GET_PROVIDER_BY_ID)
    public Mono<ProviderRepresentation> getProvider(@PathVariable(value = "provider-id") String providerId) {
        return discovery.providerBy(providerId);
    }

    @PostMapping(APP_PATH_CARE_CONTEXTS_DISCOVER)
    public Mono<DiscoveryResponse> discoverPatientCareContexts(@RequestBody @Valid DiscoveryRequest discoveryRequest) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getUsername)
                .flatMap(user -> discovery.patientInHIP(user,
                        discoveryRequest.getUnverifiedIdentifiers(),
                        discoveryRequest.getHip().getId(),
                        newRequest(),
                        discoveryRequest.getRequestId()));
    }

    @PostMapping(PATH_CARE_CONTEXTS_ON_DISCOVER)
    public Mono<Void> onDiscoverPatientCareContexts(@RequestBody @Valid DiscoveryResult discoveryResult) {
        return Mono.just(discoveryResult)
                .filterWhen(req -> validator.validate(discoveryResult.getRequestId().toString()
                        , discoveryResult.getTimestamp().toString()))
                .switchIfEmpty(Mono.error(ClientError.tooManyRequests()))
                .flatMap(validatedRequest ->
                        cacheForReplayAttack.put(discoveryResult.getRequestId().toString(), discoveryResult.getTimestamp().toString())
                                .then(discovery.onDiscoverPatientCareContexts(discoveryResult)));
    }

    private UUID newRequest() {
        return UUID.randomUUID();
    }
}
