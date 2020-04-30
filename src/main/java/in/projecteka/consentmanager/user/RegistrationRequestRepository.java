package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.common.DbOperationError;
import in.projecteka.consentmanager.user.model.RegistrationRequest;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Repository
@AllArgsConstructor
public class RegistrationRequestRepository {

    private static final String INSERT_REGISTRATION_REQUEST = "INSERT INTO " +
            "registration_request (phone_number,blocked_status) VALUES ($1,$2)";

    private static final String SELECT_REGISTRATION_REQUEST = "SELECT * FROM registration_request " +
            "WHERE timestamp >= NOW() - INTERVAL '10 MINUTES' AND" +
            " phone_number = $1";

    private final PgPool dbClient;

    public Mono<Void> insert(String phoneNumber, Boolean blockedStatus) {
        return Mono.create(monoSink -> dbClient.preparedQuery(INSERT_REGISTRATION_REQUEST)
                .execute(Tuple.of(phoneNumber, blockedStatus),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(new DbOperationError("Failed to create registration request"));
                                return;
                            }
                            monoSink.success();
                        }));
    }

    public Mono<List<RegistrationRequest>> select(String phoneNumber) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_REGISTRATION_REQUEST)
                .execute(Tuple.of(phoneNumber),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(new DbOperationError("Failed to select from registration_request"));
                            } else {

                                monoSink.success(StreamSupport.stream(handler.result().spliterator(), false)
                                        .map(row -> new RegistrationRequest(row.getString("phone_number"),
                                                row.getBoolean("blocked_status")))
                                        .collect(Collectors.toList()));
                            }
                        }));
    }
}
