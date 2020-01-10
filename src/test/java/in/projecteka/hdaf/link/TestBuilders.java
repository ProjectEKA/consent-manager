package in.projecteka.hdaf.link;

import in.projecteka.hdaf.link.discovery.model.Address;
import in.projecteka.hdaf.link.discovery.model.Coding;
import in.projecteka.hdaf.link.discovery.model.Provider;
import in.projecteka.hdaf.link.discovery.model.Telecom;
import in.projecteka.hdaf.link.discovery.model.Type;
import in.projecteka.hdaf.link.discovery.model.User;
import in.projecteka.hdaf.link.discovery.model.patient.request.Identifier;
import in.projecteka.hdaf.link.discovery.model.patient.request.Patient;
import in.projecteka.hdaf.link.discovery.model.patient.request.PatientRequest;
import in.projecteka.hdaf.link.discovery.model.patient.response.CareContext;
import in.projecteka.hdaf.link.discovery.model.patient.response.DiscoveryResponse;
import in.projecteka.hdaf.link.discovery.model.patient.response.PatientResponse;
import in.projecteka.hdaf.link.link.model.ErrorRepresentation;
import in.projecteka.hdaf.link.link.model.PatientLinkReferenceResponse;
import in.projecteka.hdaf.link.link.model.PatientLinkRequest;
import in.projecteka.hdaf.link.link.model.PatientLinkResponse;
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

    public static CareContext.CareContextBuilder careContext() {
        return easyRandom.nextObject(CareContext.CareContextBuilder.class);
    }

    public static PatientRequest.PatientRequestBuilder patientRequest() {
        return easyRandom.nextObject(PatientRequest.PatientRequestBuilder.class);
    }

    public static PatientResponse.PatientResponseBuilder patientResponse() {
        return easyRandom.nextObject(PatientResponse.PatientResponseBuilder.class);
    }

    public static DiscoveryResponse.DiscoveryResponseBuilder discoveryResponse() {
        return easyRandom.nextObject(DiscoveryResponse.DiscoveryResponseBuilder.class);
    }

    public static User.UserBuilder user() {
        return easyRandom.nextObject(User.UserBuilder.class);
    }

    public static Identifier.IdentifierBuilder patientIdentifier() {
        return easyRandom.nextObject(Identifier.IdentifierBuilder.class);
    }

    public static in.projecteka.hdaf.link.discovery.model.Identifier.IdentifierBuilder providerIdentifier() {
        return easyRandom.nextObject(in.projecteka.hdaf.link.discovery.model.Identifier.IdentifierBuilder.class);
    }

    public static Patient.PatientBuilder patientInRequest() {
        return easyRandom.nextObject(Patient.PatientBuilder.class);
    }
    public static in.projecteka.hdaf.link.discovery.model.patient.response.Patient.PatientBuilder patientInResponse() {
        return easyRandom.nextObject(in.projecteka.hdaf.link.discovery.model.patient.response.Patient.PatientBuilder.class);
    }

    public static in.projecteka.hdaf.link.discovery.model.Identifier.IdentifierBuilder identifier() {
        return easyRandom.nextObject(in.projecteka.hdaf.link.discovery.model.Identifier.IdentifierBuilder.class);
    }

    public static in.projecteka.hdaf.link.link.model.hip.PatientLinkReferenceRequest.PatientLinkReferenceRequestBuilder patientLinkReferenceRequestForHIP() {
        return easyRandom.nextObject(in.projecteka.hdaf.link.link.model.hip.PatientLinkReferenceRequest.PatientLinkReferenceRequestBuilder.class);
    }

    public static in.projecteka.hdaf.link.link.model.PatientLinkReferenceRequest.PatientLinkReferenceRequestBuilder patientLinkReferenceRequest() {
        return easyRandom.nextObject(in.projecteka.hdaf.link.link.model.PatientLinkReferenceRequest.PatientLinkReferenceRequestBuilder.class);
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
