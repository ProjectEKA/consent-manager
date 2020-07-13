package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.HASOtpRequestResponse;
import in.projecteka.consentmanager.clients.model.OtpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class HASServiceClient {
    private final WebClient webClient;

    public HASServiceClient(WebClient.Builder webClient, String baseUrl) {
        this.webClient = webClient.baseUrl(baseUrl).build();
    }

    public Mono<HASOtpRequestResponse> send(OtpRequest otpRequest) {
        return webClient
                .post()
                .uri(uriBuilder -> uriBuilder.path(getPath(otpRequest)).build())
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .body(Mono.just(getRequestBody(otpRequest)), Map.class)
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .bodyToMono(HASOtpRequestResponse.class);
    }

    private String getPath(OtpRequest otpRequest) {
        if (otpRequest.getCommunication().getMode().equals("MOBILE")) {
            return "/v1/ha/generate_mobile_otp";
        }
        return null;
    }

    private HashMap<String, String> getRequestBody(OtpRequest requestBody) {
        if (requestBody.getCommunication().getMode().equals("MOBILE")){
            HashMap<String, String> mobileRequestBody = new HashMap<>();
            mobileRequestBody.put("mobile", requestBody.getCommunication().getValue());
            return mobileRequestBody;
        }
        return new HashMap<>();
    }
}
