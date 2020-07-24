package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.HealthAccountServiceTokenResponse;
import in.projecteka.consentmanager.clients.model.OtpRequest;
import in.projecteka.consentmanager.clients.model.OtpRequestResponse;
import in.projecteka.consentmanager.user.model.GenerateAadharOtpRequest;
import in.projecteka.consentmanager.user.model.GenerateAadharOtpResponse;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class HealthAccountServiceClient {
    private final WebClient webClient;

    private final String OTP_REQUEST_FOR_MOBILE_PATH = "/v1/ha/generate_mobile_otp";
    private final String OTP_VERIFY_FOR_MOBILE_PATH = "/v1/ha/verify_mobile_otp";

    public HealthAccountServiceClient(WebClient.Builder webClient, String baseUrl) {
        this.webClient = webClient.baseUrl(baseUrl).build();
    }

    public Mono<OtpRequestResponse> send(OtpRequest otpRequest) {
        HashMap<String, String> requestBody = new HashMap<>();
        requestBody.put("mobile", removeCountryCodeIfPresent(otpRequest));

        return webClient
                .post()
                .uri(uriBuilder -> uriBuilder.path(OTP_REQUEST_FOR_MOBILE_PATH).build())
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
                .uri(uriBuilder -> uriBuilder.path(OTP_VERIFY_FOR_MOBILE_PATH).build())
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .body(Mono.just(requestBody), Map.class)
                .retrieve()
                .onStatus(httpStatus -> httpStatus.value() == 401, clientResponse -> Mono.error(ClientError.invalidOtp()))
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .bodyToMono(HealthAccountServiceTokenResponse.class);
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