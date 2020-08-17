package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.consent.model.ConsentArtefact;
import in.projecteka.consentmanager.consent.model.ConsentStatus;
import in.projecteka.consentmanager.consent.model.HIPConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.model.Query;
import in.projecteka.consentmanager.consent.model.QueryRepresentation;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static in.projecteka.library.common.Serializer.from;

@AllArgsConstructor
public class ConsentArtefactQueryGenerator {
    private static final String INSERT_CONSENT_ARTEFACT_QUERY = "INSERT INTO consent_artefact" +
            " (consent_request_id, consent_artefact_id, patient_id, consent_artefact, signature, status) VALUES" +
            " ($1, $2, $3, $4, $5, $6)";
    private static final String INSERT_HIP_CONSENT_ARTEFACT_QUERY = "INSERT INTO hip_consent_artefact" +
            " (consent_request_id, consent_artefact_id, patient_id, consent_artefact, signature, status) VALUES" +
            " ($1, $2, $3, $4, $5, $6)";
    private static final String UPDATE_CONSENT_REQUEST_STATUS_QUERY = "UPDATE consent_request SET status=$1, " +
            "date_modified=$2 WHERE request_id=$3";

    public Mono<QueryRepresentation> toQueries(String requestId,
                                               String patientId,
                                               ConsentArtefact consentArtefact,
                                               HIPConsentArtefactRepresentation hipConsentArtefact,
                                               String consentArtefactSignature) {
        Query insertCA = new Query(INSERT_CONSENT_ARTEFACT_QUERY,
                Tuple.of(requestId,
                        consentArtefact.getConsentId(),
                        patientId,
                        new JsonObject(from(consentArtefact)),
                        consentArtefactSignature,
                        ConsentStatus.GRANTED.toString()));
        Query insertHIPCA = new Query(INSERT_HIP_CONSENT_ARTEFACT_QUERY,
                Tuple.of(requestId,
                        hipConsentArtefact.getConsentDetail().getConsentId(),
                        patientId,
                        new JsonObject(from(hipConsentArtefact.getConsentDetail())),
                        consentArtefactSignature,
                        ConsentStatus.GRANTED.toString()));
        Query updateConsentReqStatus = new Query(UPDATE_CONSENT_REQUEST_STATUS_QUERY,
                Tuple.of(ConsentStatus.GRANTED.toString(),
                        LocalDateTime.now(ZoneOffset.UTC),
                        requestId));
        return Mono.just(QueryRepresentation.builder()
                .queries(List.of(insertCA, insertHIPCA, updateConsentReqStatus))
                .hipConsentArtefactRepresentations(List.of(hipConsentArtefact))
                .build());
    }
}
