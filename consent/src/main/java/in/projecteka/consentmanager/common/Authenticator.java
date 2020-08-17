package in.projecteka.consentmanager.common;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import in.projecteka.library.common.cache.CacheAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;

import static in.projecteka.consentmanager.common.Constants.BLACKLIST;
import static in.projecteka.consentmanager.common.Constants.BLACKLIST_FORMAT;

public class Authenticator {

    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
    private final Logger logger = LoggerFactory.getLogger(Authenticator.class);
    private final CacheAdapter<String, String> blacklistedTokens;

    public Authenticator(JWKSet jwkSet, CacheAdapter<String, String> blacklistedTokens, ConfigurableJWTProcessor<SecurityContext> jwtProcessor) {
        this.blacklistedTokens = blacklistedTokens;
        var immutableJWKSet = new ImmutableJWKSet<>(jwkSet);
        this.jwtProcessor = jwtProcessor;
        this.jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(JOSEObjectType.JWT));
        JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS256;
        JWSKeySelector<SecurityContext> keySelector;
        keySelector = new JWSVerificationKeySelector<>(expectedJWSAlg, immutableJWKSet);
        this.jwtProcessor.setJWSKeySelector(keySelector);
        this.jwtProcessor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
                new JWTClaimsSet.Builder().build(),
                new HashSet<>(Arrays.asList("sub", "iat", "exp", "scope", "preferred_username"))));
    }

    public Mono<Caller> verify(String token) {
        logger.debug("Authenticating {}", token);
        var parts = token.split(" ");
        if (parts.length != 2) {
            return Mono.empty();
        }
        var credentials = parts[1];
        return blacklistedTokens.exists(String.format(BLACKLIST_FORMAT, BLACKLIST, credentials))
                .filter(exists -> !exists)
                .flatMap(uselessFalse -> {
                    JWTClaimsSet jwtClaimsSet;
                    try {
                        jwtClaimsSet = jwtProcessor.process(credentials, null);
                    } catch (ParseException | BadJOSEException | JOSEException e) {
                        logger.error("Unauthorized access", e);
                        return Mono.empty();
                    }
                    try {
                        return Mono.just(from(jwtClaimsSet.getStringClaim("preferred_username")));
                    } catch (ParseException e) {
                        logger.error(e.getMessage());
                        return Mono.empty();
                    }
                });
    }

    protected Caller from(String preferredUsername) {
        final String serviceAccountPrefix = "service-account-";
        var serviceAccount = preferredUsername.startsWith(serviceAccountPrefix);
        var userName = serviceAccount ? preferredUsername.substring(serviceAccountPrefix.length()) : preferredUsername;
        return new Caller(userName, serviceAccount);
    }
}
