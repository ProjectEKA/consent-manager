package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.OtpRequest;
import in.projecteka.consentmanager.clients.model.Value;
import in.projecteka.consentmanager.clients.properties.OtpServiceProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class OtpServiceClient {

    private final WebClient.Builder webClientBuilder;

    public OtpServiceClient(WebClient.Builder webClientBuilder,
                            OtpServiceProperties otpServiceProperties) {
        this.webClientBuilder = webClientBuilder;
        this.webClientBuilder.baseUrl(otpServiceProperties.getUrl());
    }

    public Mono<Void> send(OtpRequest requestBody) {
        return webClientBuilder.build()
                .post()
                .uri(uriBuilder -> uriBuilder.path("/otp").build())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(requestBody), OtpRequest.class)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .toBodilessEntity()
                .then();
    }

    public Mono<Void> verify(String sessionId, String otp) {
        Value valueOtp = new Value(otp);
        return webClientBuilder.build()
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/otp/{sessionId}/verify")
                        .build(sessionId))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(valueOtp), Value.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, clientResponse -> Mono.error(ClientError.otpNotFound()))
                .onStatus(HttpStatus::is5xxServerError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .toBodilessEntity()
                .then();
    }
}
