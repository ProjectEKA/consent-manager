package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.HasOtpVerificationRequest;
import in.projecteka.consentmanager.clients.model.User;
import in.projecteka.consentmanager.clients.properties.GatewayServiceProperties;
import in.projecteka.consentmanager.common.ServiceAuthentication;
import in.projecteka.consentmanager.consent.NHSProperties;
import in.projecteka.consentmanager.user.model.HasToken;
import in.projecteka.consentmanager.user.model.PatientResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Supplier;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.error;

@AllArgsConstructor
public class UserServiceClient {

    private static final String PATIENT_FIND_URL_PATH = "/patients/on-find";
    private static final String INTERNAL_PATH_USER_IDENTIFICATION = "%s/internal/users/%s/";
    private static final String HAS_VERIFY_OTP_URL_PATH = "%s/v1/ha/verify_mobile_otp";
    private final WebClient webClient;
    private final String url;
    private final Supplier<Mono<String>> tokenGenerator;
    private final GatewayServiceProperties gatewayServiceProperties;
    private final ServiceAuthentication serviceAuthentication;
    private final String authorizationHeader;
    private final NHSProperties nhsProperties;

    public Mono<User> userOf(String userId) {
        return tokenGenerator.get()
                .flatMap(token ->
                        webClient
                                .get()
                                .uri(String.format(INTERNAL_PATH_USER_IDENTIFICATION, url, userId))
                                .header(authorizationHeader, token)
                                .retrieve()
                                .onStatus(httpStatus -> httpStatus.value() == 404,
                                        clientResponse -> error(ClientError.userNotFound()))
                                .bodyToMono(User.class));
    }

    public Mono<Void> sendPatientResponseToGateWay(PatientResponse patientResponse,
                                                   String routingKey,
                                                   String requesterId) {
        return serviceAuthentication.authenticate()
                .flatMap(authToken ->
                        webClient
                                .post()
                                .uri(gatewayServiceProperties.getBaseUrl() + PATIENT_FIND_URL_PATH)
                                .contentType(APPLICATION_JSON)
                                .header(AUTHORIZATION, authToken)
                                .header(routingKey, requesterId)
                                .bodyValue(patientResponse)
                                .retrieve()
                                .onStatus(httpStatus -> httpStatus.value() == 401,
                                        clientResponse -> error(ClientError.unAuthorized()))
                                .onStatus(HttpStatus::is5xxServerError,
                                        clientResponse -> error(ClientError.networkServiceCallFailed()))
                                .toBodilessEntity()
                                .timeout(Duration.ofMillis(gatewayServiceProperties.getRequestTimeout())))
                .then();
    }

    public Mono<HasToken> verifyOtpWithHasFor(HasOtpVerificationRequest request) {
        return webClient
                .post()
                .uri(String.format(HAS_VERIFY_OTP_URL_PATH, nhsProperties.getHasBaseUrl()))
                .contentType(APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(httpStatus -> httpStatus.value() == 404,
                    clientResponse -> error(ClientError.invalidOtp()))
                .bodyToMono(HasToken.class);
    }
}
