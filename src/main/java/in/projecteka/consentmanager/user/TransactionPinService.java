package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
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
        return transactionPinRepository.getTransactionPinFor(patientId).map(Optional::isPresent);
    }

    public Mono<Token> validatePinFor(String patientId, String pin,String scope) {
        return transactionPinRepository.getTransactionPinFor(patientId)
                .flatMap(transactionPin -> {
                    if (transactionPin.isEmpty()) {
                        return Mono.error(ClientError.transactionPinNotFound());
                    }

                    if (!encoder.matches(pin, transactionPin.get().getPin())) {
                        return Mono.error(ClientError.transactionPinDidNotMatch());
                    }
                    return Mono.empty();
                }).thenReturn(newToken(patientId, scope));
    }

    @SneakyThrows
    private Token newToken(String userName, String scope) {
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
}
