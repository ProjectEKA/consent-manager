package in.projecteka.consentmanager.link.discovery;

import in.projecteka.consentmanager.clients.model.Address;
import in.projecteka.consentmanager.clients.model.Coding;
import in.projecteka.consentmanager.clients.model.Provider;
import in.projecteka.consentmanager.clients.model.Telecom;
import in.projecteka.consentmanager.clients.model.Type;
import in.projecteka.consentmanager.clients.model.User;
import in.projecteka.consentmanager.link.discovery.model.patient.request.Identifier;
import in.projecteka.consentmanager.link.discovery.model.patient.request.PatientIdentifier;
import in.projecteka.consentmanager.link.discovery.model.patient.request.PatientRequest;
import in.projecteka.consentmanager.link.discovery.model.patient.response.DiscoveryResponse;
import in.projecteka.consentmanager.link.discovery.model.patient.response.PatientResponse;
import org.jeasy.random.EasyRandom;

public class TestBuilders {
    public static Coding.CodingBuilder coding() {
        return easyRandom.nextObject(Coding.CodingBuilder.class);
    }

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

    public static PatientIdentifier.PatientIdentifierBuilder patientIdentifierBuilder() {
        return easyRandom.nextObject(PatientIdentifier.PatientIdentifierBuilder.class);
    }

    public static in.projecteka.consentmanager.clients.model.Identifier.IdentifierBuilder providerIdentifier() {
        return easyRandom.nextObject(in.projecteka.consentmanager.clients.model.Identifier.IdentifierBuilder.class);
    }

    public static in.projecteka.consentmanager.link.discovery.model.patient.response.Patient.PatientBuilder patientInResponse() {
        return easyRandom.nextObject(in.projecteka.consentmanager.link.discovery.model.patient.response.Patient.PatientBuilder.class);
    }

    public static in.projecteka.consentmanager.clients.model.Identifier.IdentifierBuilder identifier() {
        return easyRandom.nextObject(in.projecteka.consentmanager.clients.model.Identifier.IdentifierBuilder.class);
    }

    public static String string() {
        return easyRandom.nextObject(String.class);
    }
}
