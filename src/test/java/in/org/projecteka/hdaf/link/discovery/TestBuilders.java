package in.org.projecteka.hdaf.link.discovery;

import in.org.projecteka.hdaf.link.discovery.model.Address;
import in.org.projecteka.hdaf.link.discovery.model.Provider;
import in.org.projecteka.hdaf.link.discovery.model.Telecom;
import in.org.projecteka.hdaf.link.discovery.model.Type;
import in.org.projecteka.hdaf.link.discovery.model.User;
import in.org.projecteka.hdaf.link.discovery.model.patient.request.Identifier;
import in.org.projecteka.hdaf.link.discovery.model.patient.request.PatientRequest;
import in.org.projecteka.hdaf.link.discovery.model.patient.response.DiscoveryResponse;
import in.org.projecteka.hdaf.link.discovery.model.patient.response.PatientResponse;
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

    public static in.org.projecteka.hdaf.link.discovery.model.Identifier.IdentifierBuilder providerIdentifier() {
        return easyRandom.nextObject(in.org.projecteka.hdaf.link.discovery.model.Identifier.IdentifierBuilder.class);
    }

    public static in.org.projecteka.hdaf.link.discovery.model.patient.response.Patient.PatientBuilder patientInResponse() {
        return easyRandom.nextObject(in.org.projecteka.hdaf.link.discovery.model.patient.response.Patient.PatientBuilder.class);
    }

    public static in.org.projecteka.hdaf.link.discovery.model.Identifier.IdentifierBuilder identifier() {
        return easyRandom.nextObject(in.org.projecteka.hdaf.link.discovery.model.Identifier.IdentifierBuilder.class);
    }
}
