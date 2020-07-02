package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.common.DbOperationError;
import in.projecteka.consentmanager.user.model.OtpAttempt;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;


@Repository
@AllArgsConstructor
public class OtpAttemptRepository {

    private final Logger logger = LoggerFactory.getLogger(OtpAttemptRepository.class);


    private static final String INSERT_OTP_ATTEMPT = "INSERT INTO " +
            "otp_attempt (session_id ,cm_id, identifier_type, identifier_value, status, action) VALUES ($1,$2,$3,$4,$5,$6)";

    private static final String SELECT_OTP_ATTEMPT = "SELECT session_id,identifier_type,identifier_value,status,attempt_at,action,cm_id FROM otp_attempt " +
            "WHERE identifier_value = $1 AND action = $3 AND cm_id = $4 AND identifier_type = $5" +
            " ORDER BY attempt_at DESC LIMIT $2";

    private static final String DELETE_OTP_ATTEMPT = "DELETE from otp_attempt " +
            "WHERE identifier_value = $1 AND action = $2 AND cm_id = $3 AND identifier_type = $4";

    private final PgPool dbClient;

    public Mono<Void> insert(OtpAttempt otpAttempt) {
        return Mono.create(monoSink -> dbClient.preparedQuery(INSERT_OTP_ATTEMPT)
                .execute(Tuple.of(otpAttempt.getSessionId(),
                        otpAttempt.getCmId(),
                        otpAttempt.getIdentifierType(),
                        otpAttempt.getIdentifierValue(),
                        otpAttempt.getAttemptStatus().name(),
                        otpAttempt.getAction().toString()),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError("Failed to create otp attempt"));
                                return;
                            }
                            monoSink.success();
                        }));
    }

    public Mono<List<OtpAttempt>> getOtpAttempts(OtpAttempt attempt, int maxOtpAttempts) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_OTP_ATTEMPT)
                .execute(Tuple.of(attempt.getIdentifierValue(), maxOtpAttempts, attempt.getAction().toString(), attempt.getCmId(), attempt.getIdentifierType()),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError("Failed to select from otp attempt"));
                            } else {
                                List<OtpAttempt> attempts = new ArrayList<>();
                                handler.result().forEach(row -> {
                                    attempts.add(otpAttemptFromRow(row));
                                });
                                monoSink.success(attempts);
                            }
                        }));
    }

    public Mono<Void> removeAttempts(OtpAttempt attempt){
        return Mono.create(monoSink -> dbClient.preparedQuery(DELETE_OTP_ATTEMPT)
                .execute(Tuple.of(attempt.getIdentifierValue(), attempt.getAction().toString(), attempt.getCmId(), attempt.getIdentifierType()),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError("Failed to delete from otp attempts"));
                            } else {
                                monoSink.success();
                            }
                        }));
    }

    private OtpAttempt otpAttemptFromRow(Row row){
        return OtpAttempt.builder()
                .action(OtpAttempt.Action.valueOf(row.getString("action")))
                .attemptAt(row.getLocalDateTime("attempt_at"))
                .attemptStatus(OtpAttempt.AttemptStatus.valueOf(row.getString("status")))
                .cmId(row.getString("cm_id"))
                .identifierType(row.getString("identifier_type"))
                .identifierValue(row.getString("identifier_value"))
                .sessionId(row.getString("session_id"))
                .build();
    }
}
