package in.projecteka.consentmanager.user;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import in.projecteka.consentmanager.clients.model.OtpAction;
import in.projecteka.consentmanager.clients.model.OtpCommunicationData;
import in.projecteka.consentmanager.clients.model.OtpCreationDetail;
import in.projecteka.consentmanager.clients.model.OtpRequest;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.common.cache.LoadingCacheAdapter;
import in.projecteka.consentmanager.user.model.SignUpSession;
import in.projecteka.consentmanager.user.model.Token;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SignUpServiceTest {

    @Mock
    private JWTProperties jwtProperties;

    @Mock
    private CacheAdapter<String, String> unverifiedSessions;

    @Mock
    private CacheAdapter<String, String> verifiedSessions;

    private SignUpService signupService;

    EasyRandom easyRandom;

    @BeforeEach
    public void setUp() {
        easyRandom = new EasyRandom();
        MockitoAnnotations.initMocks(this);
        signupService = new SignUpService(jwtProperties, unverifiedSessions, verifiedSessions, 5);
    }

    @Test
    public void shouldCreateAndSendSessionId() {
        String value = easyRandom.nextObject(String.class);
        OtpCommunicationData communicationData = new OtpCommunicationData("MOBILE", value);
        var sessionId = easyRandom.nextObject(String.class);
        OtpRequest otpRequest = new OtpRequest(sessionId,
                communicationData,
                OtpCreationDetail
                        .builder()
                        .systemName("PHR App")
                        .action(OtpAction.REGISTRATION.toString()).build());
        SignUpSession expectedResponse = new SignUpSession(sessionId);
        when(unverifiedSessions.put(sessionId, otpRequest.getCommunication().getValue())).thenReturn(Mono.empty());

        assertThat(signupService.cacheAndSendSession(
                otpRequest.getSessionId(),
                otpRequest.getCommunication().getValue()).block())
                .isEqualTo(expectedResponse);
        verify(unverifiedSessions).put(sessionId, value);
    }

    @Test
    public void shouldGenerateToken() {
        var sessionId = easyRandom.nextObject(String.class);
        String number = easyRandom.nextObject(String.class);
        when(jwtProperties.getSecret()).thenReturn(easyRandom.nextObject(String.class));
        when(verifiedSessions.put(anyString(), eq(number))).thenReturn(Mono.empty());
        when(unverifiedSessions.get(sessionId)).thenReturn(Mono.just(number));

        assertThat(signupService.generateToken(sessionId).block()).isInstanceOf(Token.class);
    }

    @Test
    public void invalidateSession() throws ExecutionException {
        var sessionId = easyRandom.nextObject(String.class);
        var verifiedSessions = CacheBuilder
                .newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build(new CacheLoader<String, String>() {
                    public String load(String key) {
                        return "";
                    }
                });
        var verifiedSessionsAdapter = new LoadingCacheAdapter(verifiedSessions);
        var value = easyRandom.nextObject(String.class);
        verifiedSessions.put(sessionId, value);
        var signUpService = new SignUpService(null, null, verifiedSessionsAdapter, 0);

        assertThat(verifiedSessions.get(sessionId)).isEqualTo(value);
        signUpService.removeOf(sessionId);
        assertThat(verifiedSessions.get(sessionId)).isEmpty();
    }
}
