package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.User;
import in.projecteka.consentmanager.clients.properties.UserServiceProperties;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;


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
        return Mono.subscriberContext()
                .map(context -> context.get(AUTHORIZATION).toString())
                .flatMap(token -> webClientBuilder.build()
                        .get()
                        .uri(String.format("%s/users/%s/", userServiceProperties.getUrl(), userId))
                        .header(AUTHORIZATION, token)
                        .retrieve()
                        .onStatus(httpStatus -> httpStatus.value() == 404,
                                clientResponse -> Mono.error(ClientError.userNotFound()))
                        .bodyToMono(User.class));
    }
}
