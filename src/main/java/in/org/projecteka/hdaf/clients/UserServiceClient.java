package in.org.projecteka.hdaf.clients;

import in.org.projecteka.hdaf.clients.properties.UserServiceProperties;
import in.org.projecteka.hdaf.link.discovery.model.User;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class UserServiceClient {

    private final WebClient.Builder webClientBuilder;
    private final UserServiceProperties userServiceProperties;

    public UserServiceClient(
            WebClient.Builder webClientBuilder,
            UserServiceProperties userServiceProperties) {
        this.webClientBuilder = webClientBuilder;
        this.userServiceProperties = userServiceProperties;
    }

    public Mono<User> userOf(String userId) {
        return webClientBuilder.build()
                .get()
                .uri(String.format("%s/users/%s", userServiceProperties.getUrl(), userId))
                .header("X-Auth-Token", userServiceProperties.getXAuthToken())
                .retrieve()
                .onStatus(httpStatus -> httpStatus.value() == 404, clientResponse -> Mono.error(new Throwable("User not found")))
                .bodyToMono(User.class);
    }
}
