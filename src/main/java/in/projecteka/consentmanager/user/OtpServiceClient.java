package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.user.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


public class OtpServiceClient {

    private final WebClient.Builder webClientBuilder;
    private UserVerificationService userVerificationService;

    public OtpServiceClient(WebClient.Builder webClientBuilder,
                            OtpServiceProperties otpServiceProperties,
                            UserVerificationService userVerificationService) {
        this.webClientBuilder = webClientBuilder;
        this.webClientBuilder.baseUrl(otpServiceProperties.getUrl());
        this.userVerificationService = userVerificationService;
    }

    public Mono<TemporarySession> send(OtpRequest requestBody) {
        return webClientBuilder.build()
                .post()
                .uri(uriBuilder ->
                        uriBuilder.path("/otp").build())
                .header("Content-Type", "application/json")
                .body(Mono.just(requestBody), OtpRequest.class)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .toBodilessEntity()
                .thenReturn(userVerificationService.cacheAndSendSession(
                        requestBody.getSessionId(),
                        requestBody.getCommunication().getValue())
                );
    }

    public Mono<Token> verify(OtpVerification requestBody) {
        Value valueOtp = new Value(requestBody.getValue());
        return webClientBuilder.build()
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/otp/{sessionId}/verify")
                        .build(requestBody.getSessionId()))
                .header("Content-Type", "application/json")
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(valueOtp),Value.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, clientResponse -> Mono.error(ClientError.otpNotFound()))
                .onStatus(HttpStatus::is5xxServerError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .toBodilessEntity().thenReturn(userVerificationService.generateToken(requestBody.getSessionId()));
    }

}
