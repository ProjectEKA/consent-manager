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
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.apache.log4j.Logger;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;

public class CentralRegistryTokenVerifier {
    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
    private final Logger logger = Logger.getLogger(CentralRegistryTokenVerifier.class);

    public CentralRegistryTokenVerifier(JWKSet jwkSet) {
        var immutableJWKSet = new ImmutableJWKSet<>(jwkSet);
        jwtProcessor = new DefaultJWTProcessor<>();
        jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(JOSEObjectType.JWT));
        JWSAlgorithm expectedJWSAlg = JWSAlgorithm.RS256;
        JWSKeySelector<SecurityContext> keySelector;
        keySelector = new JWSVerificationKeySelector<>(expectedJWSAlg, immutableJWKSet);
        jwtProcessor.setJWSKeySelector(keySelector);
        jwtProcessor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
                new JWTClaimsSet.Builder().build(),
                new HashSet<>(Arrays.asList("sub", "iat", "exp", "scope", "clientId"))));
    }

    public Mono<Caller> verify(String token) {
        try {
            var parts = token.split(" ");
            if (parts.length == 2) {
                var credentials = parts[1];
                return Mono.justOrEmpty(jwtProcessor.process(credentials, null))
                        .flatMap(jwtClaimsSet -> {
                            try {
                                return Mono.just(new Caller(jwtClaimsSet.getStringClaim("clientId"), true));
                            } catch (ParseException e) {
                                logger.error(e);
                                return Mono.empty();
                            }
                        });
            }
            return Mono.empty();
        } catch (ParseException | BadJOSEException | JOSEException e) {
            logger.error("Unauthorized access", e);
            return Mono.empty();
        }
    }
}
