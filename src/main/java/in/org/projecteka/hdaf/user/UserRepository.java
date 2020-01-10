package in.org.projecteka.hdaf.user;

import in.org.projecteka.hdaf.clients.properties.UserServiceProperties;
import in.org.projecteka.hdaf.user.model.User;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class UserRepository {

    WebClient builder;

    public UserRepository(WebClient.Builder builder, UserServiceProperties properties) {
        this.builder = builder.baseUrl(properties.getUrl()).build();
    }

    public Mono<User> userWith(String userName) {
        return builder.get().uri("/users.json")
                .retrieve()
                .bodyToFlux(User.class)
                .filter(user -> user.getIdentifier().equals(userName))
                .single()
                .switchIfEmpty(Mono.error(new Exception("Something went wrong")));
    }
}
