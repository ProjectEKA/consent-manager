package in.projecteka.consentmanager.link.link;

import in.projecteka.consentmanager.clients.model.*;
import in.projecteka.consentmanager.link.link.model.*;
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

    public static Address.AddressBuilder address() {
        return easyRandom.nextObject(Address.AddressBuilder.class);
    }

    public static Identifier.IdentifierBuilder identifier() {
        return easyRandom.nextObject(Identifier.IdentifierBuilder.class);
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

    public static PatientLinks.PatientLinksBuilder patientLinks() {
        return easyRandom.nextObject(PatientLinks.PatientLinksBuilder.class);
    }

    public static User.UserBuilder user(){
        return easyRandom.nextObject(User.UserBuilder.class);
    }

    public static Links.LinksBuilder links(){ return easyRandom.nextObject(Links.LinksBuilder.class); }

    public static PatientRepresentation.PatientRepresentationBuilder patientRepresentation(){
        return easyRandom.nextObject(PatientRepresentation.PatientRepresentationBuilder.class);
    }
}
