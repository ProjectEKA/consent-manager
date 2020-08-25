package in.projecteka.user;

import in.projecteka.library.clients.IdentityServiceClient;
import in.projecteka.library.clients.model.Session;
import in.projecteka.user.properties.IdentityServiceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class TokenServiceTest {

    @Mock
    IdentityServiceProperties keyCloakProperties;
    @Mock
    IdentityServiceClient identityServiceClient;
    @Mock
    UserRepository userRepository;

    @BeforeEach
    void init() {
        initMocks(this);
    }

    @Test
    void shouldReturnTokenForAdmin() {
        Session session = new Session("accessToken", 2, 2, "refreshToken", "login");
        when(identityServiceClient.getToken(any())).thenReturn(Mono.just(session));
        TokenService tokenService = new TokenService(keyCloakProperties, identityServiceClient, userRepository);


        StepVerifier.create(tokenService.tokenForAdmin())
                .assertNext(s -> assertThat(s).isEqualTo(session))
                .verifyComplete();
    }

    @Test
    void shouldReturnTokenForUser() {
        Session session = new Session("accessToken", 2, 2, "refreshToken", "login");
        when(identityServiceClient.getToken(any())).thenReturn(Mono.just(session));
        TokenService tokenService = new TokenService(keyCloakProperties, identityServiceClient, userRepository);


        StepVerifier.create(tokenService.tokenForUser("userName","password"))
                .assertNext(s -> assertThat(s).isEqualTo(session))
                .verifyComplete();
    }

    @Test
    void shouldReturnRefreshTokenForUser() {
        Session session = new Session("accessToken", 2, 2, "refreshToken", "login");
        when(identityServiceClient.getToken(any())).thenReturn(Mono.just(session));
        TokenService tokenService = new TokenService(keyCloakProperties, identityServiceClient, userRepository);


        StepVerifier.create(tokenService.tokenForRefreshToken("userName","refereshToken"))
                .assertNext(s -> assertThat(s).isEqualTo(session))
                .verifyComplete();
    }
}