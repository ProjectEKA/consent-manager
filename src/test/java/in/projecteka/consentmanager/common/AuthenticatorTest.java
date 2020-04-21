package in.projecteka.consentmanager.common;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import reactor.core.publisher.Mono;

import java.text.ParseException;

import static in.projecteka.consentmanager.common.Constants.BLACKLIST;
import static in.projecteka.consentmanager.common.Constants.BLACKLIST_FORMAT;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
class AuthenticatorTest {
    @Mock
    private ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
    @Mock
    private CacheAdapter<String, String> blacklistedTokens;
    @Mock
    private JWKSet jwkSet;
    @InjectMocks
    @Spy
    private Authenticator authenticator;
    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldReturnNoCallerWhenBlacklisted() {
        String accessToken = "accesstoken";
        String testToken = String.format("%s %s","Bearer", accessToken);
        when(blacklistedTokens.exists(String.format(BLACKLIST_FORMAT,BLACKLIST,accessToken))).thenReturn(Mono.just(true));

        Caller caller = authenticator.verify(testToken).block();
        Assert.assertNull(caller);
    }

    @Test
    public void shouldReturnCallerWhenNotBlackListed() throws ParseException, JOSEException, BadJOSEException {
        String accessToken = "accesstoken";
        String testUserName = "testuser";
        String testToken = String.format("%s %s","Bearer", accessToken);
        JWTClaimsSet jwtClaimsSet = Mockito.mock(JWTClaimsSet.class);
        when(blacklistedTokens.exists(String.format(BLACKLIST_FORMAT,BLACKLIST,accessToken))).thenReturn(Mono.just(false));
        when(jwtProcessor.process(accessToken, null)).thenReturn(jwtClaimsSet);
        when(jwtClaimsSet.getStringClaim("preferred_username")).thenReturn(testUserName);
        Caller expectedCaller = Mockito.mock(Caller.class);
        when(authenticator.from(testUserName)).thenReturn(expectedCaller);

        Caller caller = authenticator.verify(testToken).block();

        Assert.assertEquals(expectedCaller, caller);
        verify(blacklistedTokens).exists(String.format(BLACKLIST_FORMAT,BLACKLIST,accessToken));
        verify(jwtProcessor).process(accessToken, null);
        verify(authenticator).from(testUserName);
    }
}