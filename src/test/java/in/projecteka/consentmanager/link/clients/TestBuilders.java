package in.projecteka.consentmanager.link.clients;

import in.projecteka.consentmanager.link.discovery.model.Address;
import in.projecteka.consentmanager.link.discovery.model.Coding;
import in.projecteka.consentmanager.clients.model.Provider;
import in.projecteka.consentmanager.link.discovery.model.Telecom;
import in.projecteka.consentmanager.link.discovery.model.Type;
import in.projecteka.consentmanager.clients.model.User;
import in.projecteka.consentmanager.link.discovery.model.patient.request.Patient;
import in.projecteka.consentmanager.link.discovery.model.patient.request.PatientRequest;
import in.projecteka.consentmanager.link.discovery.model.patient.response.CareContext;
import in.projecteka.consentmanager.link.discovery.model.patient.response.PatientResponse;
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

    public static PatientRequest.PatientRequestBuilder patientRequest() {
        return easyRandom.nextObject(PatientRequest.PatientRequestBuilder.class);
    }
    public static User.UserBuilder user() {
        return easyRandom.nextObject(User.UserBuilder.class);
    }

    public static PatientResponse.PatientResponseBuilder patientResponse() {
        return easyRandom.nextObject(PatientResponse.PatientResponseBuilder.class);
    }

    public static CareContext.CareContextBuilder careContext() {
        return easyRandom.nextObject(CareContext.CareContextBuilder.class);
    }
    public static Patient.PatientBuilder patientInRequest() {
        return easyRandom.nextObject(Patient.PatientBuilder.class);
    }

    public static in.projecteka.consentmanager.link.discovery.model.patient.response.Patient.PatientBuilder patientInResponse() {
        return easyRandom.nextObject(in.projecteka.consentmanager.link.discovery.model.patient.response.Patient.PatientBuilder.class);
    }

    public static in.projecteka.consentmanager.link.discovery.model.Identifier.IdentifierBuilder identifier() {
        return easyRandom.nextObject(in.projecteka.consentmanager.link.discovery.model.Identifier.IdentifierBuilder.class);
    }
}
