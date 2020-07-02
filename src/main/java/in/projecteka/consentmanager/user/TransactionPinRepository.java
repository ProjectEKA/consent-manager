package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.user.model.TransactionPin;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;


@AllArgsConstructor
public class TransactionPinRepository {

    Logger logger;

    private static final String INSERT_TRANSACTION_PIN = "INSERT INTO " +
            "transaction_pin (pin, patient_id) VALUES ($1, $2)";
    private static final String SELECT_TRANSACTION_PIN_BY_PATIENT;
    private static final String SELECT_TRANSACTION_PIN_BY_REQUEST;
    private static final String UPDATE_REQUEST_ID = "UPDATE transaction_pin SET request_id=$1 WHERE patient_id=$2";
    private static final String UPDATE_TRANSACTION_PIN = "UPDATE transaction_pin SET pin=$2 WHERE patient_id=$1";
    private final PgPool dbClient;

    static {
        String s = "SELECT pin, patient_id from transaction_pin WHERE ";
        SELECT_TRANSACTION_PIN_BY_PATIENT = s + "patient_id=$1";
        SELECT_TRANSACTION_PIN_BY_REQUEST = s + "request_id=$1";
    }

    public Mono<Void> insert(TransactionPin transactionPin) {
        return Mono.create(monoSink -> dbClient.preparedQuery(INSERT_TRANSACTION_PIN)
                .execute(Tuple.of(transactionPin.getPin(), transactionPin.getPatientId()),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(ClientError.failedToCreateTransactionPin());
                                return;
                            }
                            monoSink.success();
                        }));
    }

    public Mono<Optional<TransactionPin>> getTransactionPinByPatient(String patientId) {
        return getTransactionPin(patientId, SELECT_TRANSACTION_PIN_BY_PATIENT);
    }

    public Mono<Optional<TransactionPin>> getTransactionPinByRequest(UUID requestId) {
        return getTransactionPin(requestId.toString(), SELECT_TRANSACTION_PIN_BY_REQUEST);
    }

    private Mono<Optional<TransactionPin>> getTransactionPin(String patientId, String selectTransactionPinByRequest) {
        return Mono.create(monoSink -> dbClient.preparedQuery(selectTransactionPinByRequest)
                .execute(Tuple.of(patientId),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(ClientError.failedToFetchTransactionPin());
                                return;
                            }
                            var transactionPinIterator = handler.result().iterator();
                            if (!transactionPinIterator.hasNext()) {
                                monoSink.success(Optional.empty());
                                return;
                            }
                            monoSink.success(transactionPinFrom(transactionPinIterator.next()));
                        }));
    }

    private Optional<TransactionPin> transactionPinFrom(Row row) {
        return Optional.of(TransactionPin
                .builder()
                .patientId(row.getString("patient_id"))
                .pin(row.getString("pin"))
                .build());
    }

    public Mono<Void> updateRequestId(UUID requestId, String patientId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(UPDATE_REQUEST_ID)
                .execute(Tuple.of(requestId.toString(), patientId),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(ClientError.failedToUpdateTransactionPin());
                                return;
                            }
                            monoSink.success();
                        }));
    }

    public Mono<Void> changeTransactionPin(String patientId, String pin) {
        return Mono.create(monoSink -> dbClient.preparedQuery(UPDATE_TRANSACTION_PIN)
                .execute(Tuple.of(patientId,pin),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(ClientError.failedToEditTransactionPin());
                                return;
                            }
                            monoSink.success();
                        }));
    }
}
