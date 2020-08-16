package clients;

import in.projecteka.consentmanager.clients.model.Address;
import in.projecteka.consentmanager.clients.model.Coding;
import in.projecteka.consentmanager.clients.model.Identifier;
import in.projecteka.consentmanager.clients.model.KeyCloakUserPasswordChangeRequest;
import in.projecteka.consentmanager.clients.model.KeycloakUser;
import in.projecteka.consentmanager.clients.model.Provider;
import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.clients.model.Telecom;
import in.projecteka.consentmanager.clients.model.Type;
import in.projecteka.consentmanager.clients.model.User;
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


    public static User.UserBuilder user() {
        return easyRandom.nextObject(User.UserBuilder.class);
    }

    public static Identifier.IdentifierBuilder identifier() {
        return easyRandom.nextObject(Identifier.IdentifierBuilder.class);
    }

    public static KeycloakUser.KeycloakUserBuilder keycloakCreateUser() {
        return easyRandom.nextObject(KeycloakUser.KeycloakUserBuilder.class);
    }

    public static Session.SessionBuilder session() {
        return easyRandom.nextObject(Session.SessionBuilder.class);
    }

    public static KeyCloakUserPasswordChangeRequest.KeyCloakUserPasswordChangeRequestBuilder keyCloakUserPasswordChangeRequest() {
        return easyRandom.nextObject(KeyCloakUserPasswordChangeRequest.KeyCloakUserPasswordChangeRequestBuilder.class);
    }
}
