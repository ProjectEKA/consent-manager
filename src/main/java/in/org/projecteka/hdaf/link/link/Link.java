package in.org.projecteka.hdaf.link.link;

import in.org.projecteka.hdaf.link.ClientError;
import in.org.projecteka.hdaf.link.ClientRegistryClient;
import in.org.projecteka.hdaf.link.HIPClient;
import in.org.projecteka.hdaf.link.link.model.ErrorCode;
import in.org.projecteka.hdaf.link.link.model.PatientLinkResponse;
import in.org.projecteka.hdaf.link.link.model.Error;
import in.org.projecteka.hdaf.link.link.model.PatientLinkReferenceRequest;
import in.org.projecteka.hdaf.link.link.model.ErrorRepresentation;
import in.org.projecteka.hdaf.link.link.model.PatientLinkRequest;
import in.org.projecteka.hdaf.link.link.model.PatientLinkReferenceResponse;
import in.org.projecteka.hdaf.link.link.model.hip.Patient;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import static in.org.projecteka.hdaf.link.link.Transformer.toHIPPatient;

public class Link {

    private final HIPClient hipClient;
    private final ClientRegistryClient clientRegistryClient;

    public Link(HIPClient hipClient, ClientRegistryClient clientRegistryClient) {
        this.hipClient = hipClient;
        this.clientRegistryClient = clientRegistryClient;
    }

    public Mono<PatientLinkReferenceResponse> patientWith(String patientId, PatientLinkReferenceRequest patientLinkReferenceRequest) {
        //providerid to be fetched from DB using transactionID
        String providerId = "10000005";
        Patient patient = toHIPPatient(patientId, patientLinkReferenceRequest);
        var linkReferenceRequest = new in.org.projecteka.hdaf.link.link.model.hip.PatientLinkReferenceRequest(
                patientLinkReferenceRequest.getTransactionId(),
                patient);
        return providerUrl(providerId)
                .flatMap(url -> hipClient.linkPatientCareContext(linkReferenceRequest, url))
                .switchIfEmpty(Mono.error(new ClientError(
                        HttpStatus.NOT_FOUND,
                        new ErrorRepresentation(new Error(
                                ErrorCode.NotHIPFound,
                                "No HIP found with given transaction ID")))));
    }

    public Mono<PatientLinkResponse> verifyToken(String linkRefNumber, PatientLinkRequest patientLinkRequest) {
        //from linkRefNumber get TransactionId
        //from transactionID get providerID
        String providerId = "10000005";
        //Check otp for expiry
        return providerUrl(providerId)
                .flatMap(url -> hipClient.validateToken(linkRefNumber, patientLinkRequest, url))
                .switchIfEmpty(Mono.error(new ClientError(
                        HttpStatus.NOT_FOUND,
                        new ErrorRepresentation(new Error(
                                ErrorCode.NotHIPFound,
                                "No HIP found with given link reference number")))));
    }

    private Mono<String> providerUrl(String providerId) {
        return clientRegistryClient.providerWith(providerId)
                .flatMap(provider -> provider.getIdentifiers()
                        .stream()
                        .findFirst()
                        .map(identifier -> Mono.just(identifier.getSystem()))
                        .orElse(Mono.empty()));
    }
}
