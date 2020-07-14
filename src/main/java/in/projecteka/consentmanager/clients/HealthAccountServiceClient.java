package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.OtpRequestResponse;
import in.projecteka.consentmanager.clients.model.OtpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class HealthAccountServiceClient {
    private final WebClient webClient;

    public HealthAccountServiceClient(WebClient.Builder webClient, String baseUrl) {
        this.webClient = webClient.baseUrl(baseUrl).build();
    }

    public Mono<OtpRequestResponse> send(OtpRequest otpRequest) {
        return webClient
                .post()
                .uri(uriBuilder -> uriBuilder.path(getPath(otpRequest)).build())
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .body(Mono.just(getRequestBody(otpRequest)), Map.class)
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .bodyToMono(OtpRequestResponse.class);
    }

    private String getPath(OtpRequest otpRequest) {
        if (isModeMobile(otpRequest)) {
            return "/v1/ha/generate_mobile_otp";
        }
        return null;
    }

    private HashMap<String, String> getRequestBody(OtpRequest requestBody) {
        if (isModeMobile(requestBody)){
            HashMap<String, String> mobileRequestBody = new HashMap<>();
            mobileRequestBody.put("mobile", requestBody.getCommunication().getValue());
            return mobileRequestBody;
        }
        return new HashMap<>();
    }

    private boolean isModeMobile(OtpRequest otpRequest) {
        return otpRequest.getCommunication().getMode().equals("MOBILE");
    }
}
