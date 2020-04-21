package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.common.DbOperationError;
import in.projecteka.consentmanager.user.model.Gender;
import in.projecteka.consentmanager.user.model.User;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);

    private static final String INSERT_PATIENT = "Insert into patient(id, " +
            "name, gender, year_of_birth, phone_number)" +
            " values($1, $2, $3, $4, $5);";

    private static final String SELECT_PATIENT = "select id, name, gender, year_of_birth, phone_number " +
            "from patient where id = $1";

    private final static String DELETE_PATIENT = "DELETE FROM patient WHERE id=$1";

    private PgPool dbClient;

    public Mono<User> userWith(String userName) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_PATIENT)
                .execute(Tuple.of(userName),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError());
                                return;
                            }
                            var patientIterator = handler.result().iterator();
                            if (!patientIterator.hasNext()) {
                                monoSink.success();
                                return;
                            }
                            var patientRow = patientIterator.next();
                            monoSink.success(User.builder()
                                    .identifier(patientRow.getString("id"))
                                    .name(patientRow.getString("name"))
                                    .yearOfBirth(patientRow.getInteger("year_of_birth"))
                                    .gender(Gender.valueOf(patientRow.getString("gender")))
                                    .phone(patientRow.getString("phone_number"))
                                    .build());
                        }));
    }

    public Mono<Void> save(User user) {
        Tuple userDetails = Tuple.of(user.getIdentifier(),
                user.getName(),
                user.getGender().toString(),
                user.getYearOfBirth(),
                user.getPhone());
        return doOperation(INSERT_PATIENT, userDetails);
    }

    public Mono<Void> delete(String username) {
        Tuple userDetails = Tuple.of(username);
        return doOperation(DELETE_PATIENT, userDetails);
    }

    private Mono<Void> doOperation(String query, Tuple userDetails) {
        return Mono.create(monoSink -> dbClient.preparedQuery(query)
                .execute(userDetails, handler -> {
                    if (handler.failed()) {
                        logger.error(handler.cause().getMessage(), handler.cause());
                        monoSink.error(new DbOperationError());
                        return;
                    }
                    monoSink.success();
                }));
    }
}
