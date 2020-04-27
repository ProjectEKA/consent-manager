package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.common.DbOperation;
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
    private static final String CHECK_REQUEST_ID_EXISTS = "SELECT exists(SELECT * FROM transaction_pin WHERE " +
            "request_id=$1)";
    private static final String UPDATE_REQUEST_ID = "UPDATE transaction_pin SET request_id=$1 WHERE patient_id=$2";
    private final PgPool dbClient;

    public Mono<Void> insert(TransactionPin transactionPin) {
        return Mono.create(monoSink -> dbClient.preparedQuery(INSERT_TRANSACTION_PIN)
                .execute(Tuple.of(transactionPin.getPin(), transactionPin.getPatientId()),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(ClientError.failedToCreateTransactionPin());
                                return;
                            }
                            monoSink.success();
                        }));
    }

    public Mono<Optional<TransactionPin>> getTransactionPinFor(String patientId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_TRANSACTION_PIN)
                .execute(Tuple.of(patientId),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(ClientError.failedToFetchTransactionPin());
                                return;
                            }
                            var transactionPinIterator = handler.result().iterator();
                            if (!transactionPinIterator.hasNext()) {
                                monoSink.success(Optional.empty());
                            }
                            monoSink.success(transactionPinFrom(transactionPinIterator.next()));
                        }));
    }
    public Mono<Boolean> isRequestPresent(String requestId) {
        return DbOperation.getBooleanMono(requestId, dbClient, CHECK_REQUEST_ID_EXISTS);
    }

    private Optional<TransactionPin> transactionPinFrom(Row row) {
        return Optional.of(TransactionPin
                .builder()
                .patientId(row.getString("patient_id"))
                .pin(row.getString("pin"))
                .build());
    }

    public Mono<Void> updateRequestId(String requestId, String patientId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(UPDATE_REQUEST_ID)
                .execute(Tuple.of(requestId, patientId),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(ClientError.failedToUpdateTransactionPin());
                                return;
                            }
                            monoSink.success();
                        }));
    }
}
