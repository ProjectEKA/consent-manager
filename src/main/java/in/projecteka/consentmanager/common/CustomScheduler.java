package in.projecteka.consentmanager.common;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.function.Function;

@AllArgsConstructor
public class CustomScheduler<T> {
	Mono<T> producer;

	public Wrapper<T> over(Duration duration) {
		return new Wrapper<>(producer.zipWith(Mono.delay(duration)));
	}

	@AllArgsConstructor
	public static class Wrapper<T> {
		Mono<Tuple2<T, Long>> producer;

		public <U> Mono<U> responseFrom(Function<Tuple2<T, Long>, Mono<U>> extractor) {
			return producer.flatMap(extractor);
		}
	}

	public static <T> CustomScheduler<T> scheduleThis(Mono<T> producer) {
		return new CustomScheduler<>(producer);
	}
}
