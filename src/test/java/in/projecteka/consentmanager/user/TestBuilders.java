package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.user.model.SessionRequest;
import in.projecteka.consentmanager.user.model.SignUpRequest;
import in.projecteka.consentmanager.user.model.UserSignUpEnquiry;
import org.jeasy.random.EasyRandom;

public class TestBuilders {

    private static final EasyRandom easyRandom = new EasyRandom();

    public static SignUpRequest.SignUpRequestBuilder signUpRequest() {
        return easyRandom.nextObject(SignUpRequest.SignUpRequestBuilder.class);
    }

    public static Session.SessionBuilder session() {
        return easyRandom.nextObject(Session.SessionBuilder.class);
    }

    public static String string() {
        return easyRandom.nextObject(String.class);
    }

    public static UserSignUpEnquiry.UserSignUpEnquiryBuilder userSignUpEnquiry() {
        return easyRandom.nextObject(UserSignUpEnquiry.UserSignUpEnquiryBuilder.class);
    }

    public static SessionRequest.SessionRequestBuilder sessionRequest() {
        return easyRandom.nextObject(SessionRequest.SessionRequestBuilder.class);
    }
}
