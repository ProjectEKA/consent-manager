package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.properties.GatewayServiceProperties;
import in.projecteka.consentmanager.consent.model.request.HIPNotificationRequest;
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

import static in.projecteka.consentmanager.clients.HeaderConstants.HDR_HIP_ID;
import static in.projecteka.consentmanager.clients.TestBuilders.string;
import static org.mockito.Mockito.when;

public class ConsentArtefactNotifierTest {
    @Captor
    private ArgumentCaptor<ClientRequest> captor;
    private ConsentArtefactNotifier consentArtefactNotifier;
    @Mock
    private ExchangeFunction exchangeFunction;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        WebClient.Builder webClientBuilder = WebClient.builder().exchangeFunction(exchangeFunction);
        var token = string();
        GatewayServiceProperties serviceProperties = new GatewayServiceProperties("http://example.com", 1000);

        consentArtefactNotifier = new ConsentArtefactNotifier(webClientBuilder,() -> Mono.just(token), serviceProperties);
    }

    @Test
    void shouldSendConsentArtefactNotificationToHIP() {
        HIPNotificationRequest notificationRequest = HIPNotificationRequest.builder().build();
        String hipId = "MAX-ID";


        when(exchangeFunction.exchange(captor.capture())).thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HDR_HIP_ID, hipId)
                .build()));

        StepVerifier.create(consentArtefactNotifier.sendConsentArtefactToHIP(notificationRequest,hipId))
                .verifyComplete();
    }

    @Test
    void shouldThrowErrorForInvalidToken() {
        HIPNotificationRequest notificationRequest = HIPNotificationRequest.builder().build();
        String hipId = "MAX-ID";


        when(exchangeFunction.exchange(captor.capture())).thenReturn(Mono.just(ClientResponse.create(HttpStatus.UNAUTHORIZED)
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
        HIPNotificationRequest notificationRequest = HIPNotificationRequest.builder().build();
        String hipId = "MAX-ID";


        when(exchangeFunction.exchange(captor.capture())).thenReturn(Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HDR_HIP_ID, hipId)
                .build()));

        StepVerifier.create(consentArtefactNotifier.sendConsentArtefactToHIP(notificationRequest, hipId))
                .expectErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus().is5xxServerError())
                .verify();
    }
}
