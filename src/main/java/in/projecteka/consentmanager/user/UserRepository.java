package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.common.DbOperationError;
import in.projecteka.consentmanager.user.model.*;
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
    private static final String INSERT_PATIENT = "Insert into patient(health_id, " +
            "first_name, middle_name, last_name, gender, date_of_birth, month_of_birth, year_of_birth, phone_number)" +
            " values($1, $2, $3, $4, $5, $6, $7, $8, $9);";

    private static final String SELECT_PATIENT = "select id, first_name, middle_name, last_name, gender, date_of_birth, month_of_birth, year_of_birth, phone_number, unverified_identifiers " +
            "from patient where id = $1";

    private static final String SELECT_PATIENT_BY_GENDER_MOB = "select id, first_name, middle_name, last_name, date_of_birth, month_of_birth, year_of_birth, unverified_identifiers from patient" +
            " where gender = $1 and phone_number = $2";

    private final static String DELETE_PATIENT = "DELETE FROM patient WHERE id=$1";

    private final String UPDATE_CM_ID = "Update patient set id=$1 where health_id=$2";

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
                                                .first(patientRow.getString("first_name"))
                                                .middle(patientRow.getString("middle_name"))
                                                .last(patientRow.getString("last_name"))
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
                user.getName().getFirst(),
                user.getName().getMiddle(),
                user.getName().getLast(),
                user.getGender().toString(),
                user.getDateOfBirth().getDate(),
                user.getDateOfBirth().getMonth(),
                user.getDateOfBirth().getYear(),
                user.getPhone(),
                user.getUnverifiedIdentifiers());
        return doOperation(INSERT_PATIENT, userDetails);
    }

    public Mono<Void> save(HealthAccountUser user, String mobileNumber) {
        Tuple userDetails = Tuple.of(user.getHealthId(),
                user.getFirstName(),
                user.getMiddleName(),
                user.getLastName(),
                user.getGender(),
                user.getDayOfBirth(),
                user.getMonthOfBirth(),
                user.getYearOfBirth(),
                mobileNumber);
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
                                                    .first(row.getString("first_name"))
                                                    .middle(row.getString("middle_name"))
                                                    .last(row.getString("last_name"))
                                                    .build()
                                            )
                                            .dateOfBirth(DateOfBirth.builder()
                                                    .date(row.getInteger("date_of_birth"))
                                                    .month(row.getInteger("month_of_birth"))
                                                    .year(row.getInteger("year_of_birth"))
                                                    .build())
                                            .unverifiedIdentifiers((JsonArray) row.getValue("unverified_identifiers"))
                                            .gender(gender)
                                            .phone(phoneNumber)
                                            .build();
                                    userFluxSink.next(user);
                                });
                                userFluxSink.complete();
                            }
                        }));
    }

    public Mono<Void> updateCMId(String healthId, String cmId) {
        Tuple userDetails = Tuple.of(healthId,cmId);
        System.out.println("UPDATE CM_ID : came ------> \n");
        return doOperation(UPDATE_CM_ID, userDetails);
    }
}
