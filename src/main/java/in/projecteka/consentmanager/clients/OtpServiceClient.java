package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.OtpRequest;
import in.projecteka.consentmanager.clients.model.VerificationRequest;
import in.projecteka.consentmanager.consent.model.Notification;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.clients.ClientError.unknownErrorOccurred;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class OtpServiceClient {

    private final WebClient.Builder webClientBuilder;

    public OtpServiceClient(WebClient.Builder webClientBuilder, String baseUrl) {
        this.webClientBuilder = webClientBuilder;
        this.webClientBuilder.baseUrl(baseUrl);
    }

    public Mono<Void> send(OtpRequest requestBody) {
        return webClientBuilder.build()
                .post()
                .uri(uriBuilder -> uriBuilder.path("/otp").build())
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestBody), OtpRequest.class)
                .accept(APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .toBodilessEntity()
                .then();
    }

    public Mono<Void> verify(String sessionId, String otp) {
        return webClientBuilder.build()
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/otp/{sessionId}/verify")
                        .build(sessionId))
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON)
                .body(Mono.just(new VerificationRequest(otp)), VerificationRequest.class)
                .retrieve()
                .onStatus(httpStatus -> httpStatus.value() == 400,
                        clientResponse -> Mono.error(ClientError.invalidOtp()))
                .onStatus(httpStatus -> httpStatus.value() == 401,
                        clientResponse -> Mono.error(ClientError.otpExpired()))
                .onStatus(HttpStatus::is5xxServerError,
                        clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .toBodilessEntity()
                .then();
    }

    public Mono<Void> send(Notification notification) {
        return webClientBuilder.build()
                .post()
                .uri(uriBuilder -> uriBuilder.path("/notification").build())
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .bodyValue(notification)
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(unknownErrorOccurred()))
                .toBodilessEntity()
                .then();
    }
}
