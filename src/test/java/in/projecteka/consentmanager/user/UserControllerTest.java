package in.projecteka.consentmanager.user;

import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.consent.ConsentManager;
import in.projecteka.consentmanager.consent.ConsentRequestNotificationListener;
import in.projecteka.consentmanager.consent.HipConsentNotificationListener;
import in.projecteka.consentmanager.consent.HiuConsentNotificationListener;
import in.projecteka.consentmanager.dataflow.DataFlowBroadcastListener;
import in.projecteka.consentmanager.dataflow.DataFlowRequester;
import in.projecteka.consentmanager.user.model.OtpVerification;
import in.projecteka.consentmanager.user.model.SignUpSession;
import in.projecteka.consentmanager.user.model.Token;
import in.projecteka.consentmanager.user.model.UserSignUpEnquiry;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.user.TestBuilders.string;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@SuppressWarnings("ALL")
@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureWebTestClient
class UserControllerTest {

    @MockBean
    private UserService mockService;

    @MockBean
    private ConsentManager consentManager;

    @MockBean
    private DataFlowRequester dataFlowRequester;

    @MockBean
    private DestinationsConfig destinationsConfig;

    @MockBean
    private HiuConsentNotificationListener hiuConsentNotificationListener;

    @MockBean
    private HipConsentNotificationListener hipConsentNotificationListener;

    @MockBean
    private DataFlowBroadcastListener dataFlowBroadcastListener;

    @SuppressWarnings("unused")
    @MockBean
    private ConsentRequestNotificationListener consentRequestNotificationListener;

    @MockBean
    private SignUpService signupService;

    @Autowired
    private WebTestClient webClient;

    @SuppressWarnings("unused")
    @MockBean(name = "centralRegistryJWKSet")
    private JWKSet centralRegistryJWKSet;

    @SuppressWarnings("unused")
    @MockBean(name = "identityServiceJWKSet")
    private JWKSet identityServiceJWKSet;

    @Test
    public void shouldReturnTemporarySessionIfOtpRequestIsSuccessful() {
        UserSignUpEnquiry userSignupEnquiry = new UserSignUpEnquiry(
                "MOBILE",
                string());
        when(mockService.sendOtp(any())).thenReturn(Mono.just(new SignUpSession(string())));

        webClient.post()
                .uri("/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(userSignupEnquiry))
                .exchange().expectStatus().isCreated();

        Mockito.verify(mockService, times(1)).sendOtp(userSignupEnquiry);
    }

    @Test
    public void shouldReturnTemporarySessionIfOtpPermitRequestIsSuccessful() {
        OtpVerification otpVerification = new OtpVerification(
                string(),
                string());
        Token token = new Token(string());

        when(mockService.permitOtp(any())).thenReturn(Mono.just(token));

        webClient.post()
                .uri("/users/permit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(otpVerification))
                .exchange().expectStatus().isOk();

        Mockito.verify(mockService, times(1)).permitOtp(otpVerification);
    }
}
