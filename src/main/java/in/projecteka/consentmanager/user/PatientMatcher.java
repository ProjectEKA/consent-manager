package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.user.model.RecoverCmIdRequest;
import in.projecteka.consentmanager.user.model.User;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PatientMatcher {

    private final HashMap<String, FilterStrategy> attributeFilterStrategyMap = new HashMap<>() {
        {
            put("name",new NameFilter());
            put("yearOfBirth",new YOBFilter());
            put("ABPMJAYId",new ABPMJAYIdFilter());
        }
    };

    public Mono<List<User>> match(List<User> users, RecoverCmIdRequest request) {
        List<String> attributesToFilter = new ArrayList<>(List.of("name","yearOfBirth","ABPMJAYID"));
        attributesToFilter.stream();
        return Mono.just(users);
    }
}
