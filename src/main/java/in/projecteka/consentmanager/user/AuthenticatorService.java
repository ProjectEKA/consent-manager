package in.projecteka.consentmanager.user;

import com.google.common.cache.LoadingCache;
import in.projecteka.consentmanager.user.exception.CacheNotAccessibleException;
import in.projecteka.consentmanager.user.exception.InvalidSessionException;
import in.projecteka.consentmanager.user.model.OtpRequest;
import in.projecteka.consentmanager.user.model.OtpVerification;
import in.projecteka.consentmanager.user.model.TemporarySession;
import in.projecteka.consentmanager.user.model.Token;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ExecutionException  ;
import java.util.function.Function;

public class AuthenticatorService {

    private JWTProperties jwtProperties;
    private LoadingCache<String, Optional<String>> sessionCache;
    private LoadingCache<String, Optional<String>> secondSessionCache;
    public static final long JWT_TOKEN_VALIDITY = 5 * 60 * 60;

    public AuthenticatorService(JWTProperties jwtProperties,
                                LoadingCache<String, Optional<String>> sessionCache,
                                LoadingCache<String, Optional<String>> secondSessionCache) {
        this.jwtProperties = jwtProperties;
        this.sessionCache = sessionCache;
        this.secondSessionCache = secondSessionCache;
    }

    public TemporarySession cacheAndSendSession(OtpRequest requestBody) {
        TemporarySession temporarySession = new TemporarySession(requestBody.getSessionId());
        sessionCache.put(temporarySession.getSessionId(), Optional.of(requestBody.getCommunication().getValue()));
        return temporarySession;
    }

    public Boolean validateToken(String token) {
        return !isTokenExpired(token) && !StringUtils.isEmpty(getSessionFromToken(token));
    }

    public String getSessionFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public Token generateToken(OtpVerification request) {
        try {
            return sessionCache.get(request.getSessionId())
                    .map(number -> {
                        String newSession = UUID.randomUUID().toString();
                        secondSessionCache.put(newSession, Optional.of(number));
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


    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser().setSigningKey(jwtProperties.getSecret()).parseClaimsJws(token).getBody();
    }



}
