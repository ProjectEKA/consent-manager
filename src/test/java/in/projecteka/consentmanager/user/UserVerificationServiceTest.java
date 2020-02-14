package in.projecteka.consentmanager.user;

import com.google.common.cache.LoadingCache;
import in.projecteka.consentmanager.user.model.OtpCommunicationData;
import in.projecteka.consentmanager.user.model.OtpRequest;
import in.projecteka.consentmanager.user.model.TemporarySession;
import in.projecteka.consentmanager.user.model.Token;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class UserVerificationServiceTest {

    @Mock
    private JWTProperties jwtProperties;

    @Mock
    private LoadingCache<String, Optional<String>> unverifiedSessions;

    @Mock
    private LoadingCache<String, Optional<String>> verifiedSessions;

    private UserVerificationService userVerificationService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        userVerificationService = new UserVerificationService(jwtProperties, unverifiedSessions, verifiedSessions);
        when(jwtProperties.getSecret()).thenReturn("SOME_SECRET");
        try {
            when(unverifiedSessions.get(any())).thenReturn(Optional.of("1234567891"));
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void shouldCreateAndSendSessionId() {
        OtpCommunicationData communicationData = new OtpCommunicationData("MOBILE", "1234567891");
        OtpRequest otpRequest = new OtpRequest("SOME_SESSION_ID", communicationData);
        TemporarySession expectedResponse = new TemporarySession("SOME_SESSION_ID");

        assertThat(userVerificationService.
                cacheAndSendSession(otpRequest.getSessionId(), otpRequest.getCommunication().getValue()))
                .isEqualTo(expectedResponse);
        verify(unverifiedSessions).put("SOME_SESSION_ID", Optional.of("1234567891"));
    }

    @Test
    public void shouldGenerateToken() {
       assertThat(userVerificationService.generateToken("SOME_SESSION_ID")).isInstanceOf(Token.class);
    }



}