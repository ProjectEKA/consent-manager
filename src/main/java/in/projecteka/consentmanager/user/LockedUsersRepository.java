package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.user.model.LockedUser;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class LockedUsersRepository {

    private static final String INSERT_INVALID_ATTEMPTS = "INSERT INTO " +
            "locked_users (patient_id,locked_time,invalid_attempts,is_locked, first_invalid_attempt_time) VALUES ($1, $2, $3, $4, $5)";

    private static final String DELETE_LOCKED_USER = "DELETE FROM " +
            "locked_users WHERE patient_id=$1";

    private static final String UPDATE_LOCKED_USER = "UPDATE locked_users " +
            "SET is_locked=$1, locked_time=$2, invalid_attempts=$3 WHERE patient_id=$4";

    private static final String SELECT_LOCKED_USER = "SELECT * from " +
            "locked_users WHERE patient_id=$1";

    private final PgPool dbClient;

    public Mono<Void> insert(LockedUser lockedUser) {
        return Mono.create(monoSink -> dbClient.preparedQuery(INSERT_INVALID_ATTEMPTS)
                .execute(Tuple.of(lockedUser.getPatientId(), lockedUser.getLockedTime(),
                        lockedUser.getInvalidAttempts(), lockedUser.getIsLocked(), lockedUser.getFirstInvalidAttemptTime()),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(ClientError.failedToInsertLockedUser());
                                return;
                            }
                            monoSink.success();
                        }));
    }

    public Mono<Void> updateUser(boolean isLocked, String lockedTime, String patientsId, int invalidAttempts) {
        return Mono.create(monoSink -> dbClient.preparedQuery(UPDATE_LOCKED_USER)
                .execute(Tuple.of(isLocked, lockedTime, invalidAttempts, patientsId),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(ClientError.failedToUpdateLockedUser());
                                return;
                            }
                            monoSink.success();
                        }));
    }

    public Mono<Void> deleteUser(String patientsId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(DELETE_LOCKED_USER)
                .execute(Tuple.of(patientsId),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(ClientError.failedToUpdateLockedUser());
                                return;
                            }
                            monoSink.success();
                        }));
    }


    public Mono<LockedUser> getLockedUserFor(String patientId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_LOCKED_USER)
                .execute(Tuple.of(patientId),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(ClientError.failedToFetchLockedUser());
                                return;
                            }
                            var lockedUserIterator = handler.result().iterator();
                            if (!lockedUserIterator.hasNext()) {
                                monoSink.success();
                            }
                            monoSink.success(lockedUserFrom(lockedUserIterator.next()));
                        }));
    }

    private LockedUser lockedUserFrom(Row row) {
        return LockedUser
                .builder()
                .patientId(row.getString("patient_id"))
                .isLocked(row.getBoolean("is_locked"))
                .invalidAttempts((Integer) row.getValue("invalid_attempts"))
                .lockedTime(row.getString("locked_time"))
                .firstInvalidAttemptTime(row.getString("first_invalid_attempt_time"))
                .build();
    }
}