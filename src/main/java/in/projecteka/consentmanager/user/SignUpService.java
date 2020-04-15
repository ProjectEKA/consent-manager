package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.user.exception.InvalidSessionException;
import in.projecteka.consentmanager.user.model.SignUpSession;
import in.projecteka.consentmanager.user.model.Token;
import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;

public class SignUpService {
    public final long jwtTokenValidity;
    private static final Logger logger = LoggerFactory.getLogger(SignUpService.class);
    private final JWTProperties jwtProperties;
    private final CacheAdapter<String, String> unverifiedSessions;
    private final CacheAdapter<String, String> verifiedSessions;

    public SignUpService(JWTProperties jwtProperties,
                         CacheAdapter<String, String> unverifiedSessions,
                         CacheAdapter<String, String> verifiedSessions,
                         int tokenValidityInMinutes) {
        this.jwtProperties = jwtProperties;
        this.unverifiedSessions = unverifiedSessions;
        this.verifiedSessions = verifiedSessions;
        jwtTokenValidity = tokenValidityInMinutes * 60 * 60;
    }

    public Mono<SignUpSession> cacheAndSendSession(String sessionId, String mobileNumber) {
        SignUpSession signupSession = new SignUpSession(sessionId);
        return unverifiedSessions.put(signupSession.getSessionId(), mobileNumber)
                .then(Mono.just(signupSession));
    }

    public Mono<Boolean> validateToken(String token) {
        try {
            var session = sessionFrom(token);
            return verifiedSessions.
                    getIfPresent(session)
                    .flatMap(s -> Mono.just(true))
                    .switchIfEmpty(Mono.just(false));
        } catch (ExpiredJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
            return Mono.just(false);
        }
    }

    public Mono<Token> generateToken(String sessionId) {
       return unverifiedSessions.get(sessionId)
               .switchIfEmpty(Mono.error(new InvalidSessionException("invalid.session.id")))
                .flatMap(number -> {
                    String newSession = UUID.randomUUID().toString();
                    return verifiedSessions.put(newSession, number).thenReturn(newSession);
                }).flatMap(newSession -> generateToken(new HashMap<>(), newSession));
    }

    private Mono<Token> generateToken(Map<String, Object> claims, String subject) {
        return Mono.just(new Token(Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtTokenValidity * 1000))
                .signWith(SignatureAlgorithm.HS512, jwtProperties.getSecret()).compact()));
    }

    public String sessionFrom(String token) {
        return claim(from(token), Claims::getSubject);
    }

    public Mono<Void> removeOf(String sessionId) {
        return verifiedSessions.invalidate(sessionId);
    }

    public Mono<String> getMobileNumber(String sessionId) {
        return verifiedSessions.getIfPresent(sessionId);
    }

    private <T> T claim(Jws<Claims> claims, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(claims.getBody());
    }

    private Jws<Claims> from(String token) {
        return Jwts.parser().setSigningKey(jwtProperties.getSecret()).parseClaimsJws(token);
    }
}
