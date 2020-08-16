package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.model.Address;
import in.projecteka.consentmanager.clients.model.Coding;
import in.projecteka.consentmanager.clients.model.Identifier;
import in.projecteka.consentmanager.clients.model.KeyCloakUserPasswordChangeRequest;
import in.projecteka.consentmanager.clients.model.KeycloakUser;
import in.projecteka.consentmanager.clients.model.PatientLinkReferenceResponse;
import in.projecteka.consentmanager.clients.model.PatientLinkRequest;
import in.projecteka.consentmanager.clients.model.PatientLinkResponse;
import in.projecteka.consentmanager.clients.model.Provider;
import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.clients.model.Telecom;
import in.projecteka.consentmanager.clients.model.Type;
import in.projecteka.consentmanager.clients.model.User;
import in.projecteka.consentmanager.clients.properties.IdentityServiceProperties;
import in.projecteka.consentmanager.link.discovery.model.patient.request.Patient;
import in.projecteka.consentmanager.link.discovery.model.patient.request.PatientRequest;
import in.projecteka.consentmanager.link.discovery.model.patient.response.CareContext;
import in.projecteka.consentmanager.link.discovery.model.patient.response.PatientResponse;
import in.projecteka.library.clients.model.ErrorRepresentation;
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

    public static in.projecteka.consentmanager.clients.model.PatientLinkReferenceRequest.PatientLinkReferenceRequestBuilder patientLinkReferenceRequestForHIP() {
        return easyRandom.nextObject(in.projecteka.consentmanager.clients.model.PatientLinkReferenceRequest.PatientLinkReferenceRequestBuilder.class);
    }

    public static KeyCloakUserPasswordChangeRequest.KeyCloakUserPasswordChangeRequestBuilder keyCloakUserPasswordChangeRequest() {
        return easyRandom.nextObject(KeyCloakUserPasswordChangeRequest.KeyCloakUserPasswordChangeRequestBuilder.class);
    }
}
