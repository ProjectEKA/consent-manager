package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.NullableConverter;
import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.OtpServiceClient;
import in.projecteka.consentmanager.clients.model.OtpRequest;
import in.projecteka.consentmanager.clients.properties.OtpServiceProperties;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.user.model.LogoutRequest;
import in.projecteka.consentmanager.user.model.OtpVerificationRequest;
import in.projecteka.consentmanager.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

class SessionServiceTest {

    @Captor
    private ArgumentCaptor<OtpRequest> otpRequestArgumentCaptor;

    @Mock
    TokenService tokenService;

    @Mock
    CacheAdapter<String, String> blacklistedTokens;

    @Mock
    private OtpServiceClient otpServiceClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpServiceProperties otpServiceProperties;

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
        var sessionService = new SessionService(tokenService, null,null,null, null);

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
        var sessionService = new SessionService(tokenService, null,null,null,null);

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
        var sessionService = new SessionService(tokenService, null,null,null,null);

        var sessionPublisher = sessionService.forNew(sessionRequest);

        StepVerifier.create(sessionPublisher)
                .expectErrorSatisfies(throwable -> assertThat(((ClientError) throwable).getHttpStatus() == UNAUTHORIZED))
                .verify();
    }

    @Test
    void returnUnAuthorizedWhenAnyOtherErrorHappens() {
        var sessionRequest = sessionRequest().build();
        var sessionService = new SessionService(tokenService, null,null,null,null);
        when(tokenService.tokenForUser(any(), any())).thenReturn(Mono.error(new Exception()));

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
        when(blacklistedTokens.put(String.format(BLACKLIST_FORMAT, BLACKLIST, testAccessToken),"")).
                thenReturn(Mono.empty());
        when(tokenService.revoke(refreshToken)).thenReturn(Mono.empty());
        SessionService sessionService = new SessionService(tokenService, blacklistedTokens,null,null,null);
        Mono<Void> logout = sessionService.logout(testAccessToken, logoutRequest);

        StepVerifier.create(logout).verifyComplete();
        verify(blacklistedTokens).put(String.format(BLACKLIST_FORMAT, BLACKLIST, testAccessToken),"");
        verify(tokenService).revoke(refreshToken);
    }

    @Test
    public void shouldMakeOtpRequestAndReturnSessionId() {
        String username = "foobar@ncg";
        String testPhone = "9876543210";

        when(userRepository.userWith(username)).thenReturn(Mono.just(User.builder().phone(testPhone).build()));
        when(otpServiceClient.send(otpRequestArgumentCaptor.capture())).thenReturn(Mono.empty());
        when(otpServiceProperties.getExpirationTime()).thenReturn(300);

        SessionService sessionService = new SessionService(tokenService, blacklistedTokens, userRepository, otpServiceClient, otpServiceProperties);
        StepVerifier.create(sessionService.sendOtp(new OtpVerificationRequest(username))).
                assertNext(response -> {
                    assertThat(response.getSessionId()).isNotEmpty();
                    assertThat(response.getExpirationTime()).isEqualTo(300);
                    assertThat(response.getMobile()).isEqualTo(testPhone);
                }).verifyComplete();

        verify(userRepository).userWith(username);
        assertThat(otpRequestArgumentCaptor.getValue().getCommunication().getValue().equals(testPhone)).isTrue();
    }

    @Test
    public void shouldThrowExceptionWhenNoUserFound() {
        String username = "foobar@ncg";

        when(userRepository.userWith(username)).thenReturn(Mono.empty());
        SessionService sessionService = new SessionService(tokenService, blacklistedTokens, userRepository, otpServiceClient, otpServiceProperties);
        StepVerifier.create(sessionService.sendOtp(new OtpVerificationRequest(username)))
                .expectErrorSatisfies(throwable -> assertThat(((ClientError) throwable).getHttpStatus() == NOT_FOUND))
                .verify();
    }
}