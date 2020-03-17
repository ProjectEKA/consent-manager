package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.User;
import lombok.AllArgsConstructor;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@AllArgsConstructor
public class UserServiceClient {

    private final WebClient.Builder webClientBuilder;
    private final String url;
    private final Supplier<Mono<String>> tokenGenerator;

    public Mono<User> userOf(String userId) {
        return tokenGenerator.get().flatMap(token ->
                webClientBuilder.build()
                        .get()
                        .uri(String.format("%s/users/%s/", url, userId))
                        .header(AUTHORIZATION, token)
                        .retrieve()
                        .onStatus(httpStatus -> httpStatus.value() == 404,
                                clientResponse -> Mono.error(ClientError.userNotFound()))
                        .bodyToMono(User.class));
    }
}
