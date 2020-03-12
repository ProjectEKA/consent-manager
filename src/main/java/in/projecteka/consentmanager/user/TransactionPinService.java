package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.user.model.TransactionPin;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import reactor.core.publisher.Mono;

import java.util.Optional;

@AllArgsConstructor
public class TransactionPinService {
    private TransactionPinRepository transactionPinRepository;
    private BCryptPasswordEncoder encoder;

    public Mono<Void> createPinFor(String patientId, String pin) {
        String encodedPin = encoder.encode(pin);
        TransactionPin transactionPin = TransactionPin.builder().pin(encodedPin).patientId(patientId).build();
        return transactionPinRepository.insert(transactionPin);
    }

    public Mono<Boolean> isTransactionPinSet(String patientId) {
        return transactionPinRepository.getTransactionPinFor(patientId).map(Optional::isPresent);
    }
}
