package in.org.projecteka.hdaf.user;

import in.org.projecteka.hdaf.user.model.User;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
public class UserController {
    private UserService userService;

    @GetMapping("/users/{userId}")
    public Mono<User> user(@PathVariable String userId) {
        return userService.getUser(userId);
    }
}