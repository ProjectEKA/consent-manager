package in.projecteka.user.filters;

import in.projecteka.user.model.PatientName;
import in.projecteka.user.model.User;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

public class NameFilter implements FilterStrategy<PatientName> {
    @Override
    public Mono<List<User>> filter(List<User> users, PatientName name) {
        if (name == null) {
            return Mono.just(users);
        }
        List<User> filteredRows = users.stream().filter(row -> row.getName() != null && row.getName().createFullName().equalsIgnoreCase(name.createFullName())).collect(Collectors.toList());
        return Mono.just(filteredRows);
    }
}
