package in.org.projecteka.hdaf.user;

import in.org.projecteka.hdaf.user.model.User;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class UserService {
    private UserRepository userRepository;

    public Mono<User> userWith(String userName) {
        return userRepository.userWith(userName);
    }
}
