package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.user.model.TransactionPin;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.Optional;

@AllArgsConstructor
public class TransactionPinRepository {
    private static final String INSERT_TRANSACTION_PIN = "INSERT INTO " +
            "transaction_pin (pin, patient_id) VALUES ($1, $2)";
    private static final String SELECT_TRANSACTION_PIN = "SELECT pin, patient_id from " +
            "transaction_pin WHERE patient_id=$1";
    private PgPool dbClient;

    public Mono<Void> insert(TransactionPin transactionPin) {
        return Mono.create(monoSink -> dbClient.preparedQuery(INSERT_TRANSACTION_PIN,
                Tuple.of(transactionPin.getPin(), transactionPin.getPatientId()),
                handler -> {
                    if (handler.failed()) {
                        monoSink.error(ClientError.failedToCreateTransactionPin());
                        return;
                    }
                    monoSink.success();
                }));
    }

    public Mono<Optional<TransactionPin>> getTransactionPinFor(String patientId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_TRANSACTION_PIN,
                Tuple.of(patientId),
                handler -> {
                    if (handler.failed()) {
                        monoSink.error(ClientError.failedToFetchTransactionPin());
                        return;
                    }
                    var transactionPinIterator = handler.result().iterator();
                    if (transactionPinIterator.hasNext()) {
                        monoSink.success(transactionPinFrom(transactionPinIterator.next()));
                    }
                    monoSink.success(Optional.empty());
                }));
    }

    private Optional<TransactionPin> transactionPinFrom(Row row) {
        return Optional.of(TransactionPin
                .builder()
                .patientId(row.getString("patient_id"))
                .pin(row.getString("pin"))
                .build());
    }
}
