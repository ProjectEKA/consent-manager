package in.projecteka.consentmanager.user;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import in.projecteka.consentmanager.user.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.swing.text.html.Option;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthenticatorServiceTest {

    @Mock
    private JWTProperties jwtProperties;

    @Mock
    private LoadingCache<String, Optional<String>> sessionCache;

    private AuthenticatorService authenticatorService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        authenticatorService = new AuthenticatorService(jwtProperties, sessionCache);
        when(jwtProperties.getSecret()).thenReturn("SOME_SECRET");
        try {
            when(sessionCache.get(any())).thenReturn(Optional.of("1234567891"));
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void shouldCreateAndSendSessionId() {
        OtpCommunicationData communicationData = new OtpCommunicationData("MOBILE", "1234567891");
        OtpRequest otpRequest = new OtpRequest("SOME_SESSION_ID", communicationData);
        TemporarySession expectedResponse = new TemporarySession("SOME_SESSION_ID");

        assertThat(authenticatorService.cacheAndSendSession(otpRequest)).isEqualTo(expectedResponse);
        verify(sessionCache).put("SOME_SESSION_ID", Optional.of("1234567891"));
    }

    @Test
    public void shouldGenerateToken() {
        OtpVerification otpVerification = new OtpVerification("SOME_SESSION_ID", "12345");
       assertThat(authenticatorService.generateToken(otpVerification)).isInstanceOf(Token.class);
    }



}