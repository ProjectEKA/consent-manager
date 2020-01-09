package in.org.projecteka.hdaf.link;

import in.org.projecteka.hdaf.link.discovery.model.*;
import in.org.projecteka.hdaf.link.link.model.ErrorRepresentation;
import in.org.projecteka.hdaf.link.link.model.PatientLinkReferenceResponse;
import in.org.projecteka.hdaf.link.link.model.PatientLinkRequest;
import in.org.projecteka.hdaf.link.link.model.PatientLinkResponse;
import org.jeasy.random.EasyRandom;

public class TestBuilders {

    private static final EasyRandom easyRandom = new EasyRandom();

    public static Telecom.TelecomBuilder telecom() {
        return easyRandom.nextObject(Telecom.TelecomBuilder.class);
    }

    public static Provider.ProviderBuilder provider() {
        return easyRandom.nextObject(Provider.ProviderBuilder.class);
    }

    public static Type.TypeBuilder type() {
        return easyRandom.nextObject(Type.TypeBuilder.class);
    }

    public static Coding.CodingBuilder coding() {
        return easyRandom.nextObject(Coding.CodingBuilder.class);
    }

    public static Address.AddressBuilder address() {
        return easyRandom.nextObject(Address.AddressBuilder.class);
    }

    public static Identifier.IdentifierBuilder identifier() {
        return easyRandom.nextObject(Identifier.IdentifierBuilder.class);
    }

    public static in.org.projecteka.hdaf.link.link.model.hip.PatientLinkReferenceRequest.PatientLinkReferenceRequestBuilder patientLinkReferenceRequestForHIP() {
        return easyRandom.nextObject(in.org.projecteka.hdaf.link.link.model.hip.PatientLinkReferenceRequest.PatientLinkReferenceRequestBuilder.class);
    }

    public static in.org.projecteka.hdaf.link.link.model.PatientLinkReferenceRequest.PatientLinkReferenceRequestBuilder patientLinkReferenceRequest() {
        return easyRandom.nextObject(in.org.projecteka.hdaf.link.link.model.PatientLinkReferenceRequest.PatientLinkReferenceRequestBuilder.class);
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
