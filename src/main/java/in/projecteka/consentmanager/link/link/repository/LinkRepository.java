package in.projecteka.consentmanager.link.link.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.link.link.model.Hip;
import in.projecteka.consentmanager.link.link.model.Links;
import in.projecteka.consentmanager.link.link.model.PatientLinkReferenceResponse;
import in.projecteka.consentmanager.link.link.model.PatientLinks;
import in.projecteka.consentmanager.link.link.model.PatientRepresentation;
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
        JsonObject patientLinkReferenceResponseJson =
                new JsonObject(new ObjectMapper().writeValueAsString(patientLinkReferenceResponse));
        return Mono.create(monoSink ->
                dbClient.preparedQuery(
                        INSERT_TO_LINK_REFERENCE,
                        Tuple.of(patientLinkReferenceResponseJson, hipId),
                        handler -> {
                            if (handler.failed())
                                monoSink.error(new Exception("Failed to insert link reference"));
                            else
                                monoSink.success();
                        })
        );
    }

    public Mono<String> getHIPIdFromDiscovery(String transactionId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_HIP_ID_FROM_DISCOVERY, Tuple.of(transactionId),
                handler -> {
                    if (handler.failed()) {
                        monoSink.error(new Exception("Failed to get HIP Id from transaction Id"));
                    } else {
                        monoSink.success(handler.result().iterator().next().getString(0));
                    }
                }));
    }

    public Mono<String> getTransactionIdFromLinkReference(String linkRefNumber) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_TRANSACTION_ID_FROM_LINK_REFERENCE, Tuple.of(linkRefNumber),
                handler -> {
                    if (handler.failed()) {
                        monoSink.error(new Exception("Failed to get transaction id from link reference"));
                    } else {
                        monoSink.success(handler.result().iterator().next().getString(0));
                    }
                }));
    }

    @SneakyThrows
    public Mono<Void> insertToLink(String hipId, String consentManagerUserId, String linkRefNumber,
                                   PatientRepresentation patient) {
        JsonObject patientJson = new JsonObject(new ObjectMapper().writeValueAsString(patient));
        return Mono.create(monoSink ->
                dbClient.preparedQuery(
                        INSERT_TO_LINK, Tuple.of(hipId, consentManagerUserId, linkRefNumber, patientJson),
                        handler -> {
                            if (handler.failed())
                                monoSink.error(new Exception("Failed to insert link"));
                            else
                                monoSink.success();
                        })
        );
    }

    public Mono<String> getExpiryFromLinkReference(String linkRefNumber) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_EXPIRY_FROM_LINK_REFERENCE, Tuple.of(linkRefNumber),
                handler -> {
                    if (handler.failed()) {
                        monoSink.error(new Exception("Failed to get communicationExpiry from link reference"));
                    } else {
                        monoSink.success(handler.result().iterator().next().getString(0));
                    }
                }));
    }

    public Mono<PatientLinks> getLinkedCareContextsForAllHip(String patientId) {
        return Mono.create(monoSink -> dbClient.preparedQuery(SELECT_LINKED_CARE_CONTEXTS, Tuple.of(patientId),
                handler -> {
                    if (handler.failed()) {
                        monoSink.error(new Exception("Failed to get hip_id and care contexts"));
                    } else {
                        RowSet<Row> results = handler.result();
                        List<Links> linksList = new ArrayList<>();
                        HashMap<String,Links> hipIdToLinksMap = new HashMap<>();
                        for (Row row : results) {
                            String hipId = row.getString("hip_id");
                            PatientRepresentation patientRepresentation =
                                    convertToPatientRepresentation(row.getValue("patient").toString());
                            if (hipIdToLinksMap.containsKey(hipId)){
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
                        monoSink.success(
                                PatientLinks.builder().id(patientId).firstName("").lastName("").links(linksList).build());
                    }
                }));
    }

    @SneakyThrows
    private PatientRepresentation convertToPatientRepresentation(String patient) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(patient.getBytes(), PatientRepresentation.class);
    }
}
