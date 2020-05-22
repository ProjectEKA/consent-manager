package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.user.model.User;
import reactor.core.publisher.Mono;

import java.util.List;

public interface FilterStrategy<T> {
    public Mono<List<User>> filter(List<User> users, T filterValue);
}
