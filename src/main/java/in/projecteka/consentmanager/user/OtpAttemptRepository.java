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
            "otp_attempt (phone_number,blocked,action) VALUES ($1,$2,$3)";

    private static final String SELECT_OTP_ATTEMPT = "SELECT * FROM otp_attempt " +
            "WHERE phone_number = $1 AND action = $3" +
            "ORDER BY attempt_at DESC LIMIT $2";

    private final PgPool dbClient;

    public Mono<Void> insert(String phoneNumber, Boolean blockedStatus, OtpAttempt.Action action) {
        return Mono.create(monoSink -> dbClient.preparedQuery(INSERT_OTP_ATTEMPT)
                .execute(Tuple.of(phoneNumber, blockedStatus, action.name()),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(new DbOperationError("Failed to create otp attempt"));
                                return;
                            }
                            monoSink.success();
                        }));
    }

    public Mono<List<OtpAttempt>> getOtpAttempts(String phoneNumber, int maxOtpAttempts, OtpAttempt.Action action) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_OTP_ATTEMPT)
                .execute(Tuple.of(phoneNumber, maxOtpAttempts, action.name()),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(new DbOperationError("Failed to select from otp attempt"));
                            } else {
                                monoSink.success(StreamSupport.stream(handler.result().spliterator(), false)
                                        .map(row -> new OtpAttempt(
                                                row.getString("phone_number"),
                                                row.getBoolean("blocked"),
                                                row.getLocalDateTime("attempt_at"),
                                                OtpAttempt.Action.valueOf(row.getString("action"))))
                                        .collect(Collectors.toList()));
                            }
                        }));
    }
}
