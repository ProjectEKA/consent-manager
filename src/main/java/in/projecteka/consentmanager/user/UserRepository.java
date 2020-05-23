package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.common.DbOperationError;
import in.projecteka.consentmanager.user.model.Gender;
import in.projecteka.consentmanager.user.model.User;
import io.vertx.core.json.JsonArray;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@AllArgsConstructor
public class UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);
    private static final String INSERT_PATIENT = "Insert into patient(id, " +
            "name, gender, year_of_birth, phone_number, unverified_identifiers)" +
            " values($1, $2, $3, $4, $5, $6);";

    private static final String SELECT_PATIENT = "select id, name, gender, year_of_birth, phone_number, unverified_identifiers " +
            "from patient where id = $1";

    private static final String SELECT_PATIENT_BY_DETAILS = "select id, year_of_birth, unverified_identifiers, name from patient" +
            " where gender = $1 and phone_number = $2";

    private final static String DELETE_PATIENT = "DELETE FROM patient WHERE id=$1";

    private final PgPool dbClient;

    public Mono<User> userWith(String userName) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_PATIENT)
                .execute(Tuple.of(userName.toLowerCase()),
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
                                    .unverifiedIdentifiers((JsonArray) patientRow.getValue("unverified_identifiers"))
                                    .build());
                        }));
    }

    public Mono<Void> save(User user) {
        Tuple userDetails = Tuple.of(user.getIdentifier(),
                user.getName(),
                user.getGender().toString(),
                user.getYearOfBirth(),
                user.getPhone(),
                user.getUnverifiedIdentifiers());
        return doOperation(INSERT_PATIENT, userDetails);
    }

    public Mono<Void> delete(String username) {
        Tuple userDetails = Tuple.of(username);
        return doOperation(DELETE_PATIENT, userDetails);
    }

    private Mono<Void> doOperation(String query, Tuple parameters) {
        return Mono.create(monoSink -> dbClient.preparedQuery(query)
                .execute(parameters, handler -> {
                    if (handler.failed()) {
                        logger.error(handler.cause().getMessage(), handler.cause());
                        monoSink.error(new DbOperationError());
                        return;
                    }
                    monoSink.success();
                }));
    }

    public Mono<List<User>> getCmIdBy(Gender gender, String phoneNumber) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_PATIENT_BY_DETAILS)
                .execute(Tuple.of(gender.toString(), phoneNumber),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(new DbOperationError("Failed to select from patient"));
                            } else {
                                monoSink.success(StreamSupport.stream(handler.result().spliterator(), false)
                                        .map(row -> User.builder() //return list of users
                                                .identifier(row.getString("id"))
                                                .name(row.getString("name"))
                                                .yearOfBirth(row.getInteger("year_of_birth"))
                                                .unverifiedIdentifiers((JsonArray) row.getValue("unverified_identifiers"))
                                                .build())
                                        .collect(Collectors.toList()));
                            }
                        }));
    }

}
