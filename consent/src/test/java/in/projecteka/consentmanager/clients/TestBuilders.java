package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.KeyCloakUserPasswordChangeRequest;
import in.projecteka.consentmanager.clients.model.User;
import in.projecteka.consentmanager.link.discovery.model.patient.request.Patient;
import in.projecteka.consentmanager.link.discovery.model.patient.request.PatientRequest;
import in.projecteka.consentmanager.properties.IdentityServiceProperties;
import in.projecteka.library.clients.model.Address;
import in.projecteka.library.clients.model.Coding;
import in.projecteka.library.clients.model.Identifier;
import in.projecteka.library.clients.model.KeycloakUser;
import in.projecteka.library.clients.model.Provider.ProviderBuilder;
import in.projecteka.library.clients.model.Session;
import in.projecteka.library.clients.model.Telecom;
import in.projecteka.library.clients.model.Type;
import org.jeasy.random.EasyRandom;

public class TestBuilders {

    private static final EasyRandom easyRandom = new EasyRandom();

    public static Telecom.TelecomBuilder telecom() {
        return easyRandom.nextObject(Telecom.TelecomBuilder.class);
    }

    public static ProviderBuilder provider() {
        return easyRandom.nextObject(ProviderBuilder.class);
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

    public static Patient.PatientBuilder patientInRequest() {
        return easyRandom.nextObject(Patient.PatientBuilder.class);
    }

    public static in.projecteka.consentmanager.link.discovery.model.patient.response.Patient.PatientBuilder patientInResponse() {
        return easyRandom.nextObject(in.projecteka.consentmanager.link.discovery.model.patient.response.Patient.PatientBuilder.class);
    }

    public static Identifier.IdentifierBuilder identifier() {
        return easyRandom.nextObject(Identifier.IdentifierBuilder.class);
    }

    public static KeycloakUser.KeycloakUserBuilder keycloakCreateUser() {
        return easyRandom.nextObject(KeycloakUser.KeycloakUserBuilder.class);
    }

    public static IdentityServiceProperties.IdentityServicePropertiesBuilder keycloakProperties() {
        return easyRandom.nextObject(IdentityServiceProperties.IdentityServicePropertiesBuilder.class);
    }


    public static Session.SessionBuilder session() {
        return easyRandom.nextObject(Session.SessionBuilder.class);
    }

    public static String string() {
        return easyRandom.nextObject(String.class);
    }

    public static in.projecteka.consentmanager.link.link.model.PatientLinkReferenceRequest.PatientLinkReferenceRequestBuilder patientLinkReferenceRequest() {
        return easyRandom.nextObject(in.projecteka.consentmanager.link.link.model.PatientLinkReferenceRequest.PatientLinkReferenceRequestBuilder.class);
    }

    public static in.projecteka.consentmanager.clients.model.PatientLinkReferenceRequest.PatientLinkReferenceRequestBuilder patientLinkReferenceRequestForHIP() {
        return easyRandom.nextObject(in.projecteka.consentmanager.clients.model.PatientLinkReferenceRequest.PatientLinkReferenceRequestBuilder.class);
    }

    public static KeyCloakUserPasswordChangeRequest.KeyCloakUserPasswordChangeRequestBuilder keyCloakUserPasswordChangeRequest() {
        return easyRandom.nextObject(KeyCloakUserPasswordChangeRequest.KeyCloakUserPasswordChangeRequestBuilder.class);
    }
}
