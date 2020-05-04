package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.IdentityServiceClient;
import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.clients.properties.IdentityServiceProperties;
import lombok.AllArgsConstructor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class TokenService {

    private final IdentityServiceProperties keyCloakProperties;
    private final IdentityServiceClient identityServiceClient;

    public Mono<Session> tokenForAdmin() {
        return identityServiceClient.getToken(
                loginRequestWith(keyCloakProperties.getUserName(), keyCloakProperties.getPassword()));
    }

    public Mono<Session> tokenForUser(String userName, String password) {
        return identityServiceClient.getToken(loginRequestWith(userName, password));
    }

    public Mono<Session> tokenForOtpUser(String username, String sessionId, String otp) {
        return identityServiceClient.getToken(loginRequestForOtp(username,sessionId,otp));
    }

    private MultiValueMap<String, String> loginRequestForOtp(String username, String sessionId, String otp) {
        LinkedMultiValueMap<String, String> formData = loginRequestCommon();
        formData.add("username",username);
        formData.add("session_id",sessionId);
        formData.add("otp",otp);
        return formData;
    }

    private MultiValueMap<String, String> loginRequestWith(String username, String password) {
        LinkedMultiValueMap<String, String> formData = loginRequestCommon();
        formData.add("username", username);
        formData.add("password", password);
        return formData;
    }

    private LinkedMultiValueMap<String, String> loginRequestCommon() {
        var formData = new LinkedMultiValueMap<String, String>();
        formData.add("grant_type", "password");
        formData.add("scope", "openid");
        formData.add("client_id", keyCloakProperties.getClientId());
        formData.add("client_secret", keyCloakProperties.getClientSecret());
        return formData;
    }

    public Mono<Void> revoke(String refreshToken) {
        var formData = new LinkedMultiValueMap<String, String>();
        formData.add("client_id", keyCloakProperties.getClientId());
        formData.add("client_secret", keyCloakProperties.getClientSecret());
        formData.add("refresh_token", refreshToken);

        return identityServiceClient.logout(formData);
    }
}
