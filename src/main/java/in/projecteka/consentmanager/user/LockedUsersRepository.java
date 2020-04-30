package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.user.model.LockedUser;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.Optional;

@AllArgsConstructor
public class LockedUsersRepository {

    private static final String INSERT_INVALID_ATTEMPTS = "INSERT INTO " +
            "locked_users (patient_id,locked_time,invalid_attempts,is_locked) VALUES ($1, $2, $3, $4)";

    private static final String UPDATE_INVALID_ATTEMPTS = "UPDATE locked_users " +
            "SET invalid_attempts=$1 WHERE patient_id=$2";

    private static final String UPDATE_LOCKED_STATUS = "UPDATE locked_users " +
            "SET is_locked=$1, locked_time=$2  WHERE patient_id=$3";

    private static final String SELECT_LOCKED_USER = "SELECT * from " +
            "locked_users WHERE patient_id=$1";

    private final PgPool dbClient;

    public Mono<Void> insert(LockedUser lockedUser) {
        return Mono.create(monoSink -> dbClient.preparedQuery(INSERT_INVALID_ATTEMPTS)
                .execute(Tuple.of(lockedUser.getPatientId(), lockedUser.getLockedTime(),
                        lockedUser.getInvalidAttempts(), lockedUser.getIsLocked()),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(ClientError.failedToCreateTransactionPin());
                                return;
                            }
                            monoSink.success();
                        }));
    }

    public Mono<Void> updateInvalidAttempts(int inValidAttempts, String patientsId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(UPDATE_INVALID_ATTEMPTS)
                .execute(Tuple.of(inValidAttempts, patientsId),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(ClientError.failedToCreateTransactionPin());
                                return;
                            }
                            monoSink.success();
                        }));
    }

    public Mono<Void> updateLockedStatus(boolean isLocked, Date lockedTime, String patientsId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(UPDATE_LOCKED_STATUS)
                .execute(Tuple.of(isLocked, lockedTime, patientsId),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(ClientError.failedToCreateTransactionPin());
                                return;
                            }
                            monoSink.success();
                        }));
    }


    public Mono<Optional<LockedUser>> getLockedUserFor(String patientId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_LOCKED_USER)
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
                            monoSink.success(lockedUserFrom(transactionPinIterator.next()));
                        }));
    }

    private Optional<LockedUser> lockedUserFrom(Row row) {
        return Optional.of(LockedUser
                .builder()
                .patientId(row.getString("patient_id"))
                .isLocked(row.getBoolean("is_locked"))
                .invalidAttempts(row.getInteger("invalid_attempts"))
                .lockedTime((Date) row.getValue("locked_time"))
                .build());
    }
}
