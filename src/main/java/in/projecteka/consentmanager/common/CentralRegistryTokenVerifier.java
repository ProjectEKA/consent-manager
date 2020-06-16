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
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static reactor.core.publisher.Mono.just;

public class CentralRegistryTokenVerifier {
    private final ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
    private final Logger logger = LoggerFactory.getLogger(CentralRegistryTokenVerifier.class);

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
                new HashSet<>(Arrays.asList("sub", "iat", "exp", "scope", "clientId", "resource_access"))));
    }

    public Mono<ServiceCaller> verify(String token) {
        try {
            var parts = token.split(" ");
            if (parts.length == 2) {
                var credentials = parts[1];
                return Mono.justOrEmpty(jwtProcessor.process(credentials, null))
                        .flatMap(jwtClaimsSet -> {
                            try {
                                var clientId = jwtClaimsSet.getStringClaim("clientId");
                                var serviceCaller = new ServiceCaller(clientId, getRole(jwtClaimsSet, clientId));
                                return just(serviceCaller);
                            } catch (Exception e) {
                                logger.error(e.getMessage(),e);
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

    private List<Role> getRole(JWTClaimsSet jwtClaimsSet, String clientId) {
        var resourceAccess = (JSONObject) jwtClaimsSet.getClaim("resource_access");
        var clientObject = (JSONObject) resourceAccess.get(clientId);
        return ((JSONArray) clientObject.get("roles"))
                .stream()
                .map(Object::toString)
                .map(mayBeRole -> Role.valueOfIgnoreCase(mayBeRole).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
