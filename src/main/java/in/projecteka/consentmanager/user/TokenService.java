package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.IdentityServiceClient;
import in.projecteka.consentmanager.clients.properties.IdentityServiceProperties;
import in.projecteka.consentmanager.clients.model.KeycloakToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

public class TokenService {

    private final IdentityServiceProperties keyCloakProperties;
    private IdentityServiceClient identityServiceClient;

    public TokenService(IdentityServiceProperties identityServiceProperties, IdentityServiceClient identityServiceClient) {
        this.keyCloakProperties = identityServiceProperties;
        this.identityServiceClient = identityServiceClient;
    }

    public Mono<KeycloakToken> tokenForAdmin() {
        return identityServiceClient.getToken(
                loginRequestWith(keyCloakProperties.getUserName(), keyCloakProperties.getPassword()));
    }

    public Mono<KeycloakToken> tokenForUser(String userName, String password) {
        return identityServiceClient.getToken(loginRequestWith(userName, password));
    }

    private MultiValueMap<String, String> loginRequestWith(String username, String password) {
        var formData = new LinkedMultiValueMap<String, String>();
        formData.add("grant_type", "password");
        formData.add("scope", "openid");
        formData.add("client_id", keyCloakProperties.getClientId());
        formData.add("client_secret", keyCloakProperties.getClientSecret());
        formData.add("username", username);
        formData.add("password", password);
        return formData;
    }
}
