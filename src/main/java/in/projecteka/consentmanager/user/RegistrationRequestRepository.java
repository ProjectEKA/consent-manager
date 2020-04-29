package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.common.DbOperationError;
import in.projecteka.consentmanager.user.model.RegistrationRequest;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@AllArgsConstructor
public class RegistrationRequestRepository {

    private static final String INSERT_REGISTRATION_REQUEST = "INSERT INTO " +
            "registration_request (phone_number) VALUES ($1)";
    private static final String SELECT_REGISTRATION_REQUEST = "SELECT pin, patient_id from " +
            "registration_request WHERE phone_number=$1";
    private final PgPool dbClient;

    public Mono<Void> insert(RegistrationRequest registrationRequest) {
        return Mono.create(monoSink -> dbClient.preparedQuery(INSERT_REGISTRATION_REQUEST)
                .execute(Tuple.of(registrationRequest.getPhoneNumber()),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(new DbOperationError("Failed to create registration request"));
                                return;
                            }
                            monoSink.success();
                        }));
    }
}
