package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.common.Caller;
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
    private static final Logger logger = LoggerFactory.getLogger(PinVerificationTokenService.class);

    public Mono<Caller> validateToken(String authToken) {
        try {
            return usernameFrom(authToken).map(username -> Mono.just(new Caller(username, false))).orElse(Mono.empty());
        } catch (ExpiredJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
            logger.error(e.getMessage(),e);
            return Mono.empty();
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
