package in.projecteka.consentmanager.link.link;

import in.projecteka.consentmanager.clients.model.PatientLinkReferenceResponse;
import in.projecteka.consentmanager.clients.model.PatientRepresentation;
import in.projecteka.consentmanager.link.link.model.Hip;
import in.projecteka.consentmanager.link.link.model.Links;
import in.projecteka.consentmanager.link.link.model.PatientLinks;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static in.projecteka.consentmanager.common.Serializer.from;
import static in.projecteka.consentmanager.common.Serializer.to;

public class LinkRepository {

    private static final String SELECT_LINKED_CARE_CONTEXTS = "SELECT hip_id, patient FROM link WHERE " +
            "consent_manager_user_id=$1";
    private static final String INSERT_TO_LINK = "INSERT INTO link (hip_id, consent_manager_user_id, link_reference," +
            "patient) VALUES ($1, $2, $3, $4)";
    private static final String INSERT_TO_LINK_REFERENCE = "INSERT INTO link_reference (patient_link_reference, " +
            "hip_id) VALUES ($1, $2)";
    private static final String SELECT_HIP_ID_FROM_DISCOVERY = "SELECT hip_id FROM discovery_request WHERE " +
            "transaction_id=$1";
    private static final String SELECT_TRANSACTION_ID_FROM_LINK_REFERENCE = "SELECT patient_link_reference ->> " +
            "'transactionId' as transactionId FROM link_reference WHERE patient_link_reference -> 'link' ->> " +
            "'referenceNumber' = $1";
    private static final String SELECT_EXPIRY_FROM_LINK_REFERENCE = "SELECT patient_link_reference -> 'link' -> " +
            "'meta' ->> 'communicationExpiry' as communicationExpiry FROM link_reference WHERE " +
            "patient_link_reference -> 'link' ->> 'referenceNumber' = $1";
    private PgPool dbClient;

    public LinkRepository(PgPool dbClient) {
        this.dbClient = dbClient;
    }

    @SneakyThrows
    public Mono<Void> insertToLinkReference(PatientLinkReferenceResponse patientLinkReferenceResponse, String hipId) {
        return Mono.create(monoSink ->
                dbClient.preparedQuery(
                        INSERT_TO_LINK_REFERENCE,
                        Tuple.of(new JsonObject(from(patientLinkReferenceResponse)), hipId),
                        handler -> {
                            if (handler.failed()) {
                                monoSink.error(new Exception("Failed to insert link reference"));
                                return;
                            }
                            monoSink.success();
                        }));
    }

    public Mono<String> getHIPIdFromDiscovery(String transactionId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(
                SELECT_HIP_ID_FROM_DISCOVERY,
                Tuple.of(transactionId),
                handler -> {
                    if (handler.failed()) {
                        monoSink.error(new Exception("Failed to get HIP Id from transaction Id"));
                        return;
                    }
                    monoSink.success(handler.result().iterator().next().getString(0));
                }));
    }

    public Mono<String> getTransactionIdFromLinkReference(String linkRefNumber) {
        return Mono.create(monoSink -> dbClient.preparedQuery(
                SELECT_TRANSACTION_ID_FROM_LINK_REFERENCE,
                Tuple.of(linkRefNumber),
                handler -> {
                    if (handler.failed()) {
                        monoSink.error(new Exception("Failed to get transaction id from link reference"));
                        return;
                    }
                    monoSink.success(handler.result().iterator().next().getString(0));
                }));
    }

    @SneakyThrows
    public Mono<Void> insertToLink(String hipId, String consentManagerUserId, String linkRefNumber,
                                   PatientRepresentation patient) {
        return Mono.create(monoSink -> dbClient.preparedQuery(
                INSERT_TO_LINK,
                Tuple.of(hipId, consentManagerUserId, linkRefNumber, new JsonObject(from(patient))),
                handler -> {
                    if (handler.failed()) {
                        monoSink.error(new Exception("Failed to insert link"));
                        return;
                    }
                    monoSink.success();
                }));
    }

    public Mono<String> getExpiryFromLinkReference(String linkRefNumber) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_EXPIRY_FROM_LINK_REFERENCE, Tuple.of(linkRefNumber),
                handler -> {
                    if (handler.failed()) {
                        monoSink.error(new Exception("Failed to get communicationExpiry from link reference"));
                        return;
                    }
                    monoSink.success(handler.result().iterator().next().getString(0));
                }));
    }

    public Mono<PatientLinks> getLinkedCareContextsForAllHip(String patientId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_LINKED_CARE_CONTEXTS, Tuple.of(patientId),
                handler -> {
                    if (handler.failed()) {
                        monoSink.error(new Exception("Failed to get hip_id and care contexts"));
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
                                    .hip(Hip.builder().id(hipId).name("").build())
                                    .patientRepresentations(patientRepresentation)
                                    .build();
                            hipIdToLinksMap.put(hipId, links);
                        }
                    }
                    hipIdToLinksMap.forEach((key, link) -> linksList.add(link));
                    monoSink.success(PatientLinks.builder()
                            .id(patientId)
                            .firstName("")
                            .lastName("")
                            .links(linksList)
                            .build());
                }));
    }
}
