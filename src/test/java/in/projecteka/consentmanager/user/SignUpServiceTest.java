package in.projecteka.consentmanager.user;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import in.projecteka.consentmanager.clients.model.OtpCommunicationData;
import in.projecteka.consentmanager.clients.model.OtpRequest;
import in.projecteka.consentmanager.user.model.SignUpSession;
import in.projecteka.consentmanager.user.model.Token;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SignUpServiceTest {

    @Mock
    private JWTProperties jwtProperties;

    @Mock
    private LoadingCache<String, Optional<String>> unverifiedSessions;

    @Mock
    private LoadingCache<String, Optional<String>> verifiedSessions;

    private SignUpService signupService;

    EasyRandom easyRandom;

    @BeforeEach
    public void setUp() {
        easyRandom = new EasyRandom();
        MockitoAnnotations.initMocks(this);
        signupService = new SignUpService(jwtProperties, unverifiedSessions, verifiedSessions);
    }

    @Test
    public void shouldCreateAndSendSessionId() {
        String value = easyRandom.nextObject(String.class);
        OtpCommunicationData communicationData = new OtpCommunicationData("MOBILE", value);
        var sessionId = easyRandom.nextObject(String.class);
        OtpRequest otpRequest = new OtpRequest(sessionId, communicationData);
        SignUpSession expectedResponse = new SignUpSession(sessionId);

        assertThat(signupService.cacheAndSendSession(
                otpRequest.getSessionId(),
                otpRequest.getCommunication().getValue()))
                .isEqualTo(expectedResponse);
        verify(unverifiedSessions).put(sessionId, Optional.of(value));
    }

    @Test
    public void shouldGenerateToken() throws ExecutionException {
        var sessionId = easyRandom.nextObject(String.class);
        when(jwtProperties.getSecret()).thenReturn(easyRandom.nextObject(String.class));
        when(unverifiedSessions.get(sessionId)).thenReturn(Optional.of(easyRandom.nextObject(String.class)));

        assertThat(signupService.generateToken(sessionId)).isInstanceOf(Token.class);
    }

    @Test
    public void invalidateSession() throws ExecutionException {
        var sessionId = easyRandom.nextObject(String.class);
        var verifiedSessions = CacheBuilder
                .newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build(new CacheLoader<String, Optional<String>>() {
                    public Optional<String> load(String key) {
                        return Optional.empty();
                    }
                });
        var value = Optional.of("Something");
        verifiedSessions.put(sessionId, value);
        var signUpService = new SignUpService(null, null, verifiedSessions);

        assertThat(verifiedSessions.get(sessionId)).isEqualTo(value);
        signUpService.removeOf(sessionId);
        assertThat(verifiedSessions.get(sessionId)).isEmpty();
    }
}