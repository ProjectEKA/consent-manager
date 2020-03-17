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
    private static final long JWT_TOKEN_VALIDITY = 10 * 60; // TODO: Make it configurable
    private TransactionPinRepository transactionPinRepository;
    private BCryptPasswordEncoder encoder;
    private PrivateKey privateKey;

    public Mono<Void> createPinFor(String patientId, String pin) {
        String encodedPin = encoder.encode(pin);
        TransactionPin transactionPin = TransactionPin.builder().pin(encodedPin).patientId(patientId).build();
        return validateTransactionPin(patientId).then(transactionPinRepository.insert(transactionPin));
    }

    private Mono<Void> validateTransactionPin(String patientId) {
        return isTransactionPinSet(patientId)
                .flatMap(hasTransactionPin -> hasTransactionPin
                        ? Mono.error(ClientError.transactionPinAlreadyCreated())
                        : Mono.empty());
    }

    public Mono<Boolean> isTransactionPinSet(String patientId) {
        return transactionPinRepository.getTransactionPinFor(patientId).map(Optional::isPresent);
    }

    public Mono<Token> validatePinFor(String patientId, String pin) {
        return transactionPinRepository.getTransactionPinFor(patientId)
                .flatMap(transactionPin -> {
                    if (transactionPin.isEmpty()){
                        return Mono.error(ClientError.transactionPinNotFound());
                    }

                    if(!encoder.matches(pin, transactionPin.get().getPin())) {
                        return Mono.error(ClientError.invalidTransactionPin());
                    }

                    return Mono.empty();
                }).thenReturn(newToken(patientId));
    }

    @SneakyThrows
    private Token newToken(String userName) {
        return new Token(Jwts.builder()
                .setClaims(new HashMap<>())
                .setSubject(userName)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY * 1000))
                .signWith(SignatureAlgorithm.RS512, privateKey).compact());
    }
}
