package in.projecteka.dataflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.dataflow.model.AccessPeriod;
import in.projecteka.dataflow.model.ConsentArtefactRepresentation;
import in.projecteka.dataflow.model.ConsentStatus;
import in.projecteka.dataflow.model.DataFlowRequest;
import in.projecteka.dataflow.model.HIUReference;
import in.projecteka.dataflow.model.hip.DataRequest;
import in.projecteka.library.common.CentralRegistry;
import in.projecteka.library.common.GatewayTokenVerifier;
import in.projecteka.library.common.RequestValidator;
import in.projecteka.library.common.ServiceCaller;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static in.projecteka.dataflow.TestBuilders.OBJECT_MAPPER;
import static in.projecteka.dataflow.TestBuilders.consentArtefactRepresentation;
import static in.projecteka.dataflow.TestBuilders.dataRequest;
import static in.projecteka.dataflow.TestBuilders.gatewayDataFlowRequest;
import static in.projecteka.dataflow.TestBuilders.string;
import static in.projecteka.dataflow.TestBuilders.toDate;
import static in.projecteka.dataflow.TestBuilders.toDateWithMilliSeconds;
import static in.projecteka.library.common.Role.GATEWAY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class DataFlowRequesterUserJourneyTest {

    @SuppressWarnings("unused")
    @MockBean
    private DestinationsConfig destinationsConfig;

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private DataFlowRequestRepository dataFlowRequestRepository;

    @MockBean
    private PostDataFlowRequestApproval postDataFlowRequestApproval;

    @MockBean
    private DataFlowBroadcastListener dataFlowBroadcastListener;

    @MockBean
    private CentralRegistry centralRegistry;

    @SuppressWarnings("unused")
    @MockBean
    private DataRequestNotifier dataRequestNotifier;

    @SuppressWarnings("unused")
    @MockBean(name = "gatewayJWKSet")
    private JWKSet centralRegistryJWKSet;

    @SuppressWarnings("unused")
    @MockBean(name = "identityServiceJWKSet")
    private JWKSet identityServiceJWKSet;

    @MockBean
    private GatewayTokenVerifier gatewayTokenVerifier;

    @MockBean
    private RequestValidator validator;

    @MockBean
    private DataFlowRequestClient dataFlowRequestClient;

    @MockBean
    private ConsentManagerClient consentManagerClient;

    @Test
    void shouldSendDataRequestToHip() {
        DataRequest dataRequest = dataRequest().build();
        String hipId = string();
        String consentId = string();
        ConsentArtefactRepresentation caRep = ConsentArtefactRepresentation.builder().build();

        when(consentManagerClient.getConsentArtefact(consentId)).thenReturn(Mono.just(caRep));
        when(dataRequestNotifier.notifyHip(dataRequest, hipId)).thenReturn(Mono.empty());

        dataFlowBroadcastListener.configureAndSendDataRequestFor(dataRequest);

        verify(dataFlowBroadcastListener).configureAndSendDataRequestFor(dataRequest);
    }

    void shouldSendDataRequestToGateway() throws JsonProcessingException {
        String token = string();
        var hiuId = "10000005";
        var dataFlowRequest = gatewayDataFlowRequest().build();
        dataFlowRequest.getHiRequest().getDateRange().setFrom(toDate("2020-01-15T08:47:48"));
        dataFlowRequest.getHiRequest().getDateRange().setTo(toDate("2020-01-20T08:47:48"));
        var consentArtefact = consentArtefactRepresentation().build();
        consentArtefact.setStatus(ConsentStatus.GRANTED);
        consentArtefact.getConsentDetail().setHiu(HIUReference.builder().id(hiuId).name("MAX").build());
        consentArtefact.getConsentDetail().getPermission().setDataEraseAt(toDateWithMilliSeconds("253379772420000"));
        consentArtefact.getConsentDetail().getPermission().
                setDateRange(AccessPeriod.builder()
                        .fromDate(toDate("2020-01-15T08:47:48"))
                        .toDate(toDate("2020-01-20T08:47:48"))
                        .build());
        var consentArtefactRepresentationJson = OBJECT_MAPPER.writeValueAsString(consentArtefact);

        //consentArtefactRepresentationJson;

        when(validator.put(anyString(), any())).thenReturn(Mono.empty());
        when(validator.validate(anyString(), any())).thenReturn(Mono.just(Boolean.TRUE));
        when(gatewayTokenVerifier.verify(token)).thenReturn(Mono.just(new ServiceCaller(hiuId, List.of(GATEWAY))));
        when(consentManagerClient.getConsentArtefact(dataFlowRequest.getHiRequest().getConsent().getId()))
                .thenReturn(Mono.just(consentArtefact));
        when(dataFlowRequestRepository.addDataFlowRequest(anyString(), any(DataFlowRequest.class)))
                .thenReturn(Mono.empty());
        when(dataFlowRequestClient.sendHealthInformationResponseToGateway(any(), eq(hiuId)))
                .thenReturn(Mono.empty());

        webTestClient
                .post()
                .uri(Constants.PATH_HEALTH_INFORMATION_REQUEST)
                .header(AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dataFlowRequest)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isAccepted();
    }
}
