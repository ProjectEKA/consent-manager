package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.common.DbOperationError;
import in.projecteka.consentmanager.user.model.OtpRequestAttempt;
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
public class OtpRequestAttemptRepository {

    private static final String INSERT_OTP_REQUEST_ATTEMPT = "INSERT INTO " +
            "otp_attempt (cm_id,identifier_type, identifier_value,status,action) VALUES ($1,$2,$3,$4,$5)";

    private static final String SELECT_OTP_REQUEST_ATTEMPT = "SELECT identifier_type,identifier_value,status,attempt_at,action,cm_id FROM otp_attempt " +
            "WHERE identifier_value = $1 AND action = $3 AND cm_id = $4 AND identifier_type = $5" +
            " ORDER BY attempt_at DESC LIMIT $2";

    private final PgPool dbClient;

    public Mono<Void> insert(String cmId, String identifierType, String identifierValue, OtpRequestAttempt.AttemptStatus attemptStatus, OtpRequestAttempt.Action action) {
        return Mono.create(monoSink -> dbClient.preparedQuery(INSERT_OTP_REQUEST_ATTEMPT)
                .execute(Tuple.of(cmId, identifierType, identifierValue, attemptStatus.name(), action.toString()),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(new DbOperationError("Failed to create otp attempt"));
                                return;
                            }
                            monoSink.success();
                        }));
    }

    public Mono<List<OtpRequestAttempt>> getOtpAttempts(String cmId, String identifierType, String identifierValue, int maxOtpAttempts, OtpRequestAttempt.Action action) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_OTP_REQUEST_ATTEMPT)
                .execute(Tuple.of(identifierValue, maxOtpAttempts, action.toString(), cmId, identifierType),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(new DbOperationError("Failed to select from otp attempt"));
                            } else {
                                monoSink.success(StreamSupport.stream(handler.result().spliterator(), false)
                                        .map(row -> new OtpRequestAttempt(
                                                row.getString("identifier_type"),
                                                row.getString("identifier_value"),
                                                OtpRequestAttempt.AttemptStatus.valueOf(row.getString("status")),
                                                row.getLocalDateTime("attempt_at"),
                                                OtpRequestAttempt.Action.from(row.getString("action")),
                                                row.getString("cm_id")))
                                        .collect(Collectors.toList()));
                            }
                        }));
    }
}
