package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.user.model.Gender;
import in.projecteka.consentmanager.user.model.User;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.clients.ClientError.dbOperationFailed;

@AllArgsConstructor
public class UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);

    private static final String INSERT_PATIENT = "Insert into patient(id, " +
            "name, gender, year_of_birth, phone_number)" +
            " values($1, $2, $3, $4, $5);";

    private static final String SELECT_PATIENT = "select id, name, gender, year_of_birth, phone_number " +
            "from patient where id = $1";

    private final PgPool dbClient;

    public Mono<User> userWith(String userName) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_PATIENT)
                .execute(Tuple.of(userName),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(dbOperationFailed());
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
        return Mono.create(monoSink -> dbClient.preparedQuery(INSERT_PATIENT)
                .execute(Tuple.of(user.getIdentifier(),
                        user.getName(),
                        user.getGender().toString(),
                        user.getYearOfBirth(),
                        user.getPhone()),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(dbOperationFailed());
                                return;
                            }
                            monoSink.success();
                        }));
    }
}
