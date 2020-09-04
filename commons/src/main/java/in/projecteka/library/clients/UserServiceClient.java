package in.projecteka.library.clients;

import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.clients.model.User;
import lombok.AllArgsConstructor;
import org.slf4j.MDC;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

import static in.projecteka.library.common.Constants.CORRELATION_ID;
import static reactor.core.publisher.Mono.error;

@AllArgsConstructor
public class UserServiceClient {

    private static final String INTERNAL_PATH_USER_IDENTIFICATION = "%s/internal/users/%s/";
    private final WebClient webClient;
    private final String url;
    private final Supplier<Mono<String>> tokenGenerator;
    private final String authorizationHeader;

    public Mono<User> userOf(String userId) {
        return tokenGenerator.get()
                .flatMap(token ->
                        webClient
                                .get()
                                .uri(String.format(INTERNAL_PATH_USER_IDENTIFICATION, url, userId))
                                .header(authorizationHeader, token)
                                .header(CORRELATION_ID, MDC.get(CORRELATION_ID))
                                .retrieve()
                                .onStatus(httpStatus -> httpStatus.value() == 404,
                                        clientResponse -> error(ClientError.userNotFound()))
                                .bodyToMono(User.class));
    }
}
