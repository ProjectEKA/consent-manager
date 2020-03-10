package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.user.model.Gender;
import in.projecteka.consentmanager.user.model.User;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import org.apache.log4j.Logger;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.clients.ClientError.dbOperationFailed;

@AllArgsConstructor
public class UserRepository {
    private final static String INSERT_PATIENT = "Insert into patient(id, " +
            "first_name, last_name, gender, date_of_birth, phone_number)" +
            " values($1, $2, $3, $4, $5, $6);";

    private final static String SELECT_PATIENT = "select id, first_name, last_name, gender, date_of_birth, phone_number " +
            "from patient where id = $1";

    private final static Logger logger = Logger.getLogger(UserRepository.class);

    private PgPool dbClient;

    public Mono<User> userWith(String userName) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_PATIENT,
                Tuple.of(userName),
                handler -> {
                    if (handler.failed()) {
                        logger.error(handler.cause());
                        monoSink.error(dbOperationFailed());
                        return;
                    }
                    var patientIterator = handler.result().iterator();
                    if (patientIterator.hasNext()) {
                        var patientRow = patientIterator.next();
                        monoSink.success(User.builder()
                                .identifier(patientRow.getString("id"))
                                .firstName(patientRow.getString("first_name"))
                                .lastName(patientRow.getString("last_name"))
                                .dateOfBirth(patientRow.getLocalDate("date_of_birth"))
                                .gender(Gender.valueOf(patientRow.getString("gender")))
                                .phone(patientRow.getString("phone_number"))
                                .build());
                        return;
                    }
                    monoSink.success();
                }));
    }

    public Mono<Void> save(User user) {
        return Mono.create(monoSink -> dbClient.preparedQuery(INSERT_PATIENT,
                Tuple.of(user.getIdentifier(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getGender().toString(),
                        user.getDateOfBirth(),
                        user.getPhone()),
                handler -> {
                    if (handler.failed()) {
                        logger.error(handler.cause());
                        monoSink.error(dbOperationFailed());
                        return;
                    }
                    monoSink.success();
                }));
    }
}
