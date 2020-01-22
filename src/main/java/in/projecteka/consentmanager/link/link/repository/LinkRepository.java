package in.projecteka.consentmanager.link.link.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.link.link.model.PatientLinkReferenceResponse;
import in.projecteka.consentmanager.link.link.model.PatientRepresentation;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import lombok.SneakyThrows;
import reactor.core.publisher.Mono;

public class LinkRepository {

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
                "from link_reference where patient_link_reference -> 'link' ->> 'referenceNumber' = '%s';", linkRefNumber);
        return get(sql, "Failed to get transaction id from link reference");
    }

    @SneakyThrows
    public Mono<Void> insertToLink(String hipId, String consentManagerUserId, String linkRefNumber, PatientRepresentation patient) {
        String sql = String.format("insert into link (hip_id, consent_manager_user_id, link_reference, patient) " +
                        "values ('%s', '%s', '%s', '%s')",
                hipId, consentManagerUserId, linkRefNumber, new ObjectMapper().writeValueAsString(patient));
        return insert(sql, "Failed to insert link");
    }

    public Mono<String> getExpiryFromLinkReference(String linkRefNumber) {
        String sql = String.format("select patient_link_reference -> 'link' -> 'meta' ->> 'communicationExpiry' as communicationExpiry " +
                "from link_reference where patient_link_reference -> 'link' ->> 'referenceNumber' = '%s';", linkRefNumber);
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
}
