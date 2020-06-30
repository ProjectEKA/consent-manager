package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.user.model.Token;
import in.projecteka.consentmanager.user.model.TransactionPin;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.core.publisher.Mono;

import java.security.PrivateKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

@AllArgsConstructor
public class TransactionPinService {
    private final TransactionPinRepository transactionPinRepository;
    private final BCryptPasswordEncoder encoder;
    private final PrivateKey privateKey;
    private final UserServiceProperties userServiceProperties;
    private final CacheAdapter<String,String> dayCache;
    private static final Logger logger = LoggerFactory.getLogger(TransactionPinService.class);

    public Mono<Void> createPinFor(String patientId, String pin) {
        if (!isPinValid(pin)) {
            return Mono.error(ClientError.invalidTransactionPin());
        }
        String encodedPin = encoder.encode(pin);
        TransactionPin transactionPin = TransactionPin.builder().pin(encodedPin).patientId(patientId).build();
        return validateTransactionPin(patientId).then(transactionPinRepository.insert(transactionPin));
    }

    private boolean isPinValid(String pin) {
        String count = String.valueOf(userServiceProperties.getTransactionPinDigitSize());
        String regex = "^\\d{" + count + "}$";
        return pin.matches(regex);
    }

    private Mono<Void> validateTransactionPin(String patientId) {
        return isTransactionPinSet(patientId)
                .flatMap(hasTransactionPin -> Boolean.TRUE.equals(hasTransactionPin)
                                              ? Mono.error(ClientError.transactionPinAlreadyCreated())
                                              : Mono.empty());
    }

    public Mono<Boolean> isTransactionPinSet(String patientId) {
        return transactionPinRepository.getTransactionPinByPatient(patientId).map(Optional::isPresent);
    }

    public Mono<Token> validatePinFor(String patientId, String pin, UUID requestId, String scope) {
        return Mono.just(requestId)
                .filterWhen(this::validateRequest)
                .switchIfEmpty(Mono.error(ClientError.requestAlreadyExists()))
                .flatMap(val -> transactionPinRepository.updateRequestId(requestId, patientId)
                        .then(transactionPinRepository.getTransactionPinByPatient(patientId)
                                .filter(Optional::isPresent)
                                .switchIfEmpty(Mono.error(ClientError.transactionPinNotFound()))
                                .flatMap(transactionPin -> dayCache.exists(blockedKey(patientId))
                                        .filter(exists -> !exists)
                                        .switchIfEmpty(Mono.error(ClientError.invalidAttemptsExceeded()))
                                        .then(Mono.defer(()-> Mono.just(encoder.matches(pin, transactionPin.get().getPin()))))
                                        .filter(matches-> !matches)
                                        .flatMap(doesNotMatch -> dayCache.increment(incorrectAttemptKey(patientId))
                                                .filter(count-> count != userServiceProperties.getMaxIncorrectPinAttempts())
                                                .switchIfEmpty(Mono.defer(() -> dayCache.put(blockedKey(patientId),"true").thenReturn(userServiceProperties.getMaxIncorrectPinAttempts())))
                                                .flatMap(attemptCount -> Mono.error(ClientError.transactionPinDidNotMatch(String.format("%s attempts left",userServiceProperties.getMaxIncorrectPinAttempts() - attemptCount)))))).thenReturn(newToken(patientId, scope))));
    }

    static String incorrectAttemptKey(String patientId) {
        return String.format("%s.incorrect.attempts",patientId);
    }

    static String blockedKey(String patientId) {
        return String.format("%s.blocked",patientId);
    }

    Mono<Boolean> validateRequest(UUID requestId) {
        return transactionPinRepository.getTransactionPinByRequest(requestId)
                .map(Optional::isEmpty)
                .switchIfEmpty(Mono.just(true));
    }

    @SneakyThrows
    Token newToken(String userName, String scope) {
        int minutes = userServiceProperties.getTransactionPinTokenValidity() * 60 * 1000;
        HashMap<String, Object> claims = new HashMap<>(1);
        claims.put("sid" , UUID.randomUUID().toString());
        claims.put("scope" , scope);
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
        if (!isPinValid(pin)) {
            return Mono.error(ClientError.invalidTransactionPin());
        }
        String encodedPin = encoder.encode(pin);
        return transactionPinRepository.changeTransactionPin(username, encodedPin);
    }
}
