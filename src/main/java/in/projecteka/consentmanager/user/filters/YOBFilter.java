package in.projecteka.consentmanager.user.filters;

import in.projecteka.consentmanager.user.model.DateOfBirth;
import in.projecteka.consentmanager.user.model.User;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

public class YOBFilter implements FilterStrategy<DateOfBirth> {
    @Override
    public Mono<List<User>> filter(List<User> users, DateOfBirth DOB) {
        if (DOB == null) {
            return Mono.just(users);
        }
        List<User> filteredRows = users.stream().filter(row ->
                row.getDateOfBirth().getYear() != null && row.getDateOfBirth().getYear().equals(DOB.getYear()))
                .collect(Collectors.toList());
        return Mono.just(filteredRows);
    }
}
