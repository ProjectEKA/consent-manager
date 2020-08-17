package common;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import in.projecteka.library.common.Authenticator;
import in.projecteka.library.common.Caller;
import in.projecteka.library.common.cache.CacheAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import reactor.core.publisher.Mono;

import java.text.ParseException;

import static in.projecteka.library.common.Constants.BLACKLIST;
import static in.projecteka.library.common.Constants.BLACKLIST_FORMAT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

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
    void init() {
        initMocks(this);
    }

    @Test
    void shouldReturnNoCallerWhenBlacklisted() {
        String accessToken = "accesstoken";
        String testToken = String.format("%s %s","Bearer", accessToken);
        when(blacklistedTokens.exists(String.format(BLACKLIST_FORMAT,BLACKLIST,accessToken))).thenReturn(Mono.just(true));

        Caller caller = authenticator.verify(testToken).block();
        assertNull(caller);
    }

    @Test
    void shouldReturnCallerWhenNotBlackListed() throws ParseException, JOSEException, BadJOSEException {
        String accessToken = "accesstoken";
        String testUserName = "testuser";
        String testToken = String.format("%s %s","Bearer", accessToken);
        JWTClaimsSet jwtClaimsSet = Mockito.mock(JWTClaimsSet.class);
        when(blacklistedTokens.exists(String.format(BLACKLIST_FORMAT,BLACKLIST,accessToken))).thenReturn(Mono.just(false));
        when(jwtProcessor.process(accessToken, null)).thenReturn(jwtClaimsSet);
        when(jwtClaimsSet.getStringClaim("preferred_username")).thenReturn(testUserName);
        Caller expectedCaller = new Caller("testuser", false);

        Caller caller = authenticator.verify(testToken).block();

        assertEquals(expectedCaller, caller);
        verify(blacklistedTokens).exists(String.format(BLACKLIST_FORMAT,BLACKLIST,accessToken));
        verify(jwtProcessor).process(accessToken, null);
    }
}