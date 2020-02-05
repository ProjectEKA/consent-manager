package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.user.model.DeviceIdentifier;
import in.projecteka.consentmanager.user.model.OtpVerification;
import in.projecteka.consentmanager.user.model.TemporarySession;
import in.projecteka.consentmanager.user.model.Token;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;


@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureWebTestClient
class UserControllerTest {

    @MockBean
    private UserService mockService;

    @Autowired
    private WebTestClient webClient;

    @Test
    public void shouldReturnTemporarySessionIfOtpRequestIsSuccessful() {
        DeviceIdentifier deviceIdentifier = new DeviceIdentifier("MOBILE", "1234567891");
        when(mockService.sendOtp(any())).thenReturn(Mono.just(new TemporarySession("SOME-SESSION")));

        webClient.post()
                .uri("/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(deviceIdentifier))
                .exchange().expectStatus().isCreated();

        Mockito.verify(mockService, times(1)).sendOtp(deviceIdentifier);
    }

    @Test
    public void shouldReturnTemporarySessionIfOtpPermitRequestIsSuccessful() {
        OtpVerification otpVerification = new OtpVerification("SOME-SESSION", "1234");
        String temporaryToken = UUID.randomUUID().toString();
        Token token = new Token(temporaryToken);

        when(mockService.permitOtp(any())).thenReturn(Mono.just(token));

        webClient.post()
                .uri("/users/permit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(otpVerification))
                .exchange().expectStatus().isOk();

        Mockito.verify(mockService, times(1)).permitOtp(otpVerification);
    }

}
