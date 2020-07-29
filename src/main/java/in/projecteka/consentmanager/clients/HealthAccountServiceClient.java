package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.HealthAccountServiceTokenResponse;
import in.projecteka.consentmanager.clients.model.OtpRequest;
import in.projecteka.consentmanager.clients.model.OtpRequestResponse;
import in.projecteka.consentmanager.clients.model.StateRequestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static in.projecteka.consentmanager.user.Constants.HAS_OTP_REQUEST_FOR_MOBILE_PATH;
import static in.projecteka.consentmanager.user.Constants.HAS_OTP_VERIFY_FOR_MOBILE_PATH;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class HealthAccountServiceClient {
    private final Logger logger = LoggerFactory.getLogger(HealthAccountServiceClient.class);
    private final WebClient webClient;

    public HealthAccountServiceClient(WebClient.Builder webClient, String baseUrl) {
        this.webClient = webClient.baseUrl(baseUrl).build();
    }

    public Mono<OtpRequestResponse> send(OtpRequest otpRequest) {
        HashMap<String, String> requestBody = new HashMap<>();
        requestBody.put("mobile", removeCountryCodeIfPresent(otpRequest));

        return webClient
                .post()
                .uri(uriBuilder -> uriBuilder.path(HAS_OTP_REQUEST_FOR_MOBILE_PATH).build())
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestBody), Map.class)
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .bodyToMono(OtpRequestResponse.class);
    }

    public Mono<HealthAccountServiceTokenResponse> verifyOtp(String sessionId, String otpValue) {
        HashMap<String, String> requestBody = new HashMap<>();
        requestBody.put("txnId", sessionId);
        requestBody.put("otp", otpValue);

        return webClient
                .post()
                .uri(uriBuilder -> uriBuilder.path(HAS_OTP_VERIFY_FOR_MOBILE_PATH).build())
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestBody), Map.class)
                .retrieve()
                .onStatus(httpStatus -> httpStatus.value() == 401, clientResponse -> Mono.error(ClientError.invalidOtp()))
                .onStatus(HttpStatus::isError, clientResponse -> clientResponse.bodyToMono(Properties.class)
                        .doOnNext(properties -> logger.error(properties.toString()))
                        .thenReturn(ClientError.unAuthorized()))
                .bodyToMono(HealthAccountServiceTokenResponse.class);
    }

    public Mono<List<StateRequestResponse>> getState() {
        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder.path("/v1/ha/lgd/states").build())
                .retrieve()
                .onStatus(httpStatus -> httpStatus.value() == 403,
                        clientResponse -> Mono.error(ClientError.unAuthorized()))
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .bodyToMono(StateRequestResponse[].class)
                .map(Arrays::asList);
    }

    private String removeCountryCodeIfPresent(OtpRequest otpRequest) {
        String countryCode = "+91-";
        String mobileNumber = otpRequest.getCommunication().getValue();
        if (StringUtils.startsWithIgnoreCase(mobileNumber, countryCode)) {
            return StringUtils.replace(mobileNumber, countryCode, "");
        }
        return mobileNumber;
    }
}