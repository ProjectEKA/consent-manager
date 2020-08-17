package in.projecteka.consentmanager.link.link;

import in.projecteka.consentmanager.link.link.model.AuthzHipAction;
import in.projecteka.library.clients.model.ClientError;
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
public class LinkTokenVerifier {
    //TODO inject the public key
    private final PublicKey publicKey;
    private final LinkRepository linkRepository;

    private static final Logger logger = LoggerFactory.getLogger(LinkTokenVerifier.class);
    public static final String ERROR_TOKEN_IS_INVALID_OR_EXPIRED = "Token is invalid or expired";
    private static final String ERROR_NUMBER_OF_REPEAT_ACTION_EXCEEDED = "Number of repeat action exceeded";
    private static final String ERROR_INVALID_TOKEN_FOR_PURPOSE = "Invalid token for intended action";
    public static final String ERROR_INVALID_TOKEN_REQUIRED_ATTRIBUTES_NOT_PRESENT = "Invalid Token. Required attributes are not present.";
    public static final String ERROR_INVALID_TOKEN_NO_HIP_ID = "Invalid Token. AccessToken does not have hipId";

    public Mono<String> getHipIdFromToken(String accessToken) {
        Optional<String> optionalHipId = hipIdFrom(accessToken);
        if (optionalHipId.isEmpty()) {
            logger.error(ERROR_INVALID_TOKEN_NO_HIP_ID);
            return Mono.error(ClientError.invalidToken(ERROR_INVALID_TOKEN_NO_HIP_ID));
        }
        return Mono.just(optionalHipId.get());
    }

    public Mono<AuthzHipAction> validateSession(String accessToken) {
        Optional<String> optionalSessionId = sessionIdFrom(accessToken);
        if (optionalSessionId.isEmpty()) {
            logger.error(ERROR_INVALID_TOKEN_REQUIRED_ATTRIBUTES_NOT_PRESENT);
            return Mono.error(ClientError.invalidToken(ERROR_INVALID_TOKEN_REQUIRED_ATTRIBUTES_NOT_PRESENT));
        }
        String sessionId = optionalSessionId.get();
        return linkRepository.getAuthzHipAction(sessionId)
                .switchIfEmpty(logAndThrowError(ERROR_TOKEN_IS_INVALID_OR_EXPIRED));
    }

    public Mono<AuthzHipAction> validateHipAction(AuthzHipAction authHipAction, String purpose) {
        if ((authHipAction.getCurrentCounter() + 1) > authHipAction.getRepeat()) {
            logger.error(ERROR_NUMBER_OF_REPEAT_ACTION_EXCEEDED);//TODO
            return Mono.error(ClientError.invalidToken(ERROR_NUMBER_OF_REPEAT_ACTION_EXCEEDED));
        }
        if (!authHipAction.getPurpose().toUpperCase().equals(purpose)) {
            logger.error(ERROR_INVALID_TOKEN_FOR_PURPOSE);//TODO
            return Mono.error(ClientError.invalidToken(ERROR_INVALID_TOKEN_FOR_PURPOSE));
        }

        return Mono.just(authHipAction);

    }

    private Mono<AuthzHipAction> logAndThrowError(String errorMessage) {
        logger.error(errorMessage);//TODO
        return Mono.error(ClientError.invalidToken(errorMessage));
    }


    private Optional<String> sessionIdFrom(String token) { return fetchClaim(token, "sessionId"); }

    private Optional<String> patientIdFrom(String token) { return fetchClaim(token, "healthId"); }

    private Optional<String> hipIdFrom(String token) { return fetchClaim(token, "hipId"); }

    private Optional<String> repeatFrom(String token) { return fetchClaim(token, "repeat"); }

    private Optional<String> fetchClaim(String token, String claim) {
        try {
            return Optional.ofNullable(claim(from(token), claims -> claims.get(claim).toString()));
        } catch (ExpiredJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
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