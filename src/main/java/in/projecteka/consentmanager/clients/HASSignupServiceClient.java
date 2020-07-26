package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.user.model.GenerateAadharOtpRequest;
import in.projecteka.consentmanager.user.model.GenerateAadharOtpResponse;
import in.projecteka.consentmanager.user.model.HASSignupRequest;
import in.projecteka.consentmanager.user.model.HealthAccountUser;
import in.projecteka.consentmanager.user.model.UpdateHASAddressRequest;
import in.projecteka.consentmanager.user.model.UpdateHASUserRequest;
import in.projecteka.consentmanager.user.model.VerifyAadharOtpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static in.projecteka.consentmanager.user.Constants.HAS_ACCOUNT_UPDATE;
import static in.projecteka.consentmanager.user.Constants.HAS_CREATE_ACCOUNT_VERIFIED_MOBILE_TOKEN;
import static java.lang.String.format;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class HASSignupServiceClient {

    private final WebClient webClient;
    private static final String X_TOKEN_HEADER_NAME = "X-Token";
    private final String OTP_REQUEST_FOR_AADHAR = "/v1/ha/generate_aadhar_otp";
    private final String VERIFY_OTP_FOR_AADHAR  = "/v1/ha/verify_aadhar_otp";

    public HASSignupServiceClient(WebClient.Builder webClient, String baseUrl) {
        this.webClient = webClient.baseUrl(baseUrl).build();
    }

    public Mono<HealthAccountUser> createHASAccount(HASSignupRequest request) {
        return webClient
                .post()
                .uri(uriBuilder -> uriBuilder.path(HAS_CREATE_ACCOUNT_VERIFIED_MOBILE_TOKEN).build())
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .body(Mono.just(request), HASSignupRequest.class)
                .accept(APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, clientResponse -> Mono.error(ClientError.unAuthorized()))
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .bodyToMono(HealthAccountUser.class);
    }

    public Mono<Void> updateHASAccount(UpdateHASUserRequest request, String token) {
        String xTokenValue = format("Bearer %s", token);
        return webClient
                .post()
                .uri(uriBuilder -> uriBuilder.path(HAS_ACCOUNT_UPDATE).build())
                .header(X_TOKEN_HEADER_NAME, xTokenValue)
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .body(Mono.just(request), UpdateHASUserRequest.class)
                .accept(APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .toBodilessEntity()
                .then();
    }

    public Mono<GenerateAadharOtpResponse> generateAadharOtp(GenerateAadharOtpRequest request) {
        HashMap<String, String> requestBody = new HashMap<>();
        requestBody.put("aadhaar", request.getAadhaar());
        return webClient
                .post()
                .uri(uriBuilder -> uriBuilder.path(OTP_REQUEST_FOR_AADHAR).build())
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestBody), Map.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, clientResponse -> Mono.error(ClientError.invalidRequester("Invalid Request")))
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .bodyToMono(GenerateAadharOtpResponse.class);
    }

    public Mono<HealthAccountUser> verifyAadharOtp(VerifyAadharOtpRequest request) {
        return webClient
                .post()
                .uri(uriBuilder -> uriBuilder.path(VERIFY_OTP_FOR_AADHAR).build())
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .body(Mono.just(request), VerifyAadharOtpRequest.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, clientResponse -> Mono.error(ClientError.invalidRequester("Invalid Request")))
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .bodyToMono(HealthAccountUser.class);
    }


    public Mono<HealthAccountUser> updateHASAddress(UpdateHASAddressRequest request, String token) {
        return webClient
                .post()
                .uri(uriBuilder -> uriBuilder.path(HAS_ACCOUNT_UPDATE).build())
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .header("X-Token", token)
                .body(Mono.just(request), UpdateHASAddressRequest.class)
                .accept(APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .bodyToMono(HealthAccountUser.class);
    }
}