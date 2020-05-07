package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.NullableConverter;
import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.user.exception.InvalidPasswordException;
import in.projecteka.consentmanager.user.exception.InvalidUserNameException;
import in.projecteka.consentmanager.user.model.LogoutRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static in.projecteka.consentmanager.common.Constants.BLACKLIST;
import static in.projecteka.consentmanager.common.Constants.BLACKLIST_FORMAT;
import static in.projecteka.consentmanager.user.TestBuilders.session;
import static in.projecteka.consentmanager.user.TestBuilders.sessionRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

class SessionServiceTest {

    @Mock
    TokenService tokenService;

    @Mock
    CacheAdapter<String, String> blacklistedTokens;

    @Mock
    LockedUserService lockedUserService;

    @BeforeEach
    void init() {
        initMocks(this);
    }

    @Test
    void returnSession() {
        var sessionRequest = sessionRequest().build();
        var expectedSession = session().build();
        when(tokenService.tokenForUser(sessionRequest.getUsername(), sessionRequest.getPassword()))
                .thenReturn(Mono.just(expectedSession));
        var sessionService = new SessionService(tokenService, null, lockedUserService);

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
    void returnUnAuthorizedErrorWhenUsernameIsEmpty(@ConvertWith(NullableConverter.class) String value) {
        var sessionRequest = sessionRequest().username(value).build();
        var sessionService = new SessionService(tokenService, null, lockedUserService);

        var sessionPublisher = sessionService.forNew(sessionRequest);

        StepVerifier.create(sessionPublisher)
                .expectErrorSatisfies(throwable ->
                        assertThat(((ClientError) throwable).getHttpStatus() == UNAUTHORIZED))
                .verify();
    }

    @ParameterizedTest
    @CsvSource({
            ",",
            "empty",
            "null"
    })
    void returnUnAuthorizedErrorWhenPasswordIsEmpty(
            @ConvertWith(NullableConverter.class) String value) {
        var sessionRequest = sessionRequest().password(value).build();
        var sessionService = new SessionService(tokenService, null, lockedUserService);

        var sessionPublisher = sessionService.forNew(sessionRequest);

        StepVerifier.create(sessionPublisher)
                .expectErrorSatisfies(throwable -> assertThat(((ClientError) throwable).getHttpStatus() == UNAUTHORIZED))
                .verify();
    }

    @Test
    void returnUnAuthorizedWhenAnyTokenServiceThrowsInvalidPasswordException() {
        var sessionRequest = sessionRequest().build();
        var sessionService = new SessionService(tokenService, null, lockedUserService);
        when(tokenService.tokenForUser(any(), any())).thenReturn(Mono.error(new InvalidPasswordException()));
        when(lockedUserService.userFor(any())).thenReturn(Mono.empty());
        when(lockedUserService.createUser(any())).thenReturn(Mono.empty());

        var sessionPublisher = sessionService.forNew(sessionRequest);

        StepVerifier.create(sessionPublisher)
                .expectErrorSatisfies(throwable -> assertThat(((ClientError) throwable).getHttpStatus() == UNAUTHORIZED))
                .verify();
    }

    @Test
    void returnUnAuthorizedWhenAnyTokenServiceThrowsInvalidUserNameException() {
        var sessionRequest = sessionRequest().build();
        var sessionService = new SessionService(tokenService, null, lockedUserService);
        when(tokenService.tokenForUser(any(), any())).thenReturn(Mono.error(new InvalidUserNameException()));
        when(lockedUserService.userFor(any())).thenReturn(Mono.empty());
        when(lockedUserService.createUser(any())).thenReturn(Mono.empty());

        var sessionPublisher = sessionService.forNew(sessionRequest);

        StepVerifier.create(sessionPublisher)
                .expectErrorSatisfies(throwable -> assertThat(((ClientError) throwable).getHttpStatus() == UNAUTHORIZED))
                .verify();
    }

    @Test
    public void shouldBlackListToken() {
        String testAccessToken = "accessToken";
        String refreshToken = "refreshToken";
        LogoutRequest logoutRequest = new LogoutRequest(refreshToken);
        when(blacklistedTokens.put(String.format(BLACKLIST_FORMAT, BLACKLIST, testAccessToken), "")).
                thenReturn(Mono.empty());
        when(tokenService.revoke(refreshToken)).thenReturn(Mono.empty());
        SessionService sessionService = new SessionService(tokenService, blacklistedTokens, lockedUserService);
        Mono<Void> logout = sessionService.logout(testAccessToken, logoutRequest);

        StepVerifier.create(logout).verifyComplete();
        verify(blacklistedTokens).put(String.format(BLACKLIST_FORMAT, BLACKLIST, testAccessToken), "");
        verify(tokenService).revoke(refreshToken);
    }
}