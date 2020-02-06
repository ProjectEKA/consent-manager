package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.user.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;


public class OtpServiceClient {

    private final WebClient.Builder webClientBuilder;
    private final OtpServiceProperties otpServiceProperties;

    public OtpServiceClient(WebClient.Builder webClientBuilder,
                            OtpServiceProperties otpServiceProperties) {
        this.webClientBuilder = webClientBuilder;
        this.webClientBuilder.baseUrl(otpServiceProperties.getUrl());
        this.otpServiceProperties = otpServiceProperties;
    }

    public Mono<TemporarySession> sendOtpTo(OtpRequest requestBody) {
        TemporarySession temporarySession = new TemporarySession(requestBody.getSessionId());
        return webClientBuilder.build()
                .post()
                .uri(uriBuilder ->
                        uriBuilder.path("/otp").build())
                .header("Content-Type", "application/json")
                .body(Mono.just(requestBody), OtpRequest.class)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .toBodilessEntity().thenReturn(temporarySession);
    }

    public Mono<Token> permitOtp(OtpVerification requestBody) {
        Token token = new Token(UUID.randomUUID().toString());
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
                .toBodilessEntity().thenReturn(new Token(UUID.randomUUID().toString()));

    }

}
