package in.projecteka.consentmanager.common;

import reactor.core.publisher.Mono;

public interface MonoVoidOperator {
    abstract Mono<Void> perform();
}
