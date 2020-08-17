package in.projecteka.consentmanager.consent;

import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.common.Caller;
import in.projecteka.library.common.cache.CacheAdapter;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.security.PublicKey;
import java.util.Optional;
import java.util.function.Function;

@AllArgsConstructor
public class PinVerificationTokenService {
    private final PublicKey publicKey;
    private final CacheAdapter<String,String> usedTokens;
    private static final Logger logger = LoggerFactory.getLogger(PinVerificationTokenService.class);

    public Mono<Caller> validateToken(String authToken, String validScope) {
        try {
            Optional<String> optionalSessionId = sessionIdFrom(authToken);
            Optional<String> optionalScope = scopeFrom(authToken);
            if(optionalSessionId.isEmpty() || optionalScope.isEmpty()) {
                return Mono.empty();
            }
            String scope = optionalScope.get();
            logger.debug("Got scope {} on token", scope);
            if (!scope.equals(validScope)) {
                return Mono.error(ClientError.invalidScope());
            }
            String sessionId = optionalSessionId.get();
            return usedTokens.exists(sessionId)
                    .filter(exists -> {
                        logger.debug("Session id {} does exist? {}",sessionId,exists);
                        return !exists;
                    })
                    .flatMap(doesNotExist -> usernameFrom(authToken).
                            map(username -> Mono.just(new Caller(username, false,sessionId))).
                            orElse(Mono.empty()));
        } catch (ExpiredJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
            logger.error(e.getMessage(),e);
            return Mono.empty();
        }
    }

    private Optional<String> scopeFrom(String authToken) {
        return fetchClaim(authToken, "scope");
    }

    private Optional<String> sessionIdFrom(String token) {
        return fetchClaim(token, "sid");
    }

    private Optional<String> fetchClaim(String token, String claim) {
        try {
            return Optional.ofNullable(claim(from(token), claims -> claims.get(claim).toString()));
        } catch (ExpiredJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
            return Optional.empty();
        }
    }

    private Optional<String> usernameFrom(String token) {
        try {
            return Optional.ofNullable(claim(from(token), Claims::getSubject));
        } catch (ExpiredJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
            logger.error(e.getMessage(),e);
            return Optional.empty();
        }
    }

    private <T> T claim(Jws<Claims> claims, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(claims.getBody());
    }

    private Jws<Claims> from(String authToken) {
        return Jwts.parser().setSigningKey(publicKey).parseClaimsJws(authToken);
    }
}
