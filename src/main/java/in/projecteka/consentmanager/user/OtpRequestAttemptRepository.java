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
            "otp_attempt (session_id ,cm_id, identifier_type, identifier_value, status, action) VALUES ($1,$2,$3,$4,$5,$6)";

    private static final String SELECT_OTP_REQUEST_ATTEMPT = "SELECT session_id,identifier_type,identifier_value,status,attempt_at,action,cm_id FROM otp_attempt " +
            "WHERE identifier_value = $1 AND action = $3 AND cm_id = $4 AND identifier_type = $5" +
            " ORDER BY attempt_at DESC LIMIT $2";

    private static final String DELETE_OTP_ATTEMPT = "DELETE from otp_attempt " +
            "WHERE identifier_value = $1 AND action = $2 AND cm_id = $3 AND identifier_type = $4";

    private final PgPool dbClient;

    public Mono<Void> insert(OtpRequestAttempt otpRequestAttempt) {
        return Mono.create(monoSink -> dbClient.preparedQuery(INSERT_OTP_REQUEST_ATTEMPT)
                .execute(Tuple.of(otpRequestAttempt.getSessionId(),
                        otpRequestAttempt.getCmId(),
                        otpRequestAttempt.getIdentifierType().toUpperCase(),
                        otpRequestAttempt.getIdentifierValue(),
                        otpRequestAttempt.getAttemptStatus().name(),
                        otpRequestAttempt.getAction().toString()),
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
                .execute(Tuple.of(identifierValue, maxOtpAttempts, action.toString(), cmId, identifierType.toUpperCase()),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(new DbOperationError("Failed to select from otp attempt"));
                            } else {
                                monoSink.success(StreamSupport.stream(handler.result().spliterator(), false)
                                        .map(row -> OtpRequestAttempt.builder()
                                                .action(OtpRequestAttempt.Action.valueOf(row.getString("action")))
                                                .attemptAt(row.getLocalDateTime("attempt_at"))
                                                .attemptStatus(OtpRequestAttempt.AttemptStatus.valueOf(row.getString("status")))
                                                .cmId(row.getString("cm_id"))
                                                .identifierType(row.getString("identifier_type"))
                                                .identifierValue(row.getString("identifier_value"))
                                                .sessionId(row.getString("session_id"))
                                                .build())
                                        .collect(Collectors.toList()));
                            }
                        }));
    }

    public Mono<Void> removeAttempts(OtpRequestAttempt attempt){
        return Mono.create(monoSink -> dbClient.preparedQuery(DELETE_OTP_ATTEMPT)
                .execute(Tuple.of(attempt.getIdentifierValue(), attempt.getAction().toString(), attempt.getCmId(), attempt.getIdentifierType().toUpperCase()),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(new DbOperationError("Failed to delete from otp attempts"));
                            } else {
                                monoSink.success();
                            }
                        }));
    }
}
