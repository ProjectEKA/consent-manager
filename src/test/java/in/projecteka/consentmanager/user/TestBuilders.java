package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.model.KeycloakToken;
import in.projecteka.consentmanager.user.model.SignUpRequest;
import in.projecteka.consentmanager.user.model.UserSignUpEnquiry;
import org.jeasy.random.EasyRandom;

public class TestBuilders {

    private static final EasyRandom easyRandom = new EasyRandom();

    public static SignUpRequest.SignUpRequestBuilder signUpRequest() {
        return easyRandom.nextObject(SignUpRequest.SignUpRequestBuilder.class);
    }

    public static KeycloakToken.KeycloakTokenBuilder keycloakToken() {
        return easyRandom.nextObject(KeycloakToken.KeycloakTokenBuilder.class);
    }

    public static String string() {
        return easyRandom.nextObject(String.class);
    }

    public static UserSignUpEnquiry.UserSignUpEnquiryBuilder userSignUpEnquiry() {
        return easyRandom.nextObject(UserSignUpEnquiry.UserSignUpEnquiryBuilder.class);
    }
}
