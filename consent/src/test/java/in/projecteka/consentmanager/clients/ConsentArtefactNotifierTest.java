package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.properties.GatewayServiceProperties;
import in.projecteka.consentmanager.consent.model.request.ConsentNotifier;
import in.projecteka.consentmanager.consent.model.request.HIPNotificationRequest;
import in.projecteka.consentmanager.consent.model.request.HIUNotificationRequest;
import in.projecteka.library.clients.model.ClientError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static in.projecteka.consentmanager.clients.TestBuilders.string;
import static in.projecteka.consentmanager.Constants.HDR_HIP_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

class ConsentArtefactNotifierTest {
    @Captor
    private ArgumentCaptor<ClientRequest> captor;
    private ConsentArtefactNotifier consentArtefactNotifier;

    @Mock
    private ExchangeFunction exchangeFunction;

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);
        WebClient.Builder webClientBuilder = WebClient.builder().exchangeFunction(exchangeFunction);
        var token = string();
        var serviceProperties = new GatewayServiceProperties("http://example.com", 1000, "", "", "", 10);

        consentArtefactNotifier = new ConsentArtefactNotifier(webClientBuilder,
                () -> Mono.just(token),
                serviceProperties);
    }

    @Test
    void shouldSendConsentArtefactNotificationToHIP() {
        var notificationRequest = HIPNotificationRequest.builder().build();
        String hipId = "MAX-ID";
        when(exchangeFunction.exchange(captor.capture())).thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HDR_HIP_ID, hipId)
                .build()));

        StepVerifier.create(consentArtefactNotifier.sendConsentArtefactToHIP(notificationRequest, hipId))
                .verifyComplete();
    }

    @Test
    void shouldThrowErrorForInvalidToken() {
        var notificationRequest = HIPNotificationRequest.builder().build();
        String hipId = "MAX-ID";
        when(exchangeFunction.exchange(captor.capture()))
                .thenReturn(Mono.just(ClientResponse.create(HttpStatus.UNAUTHORIZED)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header(HDR_HIP_ID, hipId)
                        .build()));

        StepVerifier.create(consentArtefactNotifier.sendConsentArtefactToHIP(notificationRequest, hipId))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is4xxClientError())
                .verify();
    }

    @Test
    void shouldThrowErrorIfSendConsentArtefactNotificationToHIPFails() {
        var notificationRequest = HIPNotificationRequest.builder().build();
        String hipId = "MAX-ID";
        when(exchangeFunction.exchange(captor.capture()))
                .thenReturn(Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header(HDR_HIP_ID, hipId)
                        .build()));

        StepVerifier.create(consentArtefactNotifier.sendConsentArtefactToHIP(notificationRequest, hipId))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is5xxServerError())
                .verify();
    }

    @Test
    void sendConsentArtifactToHIU() {
        var token = string();
        when(exchangeFunction.exchange(captor.capture())).thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .build()));
        var webClientBuilder = WebClient.builder().exchangeFunction(exchangeFunction);
        var serviceProperties = new GatewayServiceProperties("http://example.com", 2000, "", "", "", 10);
        var dataRequestNotifier = new ConsentArtefactNotifier(webClientBuilder,
                () -> Mono.just(token),
                serviceProperties);
        var request = new HIUNotificationRequest(LocalDateTime.now(), UUID.randomUUID(), new ConsentNotifier());

        StepVerifier.create(dataRequestNotifier.sendConsentArtifactToHIU(request, "1000005")).verifyComplete();

        assertThat(captor.getValue().url().toString()).hasToString("http://example.com/consents/hiu/notify");
        assertThat(captor.getValue().headers().getFirst(AUTHORIZATION)).isEqualTo(token);
    }

    @Test
    void shouldThrowExceptionWhenUnAuthorizedTokenPass() {
        var token = string();
        when(exchangeFunction.exchange(captor.capture()))
                .thenReturn(Mono.just(ClientResponse.create(HttpStatus.UNAUTHORIZED)
                        .header("Content-Type", "application/json")
                        .build()));
        var webClientBuilder = WebClient.builder().exchangeFunction(exchangeFunction);
        var serviceProperties = new GatewayServiceProperties("http://example.com", 2000, "", "", "", 10);
        ConsentArtefactNotifier dataRequestNotifier = new ConsentArtefactNotifier(webClientBuilder,
                () -> Mono.just(token),
                serviceProperties);
        var request = new HIUNotificationRequest(LocalDateTime.now(), UUID.randomUUID(), new ConsentNotifier());

        StepVerifier.create(dataRequestNotifier.sendConsentArtifactToHIU(request, "1000005"))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is4xxClientError())
                .verify();
        assertThat(captor.getValue().url().toString()).hasToString("http://example.com/consents/hiu/notify");
        assertThat(captor.getValue().headers().getFirst(AUTHORIZATION)).isEqualTo(token);
    }

    @Test
    void ShouldThrowExceptionWhenInternalServerGetError() {
        var token = string();
        when(exchangeFunction.exchange(captor.capture())).thenReturn(Mono.just(ClientResponse
                .create(HttpStatus.INTERNAL_SERVER_ERROR)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build()));
        var webClientBuilder = WebClient.builder().exchangeFunction(exchangeFunction);
        var serviceProperties = new GatewayServiceProperties("http://example.com", 2000, "", "", "", 10);
        var dataRequestNotifier = new ConsentArtefactNotifier(webClientBuilder,
                () -> Mono.just(token),
                serviceProperties);
        var request = new HIUNotificationRequest(LocalDateTime.now(), UUID.randomUUID(), new ConsentNotifier());

        StepVerifier.create(dataRequestNotifier.sendConsentArtifactToHIU(request, "1000005"))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is5xxServerError())
                .verify();
        assertThat(captor.getValue().url().toString()).hasToString("http://example.com/consents/hiu/notify");
        assertThat(captor.getValue().headers().getFirst(AUTHORIZATION)).isEqualTo(token);
    }
}
