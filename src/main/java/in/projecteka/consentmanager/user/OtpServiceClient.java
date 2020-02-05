package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.user.model.OtpRequest;
import in.projecteka.consentmanager.user.model.OtpVerification;
import in.projecteka.consentmanager.user.model.TemporarySession;
import in.projecteka.consentmanager.user.model.Token;
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
        this.otpServiceProperties = otpServiceProperties;
    }

    public Mono<TemporarySession> sendOtpTo(OtpRequest requestBody) {
        TemporarySession temporarySession = new TemporarySession(requestBody.getSessionId());
        return webClientBuilder.build()
                .post()
                .uri(String.format("%s/link", otpServiceProperties.getUrl()))
                .header("Content-Type", "application/json")
                .body(Mono.just(requestBody), OtpRequest.class)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .toBodilessEntity().thenReturn(temporarySession);
    }

    public Mono<Token> permitOtp(OtpVerification requestBody) {
        Token temporaryToken = new Token(UUID.randomUUID().toString());
        return webClientBuilder.build()
                .post()
                .uri(String.format("%s/verify", otpServiceProperties.getUrl()))
                .header("Content-Type", "application/json")
                .body(Mono.just(requestBody), OtpVerification.class)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, clientResponse -> Mono.error(ClientError.otpNotFound()))
                .toBodilessEntity().thenReturn(temporaryToken);

    }

}
