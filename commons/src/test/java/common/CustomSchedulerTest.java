package common;

import in.projecteka.library.common.DelayTimeoutException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static common.TestBuilders.string;
import static in.projecteka.library.common.CustomScheduler.scheduleThis;

class CustomSchedulerTest {

	@Test
	void observeTimeoutIfScheduledMoreThanConsumerExpected() {
		var producer = Mono.defer(() -> Mono.just(3));
		var scheduledProducer = scheduleThis(producer).timeout(Duration.ofSeconds(1));
		var resultProducer = scheduledProducer.responseFrom(discard -> Mono.never());

		StepVerifier.create(resultProducer)
				.expectTimeout(Duration.ofMillis(15))
				.verify();
	}

	@Test
	void observeValueWhenExtractorReturnsValue() {
		var result = string();
		var producer = Mono.defer(() -> Mono.just(string()));
		var scheduledProducer = scheduleThis(producer).timeout(Duration.ofMillis(1));
		var resultProducer = scheduledProducer.responseFrom(discard -> Mono.just(result));

		StepVerifier.create(resultProducer)
				.expectNext(result)
				.verifyComplete();
	}

	@Test
	void observeValueWhenExtractorReturnsValueAfterSometimes() {
		var atomicInteger = new AtomicInteger();
		var result = string();
		var producer = Mono.just(string());
		var scheduledProducer = scheduleThis(producer).timeout(Duration.ofSeconds(1));
		Function<String, Mono<String>> extractor = discard ->
				Mono.defer(() -> atomicInteger.getAndIncrement() < 3
				                 ? Mono.empty()
				                 : Mono.just(result));
		var resultProducer = scheduledProducer.responseFrom(extractor);

		StepVerifier.create(resultProducer)
				.expectNext(result)
				.verifyComplete();
	}

	@Test
	void observeEmptyWhenExtractorReturnsEmpty() {
		var producer = Mono.defer(() -> Mono.just(string()));
		var scheduledProducer = scheduleThis(producer).timeout(Duration.ofSeconds(2));
		var resultProducer = scheduledProducer.responseFrom(discard -> Mono.empty());

		StepVerifier.create(resultProducer)
				.verifyError(DelayTimeoutException.class);
	}

	@Test
	void observeErrorWhenExtractorReturnsError() {
		var producer = Mono.defer(() -> Mono.just(3));
		var scheduledProducer = scheduleThis(producer).timeout(Duration.ofMillis(1));
		var resultProducer = scheduledProducer.responseFrom(discard -> Mono.error(new Exception("Error message")));

		StepVerifier.create(resultProducer)
				.verifyErrorMessage("Error message");
	}
}
