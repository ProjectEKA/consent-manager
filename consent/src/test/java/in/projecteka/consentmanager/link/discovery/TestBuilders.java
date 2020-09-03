package in.projecteka.consentmanager.link.discovery;

import in.projecteka.consentmanager.clients.model.CareContextRepresentation;
import in.projecteka.consentmanager.link.discovery.model.patient.request.PatientIdentifier;
import in.projecteka.consentmanager.link.discovery.model.patient.response.DiscoveryResponse;
import in.projecteka.library.clients.model.User;
import org.jeasy.random.EasyRandom;

public class TestBuilders {

    private static final EasyRandom easyRandom = new EasyRandom();

    public static DiscoveryResponse.DiscoveryResponseBuilder discoveryResponse() {
        return easyRandom.nextObject(DiscoveryResponse.DiscoveryResponseBuilder.class);
    }

    public static User.UserBuilder user() {
        return easyRandom.nextObject(User.UserBuilder.class);
    }


    public static PatientIdentifier.PatientIdentifierBuilder patientIdentifierBuilder() {
        return easyRandom.nextObject(PatientIdentifier.PatientIdentifierBuilder.class);
    }

    public static String string() {
        return easyRandom.nextObject(String.class);
    }

    public static in.projecteka.consentmanager.link.discovery.model.patient.response.DiscoveryResult.DiscoveryResultBuilder discoveryResult() {
        return easyRandom.nextObject(in.projecteka.consentmanager.link.discovery.model.patient.response.DiscoveryResult.DiscoveryResultBuilder.class);
    }

    public static in.projecteka.consentmanager.link.discovery.model.patient.response.Patient.PatientBuilder patient() {
        return easyRandom.nextObject(in.projecteka.consentmanager.link.discovery.model.patient.response.Patient.PatientBuilder.class);
    }

    public static CareContextRepresentation.CareContextRepresentationBuilder careContextRepresentation() {
        return easyRandom.nextObject(CareContextRepresentation.CareContextRepresentationBuilder.class);
    }
}
