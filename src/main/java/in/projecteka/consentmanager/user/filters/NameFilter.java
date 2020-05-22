package in.projecteka.consentmanager.user.filters;

import in.projecteka.consentmanager.user.model.User;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

public class NameFilter implements FilterStrategy<String> {
    @Override
    public Mono<List<User>> filter(List<User> users,String name) {
        if (name == null) {
            return Mono.just(users);
        }
        List<User> filteredRows = users.stream().filter(row -> row.getName() != null && row.getName().equals(name)).collect(Collectors.toList());
        return Mono.just(filteredRows);
    }
}
