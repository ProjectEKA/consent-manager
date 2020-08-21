package in.projecteka.user.filters;

import in.projecteka.user.model.User;
import reactor.core.publisher.Mono;

import java.util.List;

public interface FilterStrategy<T> {
    Mono<List<User>> filter(List<User> users, T filterValue);
}
