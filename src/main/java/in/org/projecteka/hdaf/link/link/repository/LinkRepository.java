package in.org.projecteka.hdaf.link.link.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.org.projecteka.hdaf.link.link.model.PatientLinkReferenceResponse;
import in.org.projecteka.hdaf.link.link.model.PatientRepresentation;
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

        return Mono.create(monoSink -> dbClient.query(sql, handler -> {
            if (handler.failed())
                monoSink.error(new Exception("Failed to insert link reference"));
            else
                monoSink.success();
        }));
    }

    public Mono<String> getHIPIdFromDiscovery(String transactionId) {
        String sql = String.format("select hip_id from discovery_request where transaction_id='%s';", transactionId);

        return Mono.create(monoSink -> dbClient.query(sql, handler -> {
            if (handler.failed()) {
                monoSink.error(new Exception("Failed to get HIP Id from transaction Id"));
            } else {
                RowSet<Row> result = handler.result();
                monoSink.success(result.iterator().next().getString(0));
            }
        }));
    }

    public Mono<String> getTransactionIdFromLinkReference(String linkRefNumber) {
        String sql = String.format("select patient_link_reference ->> 'transactionId' as transactionId " +
                "from link_reference where patient_link_reference -> 'link' ->> 'referenceNumber' = '%s';", linkRefNumber);
        return Mono.create(monoSink -> dbClient.query(sql, handler -> {
            if (handler.failed()) {
                monoSink.error(new Exception("Failed to get transaction id from link reference"));
            } else {
                RowSet<Row> result = handler.result();
                monoSink.success(result.iterator().next().getString(0));
            }
        }));
    }

    @SneakyThrows
    public Mono<Void> insertToLink(String hipId, String consentManagerUserId, String linkRefNumber, PatientRepresentation patient) {
        String sql = String.format("insert into link (hip_id, consent_manager_use_id, link_reference, patient) " +
                        "values ('%s', '%s', '%s', '%s')",
                hipId, consentManagerUserId, linkRefNumber, new ObjectMapper().writeValueAsString(patient));
        return Mono.create(monoSink -> dbClient.query(sql, handler -> {
            if (handler.failed())
                monoSink.error(new Exception("Failed to insert link"));
            else
                monoSink.success();
        }));
    }
}
