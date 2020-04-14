package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.common.cache.ICacheAdapter;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class SignUpService {
    public final long jwtTokenValidity;
    private static final Logger logger = LoggerFactory.getLogger(SignUpService.class);
    private final JWTProperties jwtProperties;
    private final ICacheAdapter<String, Optional<String>> unverifiedSessions;
    private final ICacheAdapter<String, Optional<String>> verifiedSessions;

    public SignUpService(JWTProperties jwtProperties,
                         ICacheAdapter<String, Optional<String>> unverifiedSessions,
                         ICacheAdapter<String, Optional<String>> verifiedSessions,
                         int tokenValidityInMinutes) {
        this.jwtProperties = jwtProperties;
        this.unverifiedSessions = unverifiedSessions;
        this.verifiedSessions = verifiedSessions;
        jwtTokenValidity = tokenValidityInMinutes * 60 * 60;
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
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    private Boolean isStillExists(String session) {
        var optionalSession = verifiedSessions.getIfPresent(session);
        return optionalSession != null && optionalSession.isPresent();
    }

    public Token generateToken(String sessionId) {
        return unverifiedSessions.get(sessionId)
                .map(number -> {
                    String newSession = UUID.randomUUID().toString();
                    verifiedSessions.put(newSession, Optional.of(number));
                    return generateToken(new HashMap<>(), newSession);
                }).orElseThrow(() -> new InvalidSessionException("invalid.session.id"));
    }

    private Token generateToken(Map<String, Object> claims, String subject) {
        return new Token(Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtTokenValidity * 1000))
                .signWith(SignatureAlgorithm.HS512, jwtProperties.getSecret()).compact());
    }

    public String sessionFrom(String token) {
        return claim(from(token), Claims::getSubject);
    }

    public void removeOf(String sessionId) {
        verifiedSessions.invalidate(sessionId);
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
