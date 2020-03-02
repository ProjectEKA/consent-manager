package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.consent.ConsentArtefactBroadcastListener;
import in.projecteka.consentmanager.consent.ConsentManager;
import in.projecteka.consentmanager.consent.ConsentRequestNotificationListener;
import in.projecteka.consentmanager.dataflow.DataFlowBroadcastListener;
import in.projecteka.consentmanager.dataflow.DataFlowRequester;
import in.projecteka.consentmanager.user.model.OtpVerification;
import in.projecteka.consentmanager.user.model.SignUpSession;
import in.projecteka.consentmanager.user.model.Token;
import in.projecteka.consentmanager.user.model.UserSignUpEnquiry;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
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
    private ConsentArtefactBroadcastListener consentArtefactBroadcastListener;

    @MockBean
    private DataFlowBroadcastListener dataFlowBroadcastListener;

    @MockBean
    private ConsentRequestNotificationListener consentRequestNotificationListener;

    @Autowired
    private WebTestClient webClient;

    EasyRandom easyRandom;

    @BeforeEach
    void setUp() {
        easyRandom = new EasyRandom();
    }

    @Test
    public void shouldReturnTemporarySessionIfOtpRequestIsSuccessful() {
        UserSignUpEnquiry userSignupEnquiry = new UserSignUpEnquiry(
                "MOBILE",
                easyRandom.nextObject(String.class));
        when(mockService.sendOtp(any())).thenReturn(Mono.just(new SignUpSession(easyRandom.nextObject(String.class))));

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
                easyRandom.nextObject(String.class),
                easyRandom.nextObject(String.class));
        Token token = new Token(easyRandom.nextObject(String.class));

        when(mockService.permitOtp(any())).thenReturn(Mono.just(token));

        webClient.post()
                .uri("/users/permit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(otpVerification))
                .exchange().expectStatus().isOk();

        Mockito.verify(mockService, times(1)).permitOtp(otpVerification);
    }
}
