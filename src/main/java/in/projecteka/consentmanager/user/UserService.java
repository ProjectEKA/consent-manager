package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.user.model.User;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class UserService {
    private UserRepository userRepository;

    public Mono<User> userWith(String userName) {
        return userRepository.userWith(userName);
    }
}
