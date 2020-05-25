package in.projecteka.consentmanager.user.filters;

import in.projecteka.consentmanager.user.model.User;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

public class YOBFilter implements FilterStrategy<Integer> {
    @Override
    public Mono<List<User>> filter(List<User> users, Integer YOB) {
        if (YOB == null) {
            return Mono.just(users);
        }
        List<User> filteredRows = users.stream().filter(row -> row.getYearOfBirth() != null && row.getYearOfBirth().equals(YOB)).collect(Collectors.toList());
        return Mono.just(filteredRows);
    }
}
