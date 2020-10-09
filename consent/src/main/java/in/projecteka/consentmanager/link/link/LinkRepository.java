package in.projecteka.consentmanager.link.link;

import in.projecteka.consentmanager.clients.model.CareContextRepresentation;
import in.projecteka.consentmanager.clients.model.PatientLinkReferenceResult;
import in.projecteka.consentmanager.clients.model.PatientRepresentation;
import in.projecteka.consentmanager.link.link.model.AuthzHipAction;
import in.projecteka.consentmanager.link.link.model.Hip;
import in.projecteka.consentmanager.link.link.model.Links;
import in.projecteka.consentmanager.link.link.model.PatientLinks;
import in.projecteka.consentmanager.userauth.model.RequesterType;
import in.projecteka.library.common.DbOperation;
import in.projecteka.library.common.DbOperationError;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static in.projecteka.library.clients.model.ClientError.transactionIdNotFound;
import static in.projecteka.library.common.Serializer.from;
import static in.projecteka.library.common.Serializer.to;

public class LinkRepository {

    private static final Logger logger = LoggerFactory.getLogger(LinkRepository.class);

    private static final String SELECT_LINKED_CARE_CONTEXTS = "SELECT hip_id, patient FROM link WHERE " +
            "consent_manager_user_id=$1";
    private static final String INSERT_TO_LINK = "INSERT INTO link (hip_id, consent_manager_user_id, link_reference," +
            "patient, initiated_by) VALUES ($1, $2, $3, $4, $5)";
    private static final String INSERT_TO_LINK_REFERENCE = "INSERT INTO link_reference (patient_link_reference, " +
            "hip_id, request_id) VALUES ($1, $2, $3)";
    private static final String SELECT_HIP_ID_FROM_DISCOVERY = "SELECT hip_id FROM discovery_request WHERE " +
            "transaction_id=$1";
    private static final String SELECT_TRANSACTION_ID_FROM_LINK_REFERENCE = "SELECT patient_link_reference ->> " +
            "'transactionId' as transactionId FROM link_reference WHERE patient_link_reference -> 'link' ->> " +
            "'referenceNumber' = $1";
    private static final String SELECT_LINK_REFRENCE = "SELECT patient_link_reference FROM link_reference WHERE " +
            "request_id=$1";
    private static final String SELECT_HIP_AUTHZ_ACTION = "SELECT session_id, requester_id, patient_id, purpose, expiry, repeat, current_counter, requester_type " +
            "FROM authz_hip_actions " +
            "WHERE session_id = $1 AND expiry > timezone('utc'::text, now())";
    private static final String UPDATE_HIP_ACTION_COUNTER = "UPDATE authz_hip_actions SET current_counter = current_counter + 1 " +
            "WHERE session_id=$1";
    private static final String SELECT_CARE_CONTEXTS_FOR_A_USER = "SELECT patient FROM link WHERE " +
            "consent_manager_user_id=$1";

    private final PgPool dbClient;

    public LinkRepository(PgPool dbClient) {
        this.dbClient = dbClient;
    }

    @SneakyThrows
    public Mono<Void> insert(PatientLinkReferenceResult linkReferenceResult,
                             String hipId,
                             UUID requestId) {
        return Mono.create(monoSink ->
                dbClient.preparedQuery(INSERT_TO_LINK_REFERENCE)
                        .execute(Tuple.of(new JsonObject(from(linkReferenceResult)), hipId, requestId.toString()),
                                handler -> {
                                    if (handler.failed()) {
                                        logger.error(handler.cause().getMessage(), handler.cause());
                                        monoSink.error(new DbOperationError());
                                        return;
                                    }
                                    monoSink.success();
                                }));
    }

    public Mono<String> selectLinkReference(UUID requestId) {
        return DbOperation.select(requestId, dbClient, SELECT_LINK_REFRENCE, row -> row.getString(0));
    }

    public Mono<String> getHIPIdFromDiscovery(String transactionId) {
        return getStringFrom(SELECT_HIP_ID_FROM_DISCOVERY, Tuple.of(transactionId));
    }

    public Mono<String> getTransactionIdFromLinkReference(String linkRefNumber) {
        return getStringFrom(SELECT_TRANSACTION_ID_FROM_LINK_REFERENCE, Tuple.of(linkRefNumber));
    }

