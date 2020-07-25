package in.projecteka.consentmanager.common;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.common.cache.LoadingCacheGenericAdapter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static in.projecteka.consentmanager.common.TestBuilders.string;

class RequestValidatorTest {

    private static CacheAdapter<String, LocalDateTime> cacheForReplayAttack;

    @BeforeAll
    static void setUp() {
        cacheForReplayAttack = new LoadingCacheGenericAdapter<LocalDateTime>(CacheBuilder
                .newBuilder()
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .build(new CacheLoader<>() {
                    public LocalDateTime load(String key) {
                        return LocalDateTime.MIN;
                    }
                }));
    }

    private static Stream<Arguments> nonAllowedDates() {
        var pastDate = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(2);
        var futureDate = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10);
        return Stream.of(Arguments.of(pastDate), Arguments.of(futureDate));
    }

    @ParameterizedTest
    @MethodSource("nonAllowedDates")
    void shouldReturnFalseIfTimestampIsExpired(LocalDateTime time) {
        RequestValidator validator = new RequestValidator(cacheForReplayAttack);

        StepVerifier
                .create(validator.validate(string(), time))
                .expectNext(false)
                .expectComplete()
                .verify();
    }


    @Test
    void returnErrorIfEntryExists() {
        var requestValidator = new RequestValidator(cacheForReplayAttack);
        var requestId = string();
        var timestamp = LocalDateTime.now(ZoneOffset.UTC);
        requestValidator.put(requestId, timestamp).subscribe().dispose();

        StepVerifier
                .create(requestValidator.validate(requestId, timestamp))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is4xxClientError())
                .verify();
    }

    @Test
    void returnTrueIfEntryDoesNotExists() {
        var requestValidator = new RequestValidator(cacheForReplayAttack);

        StepVerifier
                .create(requestValidator.validate(string(), LocalDateTime.now(ZoneOffset.UTC)))
                .expectNext(true)
                .expectComplete()
                .verify();
    }
}