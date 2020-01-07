package in.org.projecteka.hdaf.link.discovery;

import in.org.projecteka.hdaf.link.discovery.model.*;
import in.org.projecteka.hdaf.link.discovery.model.patient.request.Identifier;
import in.org.projecteka.hdaf.link.discovery.model.patient.request.Patient;
import in.org.projecteka.hdaf.link.discovery.model.patient.request.PatientRequest;
import in.org.projecteka.hdaf.link.discovery.model.patient.response.PatientResponse;
import org.jeasy.random.EasyRandom;

public class TestBuilders {

    private static final EasyRandom easyRandom = new EasyRandom();

    static Telecom.TelecomBuilder telecom() {
        return easyRandom.nextObject(Telecom.TelecomBuilder.class);
    }

    static Provider.ProviderBuilder provider() {
        return easyRandom.nextObject(Provider.ProviderBuilder.class);
    }

    static Type.TypeBuilder type() {
        return easyRandom.nextObject(Type.TypeBuilder.class);
    }

    static Coding.CodingBuilder coding() {
        return easyRandom.nextObject(Coding.CodingBuilder.class);
    }

    static Address.AddressBuilder address() {
        return easyRandom.nextObject(Address.AddressBuilder.class);
    }

    static PatientRequest.PatientRequestBuilder patientRequest() {
        return easyRandom.nextObject(PatientRequest.PatientRequestBuilder.class);
    }

    static PatientResponse.PatientResponseBuilder patientResponse() {
        return easyRandom.nextObject(PatientResponse.PatientResponseBuilder.class);
    }

    static User.UserBuilder user() {
        return easyRandom.nextObject(User.UserBuilder.class);
    }

    static Identifier.IdentifierBuilder patientIdentifier() {
        return easyRandom.nextObject(Identifier.IdentifierBuilder.class);
    }

    static in.org.projecteka.hdaf.link.discovery.model.Identifier.IdentifierBuilder providerIdentifier() {
        return easyRandom.nextObject(in.org.projecteka.hdaf.link.discovery.model.Identifier.IdentifierBuilder.class);
    }

    static Patient.PatientBuilder patient() {
        return easyRandom.nextObject(Patient.PatientBuilder.class);
    }

}
