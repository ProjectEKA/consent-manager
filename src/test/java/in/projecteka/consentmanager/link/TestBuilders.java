package in.projecteka.consentmanager.link;

import in.projecteka.consentmanager.clients.model.Identifier;
import in.projecteka.consentmanager.clients.model.Provider;
import in.projecteka.consentmanager.clients.model.Type;
import in.projecteka.consentmanager.link.link.model.ErrorRepresentation;
import in.projecteka.consentmanager.link.link.model.PatientLinkReferenceResponse;
import in.projecteka.consentmanager.link.link.model.PatientLinkRequest;
import in.projecteka.consentmanager.link.link.model.PatientLinkResponse;
import org.jeasy.random.EasyRandom;

public class TestBuilders {

    private static final EasyRandom easyRandom = new EasyRandom();

    public static Provider.ProviderBuilder provider() {
        return easyRandom.nextObject(Provider.ProviderBuilder.class);
    }

    public static Type.TypeBuilder type() {
        return easyRandom.nextObject(Type.TypeBuilder.class);
    }

    public static Identifier.IdentifierBuilder identifier() {
        return easyRandom.nextObject(Identifier.IdentifierBuilder.class);
    }

    public static in.projecteka.consentmanager.link.link.model.hip.PatientLinkReferenceRequest.PatientLinkReferenceRequestBuilder patientLinkReferenceRequestForHIP() {
        return easyRandom.nextObject(in.projecteka.consentmanager.link.link.model.hip.PatientLinkReferenceRequest.PatientLinkReferenceRequestBuilder.class);
    }

    public static in.projecteka.consentmanager.link.link.model.PatientLinkReferenceRequest.PatientLinkReferenceRequestBuilder patientLinkReferenceRequest() {
        return easyRandom.nextObject(in.projecteka.consentmanager.link.link.model.PatientLinkReferenceRequest.PatientLinkReferenceRequestBuilder.class);
    }

    public static PatientLinkRequest.PatientLinkRequestBuilder patientLinkRequest() {
        return easyRandom.nextObject(PatientLinkRequest.PatientLinkRequestBuilder.class);
    }

    public static PatientLinkReferenceResponse.PatientLinkReferenceResponseBuilder patientLinkReferenceResponse() {
        return easyRandom.nextObject(PatientLinkReferenceResponse.PatientLinkReferenceResponseBuilder.class);
    }

    public static ErrorRepresentation.ErrorRepresentationBuilder errorRepresentation() {
        return easyRandom.nextObject(ErrorRepresentation.ErrorRepresentationBuilder.class);
    }

    public static PatientLinkResponse.PatientLinkResponseBuilder patientLinkResponse() {
        return easyRandom.nextObject(PatientLinkResponse.PatientLinkResponseBuilder.class);
    }
}
