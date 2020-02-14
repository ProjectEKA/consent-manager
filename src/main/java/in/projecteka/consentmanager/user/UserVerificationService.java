package in.projecteka.consentmanager.user;

import com.google.common.cache.LoadingCache;
import in.projecteka.consentmanager.user.exception.CacheNotAccessibleException;
import in.projecteka.consentmanager.user.exception.InvalidSessionException;
import in.projecteka.consentmanager.user.model.TemporarySession;
import in.projecteka.consentmanager.user.model.Token;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class UserVerificationService {

    private JWTProperties jwtProperties;
    private LoadingCache<String, Optional<String>> unverifiedSessions;
    private LoadingCache<String, Optional<String>> verifiedSessions;
    public static final long JWT_TOKEN_VALIDITY = 5 * 60 * 60;

    public UserVerificationService(JWTProperties jwtProperties,
                                   LoadingCache<String, Optional<String>> unverifiedSessions,
                                   LoadingCache<String, Optional<String>> verifiedSessions) {
        this.jwtProperties = jwtProperties;
        this.unverifiedSessions = unverifiedSessions;
        this.verifiedSessions = verifiedSessions;
    }

    public TemporarySession cacheAndSendSession(String sessionId, String mobileNumber) {
        TemporarySession temporarySession = new TemporarySession(sessionId);
        unverifiedSessions.put(temporarySession.getSessionId(), Optional.of(mobileNumber));
        return temporarySession;
    }

    public Boolean validateToken(String token) {
        return !isTokenExpired(token) && !StringUtils.isEmpty(getSessionFromToken(token));
    }

    public String getSessionFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public Token generateToken(String sessionId) {
        try {
            return unverifiedSessions.get(sessionId)
                    .map(number -> {
                        String newSession = UUID.randomUUID().toString();
                        verifiedSessions.put(newSession, Optional.of(number));
                        return generateToken(new HashMap<>(), newSession);
                    })
                    .orElseThrow(() -> new InvalidSessionException("invalid.session.id"));

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

    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }


    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }


    private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser().setSigningKey(jwtProperties.getSecret()).parseClaimsJws(token).getBody();
    }
}
