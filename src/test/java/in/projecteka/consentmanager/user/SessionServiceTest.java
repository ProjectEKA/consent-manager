package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.AuthorizationTest;
import in.projecteka.consentmanager.clients.ClientError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static in.projecteka.consentmanager.user.TestBuilders.session;
import static in.projecteka.consentmanager.user.TestBuilders.sessionRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

class SessionServiceTest {

    @Mock
    TokenService tokenService;

    @BeforeEach
    void init() {
        initMocks(this);
    }

    @Test
    void returnSession() {
        var sessionRequest = sessionRequest().build();
        var expectedSession = session().build();
        when(tokenService.tokenForUser(sessionRequest.getUserName(), sessionRequest.getPassword()))
                .thenReturn(Mono.just(expectedSession));
        var sessionService = new SessionService(tokenService);

        var sessionPublisher = sessionService.forNew(sessionRequest);

        StepVerifier.create(sessionPublisher)
                .assertNext(session -> assertThat(session).isEqualTo(expectedSession))
                .verifyComplete();
    }

    @ParameterizedTest
    @CsvSource({
            ",",
            "empty",
            "null"
    })
    void returnUnAuthorizedErrorWhenUsernameIsEmpty(
            @ConvertWith(AuthorizationTest.NullableConverter.class) String value) {
        var sessionRequest = sessionRequest().userName(value).build();
        var sessionService = new SessionService(tokenService);

        var sessionPublisher = sessionService.forNew(sessionRequest);

        StepVerifier.create(sessionPublisher)
                .expectErrorSatisfies(throwable -> assertThat(((ClientError)throwable).getHttpStatus() == UNAUTHORIZED))
                .verify();
    }

    @ParameterizedTest
    @CsvSource({
            ",",
            "empty",
            "null"
    })
    void returnUnAuthorizedErrorWhenPasswordIsEmpty(
            @ConvertWith(AuthorizationTest.NullableConverter.class) String value) {
        var sessionRequest = sessionRequest().password(value).build();
        var sessionService = new SessionService(tokenService);

        var sessionPublisher = sessionService.forNew(sessionRequest);

        StepVerifier.create(sessionPublisher)
                .expectErrorSatisfies(throwable -> assertThat(((ClientError)throwable).getHttpStatus() == UNAUTHORIZED))
                .verify();
    }

    @Test
    void returnUnAuthorizedWhenAnyOtherErrorHappens() {
        var sessionRequest = sessionRequest().build();
        var sessionService = new SessionService(tokenService);
        when(tokenService.tokenForUser(any(), any())).thenReturn(Mono.error(new Exception()));

        var sessionPublisher = sessionService.forNew(sessionRequest);

        StepVerifier.create(sessionPublisher)
                .expectErrorSatisfies(throwable -> assertThat(((ClientError)throwable).getHttpStatus() == UNAUTHORIZED))
                .verify();
    }
}