    @NotNull
    private Mono<String> getStringFrom(String query, Tuple parameters) {
        return Mono.create(monoSink -> dbClient.preparedQuery(query)
                .execute(parameters,
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError());
                                return;
                            }
                            var iterator = handler.result().iterator();
                            if (!iterator.hasNext()) {
                                monoSink.error(transactionIdNotFound());
                                return;
                            }
                            monoSink.success(iterator.next().getString(0));
                        }));
    }

    public Mono<Void> insertToLink(String hipId, String consentManagerUserId, String linkRefNumber,
                                   PatientRepresentation patient, String initiatedBy) {
        return Mono.create(monoSink -> dbClient.preparedQuery(INSERT_TO_LINK)
                .execute(Tuple.of(hipId, consentManagerUserId, linkRefNumber, new JsonObject(from(patient)), initiatedBy),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError());
                                return;
                            }
                            monoSink.success();
                        }));
    }

    public Mono<PatientLinks> getLinkedCareContextsForAllHip(String patientId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_LINKED_CARE_CONTEXTS)
                .execute(Tuple.of(patientId),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError());
                                return;
                            }
                            RowSet<Row> results = handler.result();
                            List<Links> linksList = new ArrayList<>();
                            HashMap<String, Links> hipIdToLinksMap = new HashMap<>();
                            for (Row row : results) {
                                String hipId = row.getString("hip_id");
                                PatientRepresentation patientRepresentation = to(
                                        row.getValue("patient").toString(),
                                        PatientRepresentation.class);
                                if (hipIdToLinksMap.containsKey(hipId)) {
                                    Links links = hipIdToLinksMap.get(hipId);
                                    links.getPatientRepresentations().getCareContexts()
                                            .addAll(patientRepresentation.getCareContexts());
                                } else {
                                    Links links = Links.builder()
                                            .hip(Hip.builder().id(hipId).build())
                                            .patientRepresentations(patientRepresentation)
                                            .build();
                                    hipIdToLinksMap.put(hipId, links);
                                }
                            }
                            hipIdToLinksMap.forEach((key, link) -> linksList.add(link));
                            monoSink.success(PatientLinks.builder()
                                    .id(patientId)
                                    .links(linksList)
                                    .build());
                        }));
    }

    public Mono<AuthzHipAction> getAuthzHipAction(String sessionId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_HIP_AUTHZ_ACTION)
                .execute(Tuple.of(sessionId),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError());
                                return;
                            }
                            RowIterator<Row> iterator = handler.result().iterator();
                            if (!iterator.hasNext()) {
                                monoSink.success();
                                return;
                            }
                            Row result = iterator.next();
                            var hipAction = AuthzHipAction.builder()
                                    .sessionId(result.getString("session_id"))
                                    .requesterId(result.getString("requester_id"))
                                    .patientId(result.getString("patient_id"))
                                    .expiry(result.getLocalDateTime("expiry"))
                                    .purpose(result.getString("purpose"))
                                    .repeat(result.getInteger("repeat"))
                                    .currentCounter(result.getInteger("current_counter"))
                                    .requesterType(RequesterType.valueOf(result.getString("requester_type")))
                                    .build();
                            monoSink.success(hipAction);
                        }));
    }

    public Mono<Void> incrementHipActionCounter(String sessionId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(UPDATE_HIP_ACTION_COUNTER)
                .execute(Tuple.of(sessionId),
                        updateHandler -> {
                            if (updateHandler.failed()) {
                                logger.error(updateHandler.cause().getMessage(), updateHandler.cause());
                                monoSink.error(new Exception("Failed to update hip action counter. sessionId:" + sessionId));
                                return;
                            }
                            monoSink.success();
                        }));
    }

    public Mono<List<CareContextRepresentation>> getCareContextsForAUserId(String userId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_CARE_CONTEXTS_FOR_A_USER)
                .execute(Tuple.of(userId),
                        handler -> {
                            if (handler.failed()) {
                                logger.error(handler.cause().getMessage(), handler.cause());
                                monoSink.error(new DbOperationError());
                                return;
                            }
                            RowSet<Row> results = handler.result();
                            List<CareContextRepresentation> careContextsForAUser = new ArrayList<>();
                            for (Row row : results) {
                                PatientRepresentation patientRepresentation = to(
                                        row.getValue("patient").toString(),
                                        PatientRepresentation.class);
                                careContextsForAUser.addAll(patientRepresentation.getCareContexts());
                            }
                            monoSink.success(careContextsForAUser);
                        }));
    }
}
