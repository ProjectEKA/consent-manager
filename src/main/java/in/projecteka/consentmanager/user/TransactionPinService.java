package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.user.model.Token;
import in.projecteka.consentmanager.user.model.TransactionPin;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.core.publisher.Mono;

import java.security.PrivateKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;

@AllArgsConstructor
public class TransactionPinService {
    private final TransactionPinRepository transactionPinRepository;
    private final BCryptPasswordEncoder encoder;
    private final PrivateKey privateKey;
    private final UserServiceProperties userServiceProperties;

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

    public Mono<Token> validatePinFor(String patientId, String pin, String requestId) {
        return Mono.just(requestId)
                .filterWhen(this::validateRequest)
                .switchIfEmpty(Mono.error(ClientError.requestAlreadyExists()))
                .flatMap(val -> transactionPinRepository.updateRequestId(requestId, patientId)
                        .then(transactionPinRepository.getTransactionPinByPatient(patientId)
                        .flatMap(transactionPin -> {
                            if (transactionPin.isEmpty()) {
                                return Mono.error(ClientError.transactionPinNotFound());
                            }
                            if (!encoder.matches(pin, transactionPin.get().getPin())) {
                                return Mono.error(ClientError.transactionPinDidNotMatch());
                            }
                            return Mono.empty();
                        }).thenReturn(newToken(patientId))));
    }

    private Mono<Boolean> validateRequest(String requestId) {
        return transactionPinRepository.getTransactionPinByRequest(requestId)
                .map(Optional::isEmpty)
                .switchIfEmpty(Mono.just(true));
    }

    @SneakyThrows
    private Token newToken(String userName) {
        int minutes = userServiceProperties.getTransactionPinTokenValidity() * 60 * 1000;
        return new Token(Jwts.builder()
                .setClaims(new HashMap<>())
                .setSubject(userName)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + minutes))
                .signWith(SignatureAlgorithm.RS512, privateKey).compact());
    }
}
