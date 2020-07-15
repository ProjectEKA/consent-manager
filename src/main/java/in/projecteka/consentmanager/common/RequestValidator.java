package in.projecteka.consentmanager.common;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@AllArgsConstructor
public class RequestValidator {
    private final CacheAdapter<String, String> cacheForReplayAttack;
    private static final Logger logger = LoggerFactory.getLogger(RequestValidator.class);

    private String keyFor(String requestId) {
        return String.format("replay_%s", requestId);
    }

    public Mono<Void> put(String requestId, String timestamp) {
        return cacheForReplayAttack.put(keyFor(requestId), timestamp);
    }

    public Mono<Boolean> validate(String requestId, String timestamp) {
        return isRequestIdPresent(keyFor(requestId))
                .flatMap(result -> Mono.error(ClientError.tooManyRequests()))
                .then(Mono.just(isRequestIdValidInGivenTimestamp(timestamp)));
    }

    private Mono<Boolean> isRequestIdPresent(String requestId) {
        return cacheForReplayAttack.get(requestId).map(StringUtils::hasText);
    }

    private boolean isRequestIdValidInGivenTimestamp(String timestamp) {
        try {
            return isValidTimestamp(toDate(timestamp));
        } catch (DateTimeParseException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
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
