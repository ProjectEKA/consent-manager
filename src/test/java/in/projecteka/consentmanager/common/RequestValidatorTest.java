package in.projecteka.consentmanager.common;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static in.projecteka.consentmanager.common.TestBuilders.string;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class RequestValidatorTest {

    @Mock
    private CacheAdapter<String, String> cacheForReplayAttack;

    @BeforeEach
    void setUp() {
        initMocks(this);
    }

    @Test
    void shouldReturnTrueForValidRequest() {
        String requestId = string();
        String timestamp = LocalDateTime.now(ZoneOffset.UTC).toString();
        RequestValidator validator = new RequestValidator(cacheForReplayAttack);

        when(cacheForReplayAttack.get("replay_" + requestId)).thenReturn(Mono.empty());

        StepVerifier
                .create(validator.validate(requestId, timestamp))
                .expectNext(true)
                .expectComplete()
                .verify();
    }

    @Test
    void shouldThrowTooManyRequestsErrorIfRequestIdIsAlreadyCached() {
        String requestId = string();
        String timestamp = LocalDateTime.now(ZoneOffset.UTC).toString();
        RequestValidator validator = new RequestValidator(cacheForReplayAttack);

        when(cacheForReplayAttack.get("replay_" + requestId)).thenReturn(Mono.just("testString"));

        StepVerifier
                .create(validator.validate(requestId, timestamp))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is4xxClientError())
                .verify();
    }

    @Test
    void shouldReturnFalseIfTimestampIsInvalid() {
        String requestId = string();
        String timestamp = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(2).toString();
        RequestValidator validator = new RequestValidator(cacheForReplayAttack);

        when(cacheForReplayAttack.get("replay_" + requestId)).thenReturn(Mono.empty());

        StepVerifier
                .create(validator.validate(requestId, timestamp))
                .expectNext(false)
                .expectComplete()
                .verify();
    }

    @Test
    void shouldReturnFalseIfTimestampIsExpired() {
        String requestId = string();
        String timestamp = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10).toString();
        RequestValidator validator = new RequestValidator(cacheForReplayAttack);

        when(cacheForReplayAttack.get("replay_" + requestId)).thenReturn(Mono.empty());

        StepVerifier
                .create(validator.validate(requestId, timestamp))
                .expectNext(false)
                .expectComplete()
                .verify();
    }

    @Test
    void shouldReturnFalseIfTimestampIsInNotValidFormat() {
        String requestId = string();
        String timestamp = string();
        RequestValidator validator = new RequestValidator(cacheForReplayAttack);
        when(cacheForReplayAttack.get("replay_" + requestId)).thenReturn(Mono.empty());

        StepVerifier
                .create(validator.validate(requestId, timestamp))
                .expectNext(false)
                .expectComplete()
                .verify();
    }
}