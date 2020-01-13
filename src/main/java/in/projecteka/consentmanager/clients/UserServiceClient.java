package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.properties.UserServiceProperties;
import in.projecteka.consentmanager.link.discovery.model.User;
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
                .uri(String.format("%s/users/%s/", userServiceProperties.getUrl(), userId))
                .retrieve()
                .onStatus(httpStatus -> httpStatus.value() == 404, clientResponse -> Mono.error(new Throwable("User not found")))
                .bodyToMono(User.class);
    }
}
