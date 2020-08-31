package in.projecteka.user;

import in.projecteka.library.clients.IdentityServiceClient;
import in.projecteka.library.clients.model.Session;
import in.projecteka.user.exception.InvalidPasswordException;
import in.projecteka.user.exception.InvalidRefreshTokenException;
import in.projecteka.user.exception.InvalidUserNameException;
import in.projecteka.user.model.GrantType;
import in.projecteka.user.properties.IdentityServiceProperties;
import lombok.AllArgsConstructor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import static reactor.core.publisher.Mono.error;

@AllArgsConstructor
public class TokenService {

    public static final String USERNAME = "username";
    private final IdentityServiceProperties keyCloakProperties;
    private final IdentityServiceClient identityServiceClient;
    private final UserRepository userRepository;

    public Mono<Session> tokenForAdmin() {
        return identityServiceClient.getToken(
                loginRequestWith(keyCloakProperties.getUserName(), keyCloakProperties.getPassword()));
    }

    public Mono<Session> tokenForUser(String userName, String password) {
        return identityServiceClient.getToken(loginRequestWith(userName, password))
                .onErrorResume(error -> userRepository.userWith(userName)
                        .switchIfEmpty(error(new InvalidUserNameException()))
                        .flatMap(user -> error(new InvalidPasswordException())));
    }

    public Mono<Session> tokenForRefreshToken(String userName, String refreshToken) {
        return identityServiceClient.getToken(loginRequestWithRefreshToken(userName, refreshToken))
                .onErrorResume(error -> userRepository.userWith(userName)
                        .switchIfEmpty(error(new InvalidUserNameException()))
                        .flatMap(user -> error(new InvalidRefreshTokenException())));
    }

    public Mono<Session> tokenForOtpUser(String username, String sessionId, String otp) {
        return identityServiceClient.getToken(loginRequestForOtp(username, sessionId, otp));
    }

    private MultiValueMap<String, String> loginRequestForOtp(String username, String sessionId, String otp) {
        LinkedMultiValueMap<String, String> formData = loginRequestCommon();
        formData.add(USERNAME, username);
        formData.add("session_id", sessionId);
        formData.add("otp", otp);
        return formData;
    }

    private MultiValueMap<String, String> loginRequestWith(String username, String password) {
        LinkedMultiValueMap<String, String> formData = loginRequestCommon();
        formData.add(USERNAME, username);
        formData.add("password", password);
        return formData;
    }

    private MultiValueMap<String, String> loginRequestWithRefreshToken(String username, String refreshToken) {
        LinkedMultiValueMap<String, String> formData = loginRequestCommon();
        formData.add(USERNAME, username);
        formData.add("refresh_token", refreshToken);
        formData.set("grant_type", GrantType.REFRESH_TOKEN.getValue());
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
