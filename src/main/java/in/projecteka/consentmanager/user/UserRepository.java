package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.common.DbOperationError;
import in.projecteka.consentmanager.user.model.DateOfBirth;
import in.projecteka.consentmanager.user.model.Gender;
import in.projecteka.consentmanager.user.model.PatientName;
import in.projecteka.consentmanager.user.model.User;
import io.vertx.core.json.JsonArray;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);
    private static final String INSERT_PATIENT = "Insert into patient(id, " +
            "first_name, middle_name, last_name, gender, date_of_birth, month_of_birth, year_of_birth, phone_number, unverified_identifiers)" +
            " values($1, $2, $3, $4, $5, $6, $7, $8, $9, $10);";

    private static final String SELECT_PATIENT = "select id, first_name, middle_name, last_name, gender, date_of_birth, month_of_birth, year_of_birth, phone_number, unverified_identifiers" +
            "from patient where id = $1";

    private static final String SELECT_PATIENT_BY_GENDER_MOB = "select id, first_name, middle_name, last_name, date_of_birth, month_of_birth, year_of_birth, unverified_identifiers from patient" +
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
                            try {
                                var user = User.builder()
                                        .identifier(patientRow.getString("id"))
                                        .name(PatientName.builder()
                                                .fName(patientRow.getString("first_name"))
                                                .mName(patientRow.getString("middle_name"))
                                                .lName(patientRow.getString("last_name"))
                                                .build()
                                        )
                                        .dateOfBirth(DateOfBirth.builder()
                                                .date(patientRow.getInteger("date_of_birth"))
                                                .month(patientRow.getInteger("month_of_birth"))
                                                .year(patientRow.getInteger("year_of_birth"))
                                                .build())
                                        .gender(Gender.valueOf(patientRow.getString("gender")))
                                        .phone(patientRow.getString("phone_number"))
                                        .unverifiedIdentifiers((JsonArray) patientRow.getValue("unverified_identifiers"))
                                        .build();
                                monoSink.success(user);
                            } catch (Exception exc) {
                                logger.error(exc.getMessage(), exc);
                                monoSink.success();
                            }
                        }));
    }

    public Mono<Void> save(User user) {
        Tuple userDetails = Tuple.of(user.getIdentifier(),
                user.getName().getFName(),
                user.getName().getMName(),
                user.getName().getLName(),
                user.getGender().toString(),
                user.getDateOfBirth().getDate(),
                user.getDateOfBirth().getMonth(),
                user.getDateOfBirth().getYear(),
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

    public Flux<User> getUserBy(Gender gender, String phoneNumber) {
        return Flux.create(userFluxSink -> dbClient.preparedQuery(SELECT_PATIENT_BY_GENDER_MOB)
                .execute(Tuple.of(gender.toString(), phoneNumber),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                userFluxSink.error(new DbOperationError("Failed to select from patient"));
                            } else {
                                handler.result().forEach(row -> {
                                    var user = User.builder()
                                            .identifier(row.getString("id"))
                                            .name(PatientName.builder()
                                                    .fName(row.getString("first_name"))
                                                    .mName(row.getString("middle_name"))
                                                    .lName(row.getString("last_name"))
                                                    .build()
                                            )
                                            .dateOfBirth(DateOfBirth.builder()
                                                    .date(row.getInteger("date_of_birth"))
                                                    .month(row.getInteger("month_of_birth"))
                                                    .year(row.getInteger("year_of_birth"))
                                                    .build())
                                            .unverifiedIdentifiers((JsonArray) row.getValue("unverified_identifiers"))
                                            .phone(row.getString("phone_number"))
                                            .build();
                                    userFluxSink.next(user);
                                });
                                userFluxSink.complete();
                            }
                        }));
    }

}
