package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.consent.ConsentServiceProperties;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.function.Predicate.not;

@AllArgsConstructor
public class PolicyServiceClient {
    private final WebClient webClient;
    private final Supplier<Mono<String>> tokenGenerator;
    private final ConsentServiceProperties consentServiceProperties;

    public Mono<Void> checkPolicyFor(String consentRequest) {
        return tokenGenerator.get()
                .flatMap(token -> webClient
                        .post()
                        .uri(format("%s/internal/policy/consent-request/%s",consentServiceProperties.getUrl(), consentRequest))
                        .header("Authorization", token)
                        .retrieve()
                        .onStatus(not(HttpStatus::is2xxSuccessful),
                                clientResponse -> Mono.error(ClientError.unknownErrorOccurred()))
                        .toBodilessEntity())
                .then(Mono.empty());
    }
}
