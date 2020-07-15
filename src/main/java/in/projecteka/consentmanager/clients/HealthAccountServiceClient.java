package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.OtpRequestResponse;
import in.projecteka.consentmanager.clients.model.OtpRequest;
import in.projecteka.consentmanager.clients.model.HealthAccountServiceTokenResponse;
import in.projecteka.consentmanager.user.UserService;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class HealthAccountServiceClient {
    private final WebClient webClient;
    private final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final String OTP_REQUEST_FOR_MOBILE_PATH = "/v1/ha/generate_mobile_otp";
    private final String OTP_VERIFY_FOR_MOBILE_PATH = "/v1/ha/verify_mobile_otp";

    public HealthAccountServiceClient(WebClient.Builder webClient, String baseUrl) {
        ReactorClientHttpConnector connector = sslIgnoreConnector();
        this.webClient = webClient.clientConnector(connector).baseUrl(baseUrl).build();
    }

    public Mono<OtpRequestResponse> send(OtpRequest otpRequest) {
        HashMap<String, String> requestBody = new HashMap<>();
        requestBody.put("mobile", otpRequest.getCommunication().getValue());

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
                .onStatus(HttpStatus::isError, clientResponse -> Mono.error(ClientError.networkServiceCallFailed()))
                .bodyToMono(HealthAccountServiceTokenResponse.class);
    }

    private ReactorClientHttpConnector sslIgnoreConnector() {
        HttpClient httpClient = null;
        try {
            SslContext sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
            httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext) );
        } catch (SSLException e) {
            e.printStackTrace();
        }

        return new ReactorClientHttpConnector(httpClient);
    }
}