package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.user.model.TransactionPin;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.Optional;

@AllArgsConstructor
public class TransactionPinService {
    private TransactionPinRepository transactionPinRepository;

    public Mono<Void> createPinFor(String patientId, String pin) {
        // TODO : Encrypt pin and store
        TransactionPin transactionPin = TransactionPin.builder().pin(pin).patientId(patientId).build();
        return transactionPinRepository.insert(transactionPin);
    }

    public Mono<Boolean> isTransactionPinSet(String patientId) {
        return transactionPinRepository.getTransactionPinFor(patientId).map(Optional::isPresent);
    }
}
