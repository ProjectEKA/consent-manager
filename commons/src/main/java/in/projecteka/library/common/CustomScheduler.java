package in.projecteka.library.common;

import lombok.AllArgsConstructor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;
import java.util.function.Function;

@AllArgsConstructor
public class CustomScheduler<T> {
	private static final Logger logger = LoggerFactory.getLogger(CustomScheduler.class);
	static final Duration Quarter_Second = Duration.ofMillis(100);
	static final Duration Half_A_Second = Duration.ofMillis(500);

	Mono<T> producer;

	public Wrapper<T> timeout(Duration duration) {
		return new Wrapper<>(producer, duration);
	}

	@AllArgsConstructor
	public static class Wrapper<T> {
		Mono<T> producer;
		Duration timeout;

		public <U> Mono<U> responseFrom(Function<T, Mono<U>> extractor) {
			return producer
					.flatMap(result -> extractor.apply(result)
							.repeatWhenEmpty(exponentialBackOff(Quarter_Second, Half_A_Second, timeout)));
		}
	}

	public static <T> CustomScheduler<T> scheduleThis(Mono<T> producer) {
		return new CustomScheduler<>(producer);
	}


	public static Function<Flux<Long>, Publisher<?>> exponentialBackOff(Duration minimum,
	                                                                    Duration maximum,
	                                                                    Duration timeout) {
		Instant finish = Instant.now().plus(timeout);
		return iterations -> getDelay(minimum, maximum, finish, iterations);
	}

	private static Flux<?> getDelay(Duration minimum, Duration maximum, Instant finish, Flux<Long> iterations) {
		return iterations
				.map(iteration -> calculateDuration(minimum, maximum, iteration))
				.concatMap(delay -> {
					if (Instant.now().isAfter(finish)) {
						return Mono.error(new DelayTimeoutException());
					}
					return Mono
							.delay(delay)
							.doOnSubscribe(logDelay(delay));
				});
	}

	private static Consumer<Subscription> logDelay(Duration delay) {
		return subscription -> {
			int seconds = (int) delay.getSeconds();
			if (seconds > 0) {
				logger.info("Delaying {} seconds", seconds);
				return;
			}

			int milliseconds = (int) delay.toMillis();
			logger.info("Delaying {} millis", milliseconds);
		};
	}

	private static Duration calculateDuration(Duration minimum, Duration maximum, Long iteration) {
		Duration candidate = minimum.multipliedBy((long) Math.pow(2, iteration));
		return min(candidate, maximum);
	}

	private static Duration min(Duration a, Duration b) {
		return (a.compareTo(b) <= 0) ? a : b;
	}
}
