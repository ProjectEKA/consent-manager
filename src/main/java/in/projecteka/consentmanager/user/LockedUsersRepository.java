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

    private static final String UPSERT_INVALID_ATTEMPTS = "INSERT INTO " +
            "locked_users (patient_id,is_locked) VALUES ($1, $2) " +
            "ON CONFLICT (patient_id) DO " +
            "UPDATE SET invalid_attempts = locked_users.invalid_attempts + 1, " +
            "date_modified = timezone('utc'::text, now()) WHERE locked_users.patient_id = $1";

    private static final String DELETE_LOCKED_USER = "DELETE FROM " +
            "locked_users WHERE patient_id=$1";


    private static final String SELECT_LOCKED_USER = "SELECT * from " +
            "locked_users WHERE patient_id=$1";

    private final PgPool dbClient;

    public Mono<Void> upsert(String cmId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(UPSERT_INVALID_ATTEMPTS)
                .execute(Tuple.of(cmId, false),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(ClientError.failedToInsertLockedUser());
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
                .dateModified(row.getLocalDateTime("date_modified"))
                .dateCreated(row.getLocalDateTime("date_created"))
                .build();
    }
}
