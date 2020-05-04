package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.common.DbOperationError;
import in.projecteka.consentmanager.user.model.OtpAttempt;
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
public class OtpAttemptRepository {

    private static final String INSERT_OTP_ATTEMPT = "INSERT INTO " +
            "otp_attempt (phone_number,blocked) VALUES ($1,$2)";

    private static final String SELECT_OTP_ATTEMPT = "SELECT * FROM otp_attempt " +
            "WHERE phone_number = $1 " +
            "ORDER BY timestamp DESC LIMIT $2";

    private final PgPool dbClient;

    public Mono<Void> insert(String phoneNumber, Boolean blockedStatus) {
        return Mono.create(monoSink -> dbClient.preparedQuery(INSERT_OTP_ATTEMPT)
                .execute(Tuple.of(phoneNumber, blockedStatus),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(new DbOperationError("Failed to create otp attempt"));
                                return;
                            }
                            monoSink.success();
                        }));
    }

    public Mono<List<OtpAttempt>> select(String phoneNumber, int maxOtpAttempts) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_OTP_ATTEMPT)
                .execute(Tuple.of(phoneNumber, maxOtpAttempts),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(new DbOperationError("Failed to select from otp attempt"));
                            } else {
                                monoSink.success(StreamSupport.stream(handler.result().spliterator(), false)
                                        .map(row -> new OtpAttempt(
                                                row.getString("phone_number"),
                                                row.getBoolean("blocked"),
                                                row.getLocalDateTime("timestamp")))
                                        .collect(Collectors.toList()));
                            }
                        }));
    }
}
