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
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.Arrays.asList;
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
        if(isMockedMobileNumber(otpRequest.getCommunication().getValue())){
            logger.info("Request for mock mobile {}", otpRequest.getCommunication().getValue());
            return Mono.just(new OtpRequestResponse(UUID.randomUUID().toString()));
        }

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
        if (isMockedOTP(otpValue)){
            logger.info("Request for mock otp {}", otpValue);
            return mockOtpVerificationResponse(otpValue);
        }

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

    private Mono<HealthAccountServiceTokenResponse> mockOtpVerificationResponse(String otpValue) {
        if ("666666".equals(otpValue)){
            return Mono.just(new HealthAccountServiceTokenResponse(UUID.randomUUID().toString()));
        }
        if ("444444".equals(otpValue)){
            return Mono.error(ClientError.otpExpired());
        }
        if ("222222".equals(otpValue)){
            return Mono.error(ClientError.invalidOtp());
        }
        if ("000000".equals(otpValue)){
            return Mono.error(ClientError.networkServiceCallFailed());
        }
        return null;
    }

    private boolean isMockedOTP(String otpValue) {
        return asList("666666", "444444", "222222", "000000").contains(otpValue);
    }

    private boolean isMockedMobileNumber(String value) {
        return asList("+91-8888888888").contains(value);
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