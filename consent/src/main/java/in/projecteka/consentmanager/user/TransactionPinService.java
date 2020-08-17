package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.user.model.Token;
import in.projecteka.consentmanager.user.model.TransactionPin;
import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.common.cache.CacheAdapter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.core.publisher.Mono;

import java.security.PrivateKey;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import static in.projecteka.library.clients.model.ClientError.invalidAttemptsExceeded;
import static in.projecteka.library.clients.model.ClientError.requestAlreadyExists;
import static in.projecteka.library.clients.model.ClientError.transactionPinDidNotMatch;
import static in.projecteka.library.clients.model.ClientError.transactionPinNotFound;
import static java.lang.String.format;
import static reactor.core.publisher.Mono.defer;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.just;

@AllArgsConstructor
@Builder
public class TransactionPinService {
    private final TransactionPinRepository transactionPinRepository;
    private final BCryptPasswordEncoder encoder;
    private final PrivateKey privateKey;
    private final UserServiceProperties userServiceProperties;
    private final CacheAdapter<String, String> dayCache;
    private static final Logger logger = LoggerFactory.getLogger(TransactionPinService.class);

    public Mono<Void> createPinFor(String patientId, String pin) {
        if (isInvalidPin(pin)) {
            return error(ClientError.invalidTransactionPin());
        }
        String encodedPin = encoder.encode(pin);
        TransactionPin transactionPin = TransactionPin.builder().pin(encodedPin).patientId(patientId).build();
        return validateTransactionPin(patientId).then(transactionPinRepository.insert(transactionPin));
    }

    private boolean isInvalidPin(String pin) {
        String count = String.valueOf(userServiceProperties.getTransactionPinDigitSize());
        String regex = "^\\d{" + count + "}$";
        return !pin.matches(regex);
    }

    private Mono<Void> validateTransactionPin(String patientId) {
        return isTransactionPinSet(patientId)
                .flatMap(hasTransactionPin -> Boolean.TRUE.equals(hasTransactionPin)
                                              ? error(ClientError.transactionPinAlreadyCreated())
                                              : Mono.empty());
    }

    public Mono<Boolean> isTransactionPinSet(String patientId) {
        return transactionPinRepository.getTransactionPinByPatient(patientId).hasElement();
    }

    public Mono<Token> validatePinFor(String patientId, String pin, UUID requestId, String scope) {
        return transactionPinRepository.getTransactionPinByRequest(requestId)
                .flatMap(discard -> error(requestAlreadyExists()))
                .then(defer(() -> transactionPinRepository.updateRequestId(requestId, patientId)))
                .then(defer(() -> transactionPinRepository.getTransactionPinByPatient(patientId)))
                .switchIfEmpty(defer(() -> error(transactionPinNotFound())))
                .flatMap(transactionPin -> notBlockListed(patientId)
                        .switchIfEmpty(error(invalidAttemptsExceeded()))
                        .then(defer(() -> {
                            if (!encoder.matches(pin, transactionPin.getPin())) {
                                return errorFor(patientId);
                            }
                            return just(newTokenWith(patientId, scope));
                        })));
    }

    private Mono<Token> errorFor(String patientId) {
        return dayCache.increment(incorrectAttemptKey(patientId))
                .filter(count -> count != userServiceProperties.getMaxIncorrectPinAttempts())
                .switchIfEmpty(defer(() -> dayCache.put(blockedKey(patientId), "true")
                        .thenReturn(userServiceProperties.getMaxIncorrectPinAttempts())))
                .flatMap(attemptCount -> error(transactionPinDidNotMatch(format("%s attempts left",
                        userServiceProperties.getMaxIncorrectPinAttempts() - attemptCount))));
    }

    private Mono<Boolean> notBlockListed(String patientId) {
        return dayCache.exists(blockedKey(patientId)).filter(exists -> !exists);
    }

    static String incorrectAttemptKey(String patientId) {
        return format("%s.incorrect.attempts", patientId);
    }

    static String blockedKey(String patientId) {
        return format("%s.blocked", patientId);
    }

    @SneakyThrows
    Token newTokenWith(String userName, String scope) {
        int minutes = userServiceProperties.getTransactionPinTokenValidity() * 60 * 1000;
        HashMap<String, Object> claims = new HashMap<>(1);
        claims.put("sid", UUID.randomUUID().toString());
        claims.put("scope", scope);
        logger.debug("Putting session id {}", claims.get("sid"));
        logger.debug("Putting scope {}", claims.get("scope"));
        return new Token(Jwts.builder()
                .setClaims(claims)
                .setSubject(userName)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + minutes))
                .signWith(SignatureAlgorithm.RS512, privateKey).compact());
    }

    public Mono<Void> changeTransactionPinFor(String username, String pin) {
        if (isInvalidPin(pin)) {
            return error(ClientError.invalidTransactionPin());
        }
        String encodedPin = encoder.encode(pin);
        return transactionPinRepository.changeTransactionPin(username, encodedPin);
    }
}
