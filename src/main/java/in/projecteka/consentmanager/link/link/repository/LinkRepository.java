package in.projecteka.consentmanager.link.link.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.link.link.model.Hip;
import in.projecteka.consentmanager.link.link.model.Links;
import in.projecteka.consentmanager.link.link.model.PatientLinkReferenceResponse;
import in.projecteka.consentmanager.link.link.model.PatientLinks;
import in.projecteka.consentmanager.link.link.model.PatientRepresentation;
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
    private PgPool dbClient;

    public LinkRepository(PgPool dbClient) {
        this.dbClient = dbClient;
    }

    @SneakyThrows
    public Mono<Void> insertToLinkReference(PatientLinkReferenceResponse patientLinkReferenceResponse, String hipId) {
        String sql = String.format("insert into link_reference (patient_link_reference, hip_id) values ('%s', '%s')",
                new ObjectMapper().writeValueAsString(patientLinkReferenceResponse), hipId);
        return insert(sql, "Failed to insert link reference");
    }

    public Mono<String> getHIPIdFromDiscovery(String transactionId) {
        String sql = String.format("select hip_id from discovery_request where transaction_id='%s';", transactionId);
        return get(sql, "Failed to get HIP Id from transaction Id");
    }

    public Mono<String> getTransactionIdFromLinkReference(String linkRefNumber) {
        String sql = String.format("select patient_link_reference ->> 'transactionId' as transactionId " +
                        "from link_reference where patient_link_reference -> 'link' ->> 'referenceNumber' = '%s';",
                linkRefNumber);
        return get(sql, "Failed to get transaction id from link reference");
    }

    @SneakyThrows
    public Mono<Void> insertToLink(String hipId, String consentManagerUserId, String linkRefNumber,
                                   PatientRepresentation patient) {
        final String patientAsString = new ObjectMapper().writeValueAsString(patient);
        return Mono.create(monoSink ->
                dbClient.preparedQuery(
                        INSERT_TO_LINK,
                        Tuple.of(hipId, consentManagerUserId, linkRefNumber, patientAsString),
                        handler -> {
                            if (handler.failed())
                                monoSink.error(new Exception("Failed to insert link"));
                            else
                                monoSink.success();
                        })
        );
    }

    public Mono<String> getExpiryFromLinkReference(String linkRefNumber) {
        String sql = String.format("select patient_link_reference -> 'link' -> 'meta' ->> 'communicationExpiry' as " +
                        "communicationExpiry " +
                        "from link_reference where patient_link_reference -> 'link' ->> 'referenceNumber' = '%s';",
                linkRefNumber);
        return get(sql, "Failed to get communicationExpiry from link reference");
    }

    private Mono<Void> insert(String sql, String msg) {
        return Mono.create(monoSink -> dbClient.query(sql, handler -> {
            if (handler.failed())
                monoSink.error(new Exception(msg));
            else
                monoSink.success();
        }));
    }

    private Mono<String> get(String sql, String msg) {
        return Mono.create(monoSink -> dbClient.query(sql, handler -> {
            if (handler.failed()) {
                monoSink.error(new Exception(msg));
            } else {
                RowSet<Row> result = handler.result();
                monoSink.success(result.iterator().next().getString(0));
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
                                    convertToPatientRepresentation(row.getString("patient"));
                            if (hipIdToLinksMap.containsKey(hipId)){
                                Links links = hipIdToLinksMap.get(hipId);
                                links.getPatientRepresentations().getCareContexts().addAll(patientRepresentation.getCareContexts());
                            } else {
                                Links links = Links.builder()
                                        .hip(Hip.builder().id(hipId).name("").build())
                                        .patientRepresentations(patientRepresentation)
                                        .build();
                                hipIdToLinksMap.put(hipId, links);
                            }
                        }
                        hipIdToLinksMap.forEach((key, link) -> {
                            linksList.add(link);
                        });
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
