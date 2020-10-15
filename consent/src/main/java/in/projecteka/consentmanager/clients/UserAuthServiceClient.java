package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.properties.GatewayServiceProperties;
import in.projecteka.consentmanager.userauth.Constants;
import in.projecteka.consentmanager.userauth.model.FetchAuthModesResponse;
import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.common.ServiceAuthentication;
import lombok.AllArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static in.projecteka.library.common.Constants.CORRELATION_ID;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@AllArgsConstructor
public class UserAuthServiceClient {
    private final WebClient webClientBuilder;
    private final ServiceAuthentication serviceAuthentication;
    private final GatewayServiceProperties gatewayServiceProperties;

    public Mono<Void> sendAuthModesResponseToGateway(FetchAuthModesResponse response, String routingKey, String requesterId) {
        return serviceAuthentication.authenticate()
                .flatMap(token ->
                        webClientBuilder
                                .post()
                                .uri(fetchAuthModesResponseUrl())
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(AUTHORIZATION, token)
                                .header(routingKey, requesterId)
                                .header(CORRELATION_ID, MDC.get(CORRELATION_ID))
                                .bodyValue(response)
                                .retrieve()
                                .onStatus(httpStatus -> httpStatus.value() == HttpStatus.BAD_REQUEST.value(),
                                        clientResponse -> Mono.error(ClientError.unprocessableEntity()))
                                .onStatus(httpStatus -> httpStatus.value() == HttpStatus.UNAUTHORIZED.value(),
                                        clientResponse -> Mono.error(ClientError.unAuthorized()))
                                .onStatus(HttpStatus::is5xxServerError,
                                        clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                                .toBodilessEntity()
                                .timeout(Duration.ofMillis(gatewayServiceProperties.getRequestTimeout())))
                .then();
    }

    private String fetchAuthModesResponseUrl() {
        return gatewayServiceProperties.getBaseUrlWithoutEndSlash().concat(Constants.PATH_ON_USER_FETCH_AUTH_MODES);
    }
}
