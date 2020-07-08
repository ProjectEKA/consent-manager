package in.projecteka.consentmanager.common;

import in.projecteka.consentmanager.common.cache.CacheAdapter;
import lombok.AllArgsConstructor;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@AllArgsConstructor
public class RequestValidator {
    private final CacheAdapter<String, String> cacheForReplayAttack;

    public Mono<Boolean> validate(String requestId, String timestamp) {
        return isRequestIdPresent(requestId)
                .flatMap(isRequestIdPresent ->
                        Mono.just(!isRequestIdPresent && isRequestIdValidInGivenTimestamp(timestamp)));
    }

    private Mono<Boolean> isRequestIdPresent(String requestId) {
        return cacheForReplayAttack.get(requestId)
                .map(StringUtils::hasText)
                .switchIfEmpty(Mono.just(false));
    }

    private boolean isRequestIdValidInGivenTimestamp(String timestamp) {
        return  isValidTimestamp(toDate(timestamp));

    }

    private LocalDateTime toDate(String timestamp) {
        return LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private boolean isValidTimestamp(LocalDateTime timestamp) {
        LocalDateTime currentTime = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime startTime = currentTime.minusMinutes(1);
        LocalDateTime endTime = currentTime.plusMinutes(9);
        return timestamp.isAfter(startTime) && timestamp.isBefore(endTime);
    }
}
