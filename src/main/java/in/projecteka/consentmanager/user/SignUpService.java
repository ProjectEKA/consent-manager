package in.projecteka.consentmanager.user;

import com.google.common.cache.LoadingCache;
import in.projecteka.consentmanager.user.exception.CacheNotAccessibleException;
import in.projecteka.consentmanager.user.exception.InvalidSessionException;
import in.projecteka.consentmanager.user.model.SignUpSession;
import in.projecteka.consentmanager.user.model.Token;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class SignUpService {

    private final static Logger logger = Logger.getLogger(SignUpService.class);
    private JWTProperties jwtProperties;
    private LoadingCache<String, Optional<String>> unverifiedSessions;
    private LoadingCache<String, Optional<String>> verifiedSessions;
    public static final long JWT_TOKEN_VALIDITY = 5 * 60 * 60;

    public SignUpService(JWTProperties jwtProperties,
                         LoadingCache<String, Optional<String>> unverifiedSessions,
                         LoadingCache<String, Optional<String>> verifiedSessions) {
        this.jwtProperties = jwtProperties;
        this.unverifiedSessions = unverifiedSessions;
        this.verifiedSessions = verifiedSessions;
    }

    public SignUpSession cacheAndSendSession(String sessionId, String mobileNumber) {
        SignUpSession signupSession = new SignUpSession(sessionId);
        unverifiedSessions.put(signupSession.getSessionId(), Optional.of(mobileNumber));
        return signupSession;
    }

    public Boolean validateToken(String token) {
        try {
            var session = sessionFrom(token);
            return isStillExists(session);
        } catch (ExpiredJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
            logger.error(e);
            return false;
        }
    }

    private Boolean isStillExists(String session) {
        return verifiedSessions.getIfPresent(session).isPresent();
    }

    public Token generateToken(String sessionId) {
        try {
            return unverifiedSessions.get(sessionId)
                    .map(number -> {
                        String newSession = UUID.randomUUID().toString();
                        verifiedSessions.put(newSession, Optional.of(number));
                        return generateToken(new HashMap<>(), newSession);
                    }).orElseThrow(() -> new InvalidSessionException("invalid.session.id"));
        } catch (ExecutionException e) {
            throw new CacheNotAccessibleException("cache.not.accessible");
        }
    }

    private Token generateToken(Map<String, Object> claims, String subject) {
        return new Token(Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY * 1000))
                .signWith(SignatureAlgorithm.HS512, jwtProperties.getSecret()).compact());
    }

    public String sessionFrom(String token) {
        return claim(from(token), Claims::getSubject);
    }

    public Optional<String> getMobileNumber(String sessionId) {
        return verifiedSessions.getIfPresent(sessionId);
    }

    private <T> T claim(Jws<Claims> claims, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(claims.getBody());
    }

    private Jws<Claims> from(String token) {
        return Jwts.parser().setSigningKey(jwtProperties.getSecret()).parseClaimsJws(token);
    }
}
