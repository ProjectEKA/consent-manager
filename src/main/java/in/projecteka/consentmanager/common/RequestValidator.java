package in.projecteka.consentmanager.common;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static in.projecteka.consentmanager.common.Constants.DEFAULT_CACHE_VALUE;
import static reactor.core.publisher.Mono.defer;
import static reactor.core.publisher.Mono.just;

@AllArgsConstructor
public class RequestValidator {
    private final CacheAdapter<String, LocalDateTime> cacheForReplayAttack;

    private String keyFor(String requestId) {
        return String.format("replay_%s", requestId);
    }

    public Mono<Void> put(String requestId, LocalDateTime timestamp) {
        return cacheForReplayAttack.put(keyFor(requestId), timestamp);
    }

    public Mono<Boolean> validate(String requestId, LocalDateTime timestamp) {
        return isRequestIdPresent(keyFor(requestId))
                .flatMap(result -> Mono.error(ClientError.tooManyRequests()))
                .then(defer(() -> just(isRequestIdValidInGivenTimestamp(timestamp))));
    }

    private Mono<Boolean> isRequestIdPresent(String key) {
        return cacheForReplayAttack.get(key).map(dateTime -> dateTime != DEFAULT_CACHE_VALUE);
    }

    private boolean isRequestIdValidInGivenTimestamp(LocalDateTime timestamp) {
        return isValidTimestamp(timestamp);
    }

    private boolean isValidTimestamp(LocalDateTime timestamp) {
        LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime startTime = currentTime.minusMinutes(1);
        LocalDateTime endTime = currentTime.plusMinutes(9);
        return timestamp.isAfter(startTime) && timestamp.isBefore(endTime);
    }
}
