package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.KeycloakClient;
import in.projecteka.consentmanager.user.model.KeycloakToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

public class TokenService {

    private final KeycloakProperties keyCloakProperties;
    private KeycloakClient keycloakClient;

    public TokenService(KeycloakProperties keycloakProperties, KeycloakClient keycloakClient) {
        this.keyCloakProperties = keycloakProperties;
        this.keycloakClient = keycloakClient;
    }

    public Mono<KeycloakToken> tokenForAdmin() {
        MultiValueMap<String, String> formData= new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("scope", "openid");
        formData.add("client_id", keyCloakProperties.getClientId());
        formData.add("client_secret", keyCloakProperties.getClientSecret());
        formData.add("username", keyCloakProperties.getUserName());
        formData.add("password", keyCloakProperties.getPassword());

        return keycloakClient.getToken(formData);
    }

    public Mono<KeycloakToken> tokenForUser(String userName, String password) {
        MultiValueMap<String, String> formData= new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("scope", "openid");
        formData.add("client_id", keyCloakProperties.getClientId());
        formData.add("client_secret", keyCloakProperties.getClientSecret());
        formData.add("username", userName);
        formData.add("password", password);

        return keycloakClient.getToken(formData);
    }
}
