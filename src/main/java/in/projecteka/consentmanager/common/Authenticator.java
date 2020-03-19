package in.projecteka.consentmanager.common;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.properties.IdentityServiceProperties;
import org.apache.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Properties;

public class Authenticator {

    private final WebClient.Builder webClientBuilder;
    private final static Logger logger = Logger.getLogger(Authenticator.class);

    public Authenticator(WebClient.Builder webClientBuilder,
                         IdentityServiceProperties identityServiceProperties) {
        this.webClientBuilder = webClientBuilder;
        this.webClientBuilder.baseUrl(identityServiceProperties.getBaseUrl());
    }

    public Mono<Caller> verify(String token) {
        return webClientBuilder.build()
                .get()
                .uri(uriBuilder ->
                        uriBuilder.path("/realms/consent-manager/protocol/openid-connect/userinfo").build())
                .header(HttpHeaders.AUTHORIZATION, token)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::isError, clientResponse -> clientResponse.bodyToMono(Properties.class)
                        .doOnNext(properties -> logger.error(properties.toString()))
                        .thenReturn(ClientError.unAuthorized()))
                .bodyToMono(Properties.class)
                .map(Authenticator::to)
                .cache();
    }

    private static Caller to(Properties properties) {
        final String serviceAccountPrefix = "service-account-";
        var preferredUsername = properties.getProperty("preferred_username");
        var serviceAccount = preferredUsername.startsWith(serviceAccountPrefix);
        var userName = serviceAccount ? preferredUsername.substring(serviceAccountPrefix.length()) : preferredUsername;
        return new Caller(userName, serviceAccount);
    }
}
