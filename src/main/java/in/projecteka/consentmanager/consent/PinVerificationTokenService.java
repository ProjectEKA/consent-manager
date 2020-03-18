package in.projecteka.consentmanager.consent;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.log4j.Logger;

import java.security.PublicKey;
import java.util.Optional;
import java.util.function.Function;

@AllArgsConstructor
public class PinVerificationTokenService {
    private final static Logger logger = Logger.getLogger(PinVerificationTokenService.class);
    private final PublicKey publicKey;

    public boolean validateToken(String authToken) {
        try {
            var userName = claim(from(authToken), Claims::getSubject);
            return userName != null;
        } catch (ExpiredJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
            logger.error(e);
            return false;
        }
    }

    public Optional<String> usernameFrom(String authToken) {
        try {
            return Optional.ofNullable(claim(from(authToken), Claims::getSubject));
        } catch (ExpiredJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
            logger.error(e);
            return Optional.empty();
        }
    }

    private <T> T claim(Jws<Claims> claims, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(claims.getBody());
    }

    @SneakyThrows
    private Jws<Claims> from(String authToken) {
        String token = authToken.replaceAll("Bearer ", "");
        return Jwts.parser().setSigningKey(publicKey).parseClaimsJws(token);
    }
}
