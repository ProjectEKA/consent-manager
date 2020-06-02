package in.projecteka.consentmanager.consent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.jwk.JWKSet;
import in.projecteka.consentmanager.DestinationsConfig;
import in.projecteka.consentmanager.clients.model.Error;
import in.projecteka.consentmanager.clients.model.ErrorCode;
import in.projecteka.consentmanager.clients.model.ErrorRepresentation;
import in.projecteka.consentmanager.clients.model.Provider;
import in.projecteka.consentmanager.common.Authenticator;
import in.projecteka.consentmanager.common.Caller;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.consent.model.ConsentStatus;
import in.projecteka.consentmanager.consent.model.ListResult;
import in.projecteka.consentmanager.consent.model.RevokeRequest;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactRepresentation;
import in.projecteka.consentmanager.consent.model.response.ConsentArtefactResponse;
import in.projecteka.consentmanager.dataflow.DataFlowBroadcastListener;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static in.projecteka.consentmanager.consent.TestBuilders.OBJECT_MAPPER;
import static in.projecteka.consentmanager.consent.TestBuilders.consentArtefactRepresentation;
import static in.projecteka.consentmanager.consent.TestBuilders.consentRepresentation;
import static in.projecteka.consentmanager.consent.TestBuilders.consentRequestDetail;
import static in.projecteka.consentmanager.consent.TestBuilders.string;
import static in.projecteka.consentmanager.dataflow.Utils.toDateWithMilliSeconds;
import static java.lang.String.valueOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ConsentArtefactUserJourneyTest {

    @Autowired
    private WebTestClient webTestClient;

    @SuppressWarnings("unused")
    @MockBean
    private DestinationsConfig destinationsConfig;

    @SuppressWarnings("unused")
    @MockBean
    private HiuConsentNotificationListener hiuConsentNotificationListener;

    @MockBean
    private PinVerificationTokenService pinVerificationTokenService;

    @MockBean
    private HipConsentNotificationListener hipConsentNotificationListener;

    @SuppressWarnings("unused")
    @MockBean
    private DataFlowBroadcastListener dataFlowBroadcastListener;

    @SuppressWarnings("unused")
    @MockBean
    private ConsentRequestNotificationListener consentRequestNotificationListener;

    @MockBean
    private CentralRegistry centralRegistry;

    @MockBean
    private ConsentArtefactRepository consentArtefactRepository;

    @MockBean
    private ConsentNotificationPublisher consentNotificationPublisher;

    @MockBean
    private ConsentRequestRepository repository;

    @MockBean
    private Authenticator authenticator;

    @SuppressWarnings("unused")
    @MockBean(name = "centralRegistryJWKSet")
    private JWKSet centralRegistryJWKSet;

    @SuppressWarnings("unused")
    @MockBean(name = "identityServiceJWKSet")
    private JWKSet identityServiceJWKSet;

    @SuppressWarnings("unused")
    @MockBean
    private ConceptValidator conceptValidator;

    @Test
    public void shouldListConsentArtifacts() throws ParseException {
        var consentArtefact = consentArtefactRepresentation().build();
        var token = string();
        var patientId = consentArtefact.getConsentDetail().getPatient().getId();
        var consentRequestId = "request-id";
        consentArtefact.getConsentDetail().getPermission().setDataEraseAt(toDateWithMilliSeconds("253379772420000"));

        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller(patientId, false)));
        when(consentArtefactRepository.getConsentArtefacts(eq(consentRequestId)))
                .thenReturn(Flux.just(consentArtefact.getConsentDetail().getConsentId()));
        when(consentArtefactRepository.getConsentArtefact(eq(consentArtefact.getConsentDetail().getConsentId())))
                .thenReturn(Mono.just(consentArtefact));

        webTestClient.get()
                .uri("/consent-requests/" + consentRequestId + "/consent-artefacts")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<ConsentArtefactRepresentation>>() {
                })
                .value(value -> value.get(0).getConsentDetail(), equalTo(consentArtefact.getConsentDetail()))
                .value(value -> value.get(0).getStatus(), is(consentArtefact.getStatus()))
                .value(value -> value.get(0).getSignature(), is(consentArtefact.getSignature()));
    }

    @Test
    public void shouldThrowConsentArtifactNotFound() throws JsonProcessingException {
        var token = string();
        var consentArtefact = consentArtefactRepresentation().build();
        var patientId = consentArtefact.getConsentDetail().getPatient().getId();
        var errorResponse = new ErrorRepresentation(new Error(ErrorCode.CONSENT_ARTEFACT_NOT_FOUND,
                "Cannot find the consent artefact"));
        var errorResponseJson = OBJECT_MAPPER.writeValueAsString(errorResponse);
        var consentRequestId = "request-id";

        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller(patientId, false)));
        when(consentArtefactRepository.getConsentArtefacts(eq(consentRequestId)))
                .thenReturn(Flux.empty());
        when(consentArtefactRepository.getConsentArtefact(eq(consentArtefact.getConsentDetail().getConsentId())))
                .thenReturn(Mono.just(consentArtefact));

        webTestClient.get()
                .uri("/consent-requests/" + consentRequestId + "/consent-artefacts")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .json(errorResponseJson);
    }

    @Test
    public void shouldThrowInvalidRequester() throws JsonProcessingException {
        var token = string();
        var anotherUser = string();
        var consentArtefact = consentArtefactRepresentation().build();
        var errorResponse = new ErrorRepresentation(new Error(ErrorCode.CONSENT_ARTEFACT_FORBIDDEN,
                "Cannot retrieve Consent artefact"));
        var errorResponseJson = OBJECT_MAPPER.writeValueAsString(errorResponse);
        var consentRequestId = "request-id";

        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller(anotherUser, false)));
        when(consentArtefactRepository.getConsentArtefacts(eq(consentRequestId)))
                .thenReturn(Flux.just(consentArtefact.getConsentDetail().getConsentId()));
        when(consentArtefactRepository.getConsentArtefact(eq(consentArtefact.getConsentDetail().getConsentId())))
                .thenReturn(Mono.just(consentArtefact));

        webTestClient.get()
                .uri("/consent-requests/" + consentRequestId + "/consent-artefacts")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody()
                .json(errorResponseJson);
    }

    @Test
    public void shouldRevokeConsentArtefact() {
        var token = string();
        var consentRepresentation = consentRepresentation().status(ConsentStatus.GRANTED).build();
        var consentRequestId = consentRepresentation.getConsentRequestId();
        var consentRequestDetail =
                consentRequestDetail().requestId(consentRequestId).status(ConsentStatus.GRANTED).build();
        List<String> consentIds = new ArrayList<>();
        var consentId = consentRepresentation.getConsentDetail().getConsentId();
        consentIds.add(consentRepresentation.getConsentDetail().getConsentId());
        var revokeRequest = RevokeRequest.builder().consents(consentIds).build();
        var patientId = consentRepresentation.getConsentDetail().getPatient().getId();

        String scope = "consent.revoke";
        when(pinVerificationTokenService.validateToken(token, scope))
                .thenReturn(Mono.just(new Caller(patientId, false, "testSessionId")));
        when(consentArtefactRepository.getConsentWithRequest(eq(consentId)))
                .thenReturn(Mono.just(consentRepresentation));
        when(repository.requestOf(consentRequestId, ConsentStatus.GRANTED.toString(), patientId))
                .thenReturn(Mono.just(consentRequestDetail));
        when(consentArtefactRepository.updateStatus(consentId, consentRequestId, ConsentStatus.REVOKED))
                .thenReturn(Mono.empty());
        when(consentNotificationPublisher.publish(any())).thenReturn(Mono.empty());
        when(centralRegistry.providerWith(any())).thenReturn(Mono.just(Provider.builder().build()));

        webTestClient.post()
                .uri("/consents/revoke")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .bodyValue(revokeRequest)
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void shouldNotRevokeConsentArtefactWhenItIsNotInGrantedState() throws JsonProcessingException {
        var token = string();
        var scope = "consent.revoke";
        var consentRepresentation = consentRepresentation().status(ConsentStatus.REVOKED).build();
        var errorResponse = new ErrorRepresentation(new Error(ErrorCode.CONSENT_NOT_GRANTED, "Not a granted consent."));
        var errorResponseJson = OBJECT_MAPPER.writeValueAsString(errorResponse);
        var consentRequestId = consentRepresentation.getConsentRequestId();
        List<String> consentIds = new ArrayList<>();
        var consentId = consentRepresentation.getConsentDetail().getConsentId();
        consentIds.add(consentRepresentation.getConsentDetail().getConsentId());
        var revokeRequest = RevokeRequest.builder().consents(consentIds).build();
        var patientId = consentRepresentation.getConsentDetail().getPatient().getId();

        when(pinVerificationTokenService.validateToken(token, scope))
                .thenReturn(Mono.just(new Caller(patientId, false)));
        when(consentArtefactRepository.getConsentWithRequest(eq(consentId)))
                .thenReturn(Mono.just(consentRepresentation));
        when(repository.requestOf(consentRequestId, ConsentStatus.REVOKED.toString(), patientId))
                .thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/consents/revoke")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .bodyValue(revokeRequest)
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectBody()
                .json(errorResponseJson);

        verify(consentArtefactRepository, times(0)).updateStatus(any(), any(), any());
        verifyNoInteractions(consentNotificationPublisher);
    }

    @Test
    void shouldGetAllConsentArtefacts() {
        var token = string();
        var limit = 20;
        List<ConsentArtefactRepresentation> response = new ArrayList<>();
        response.add(consentArtefactRepresentation().build());
        var result = new ListResult<>(response, response.size());
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller("shweta@ncg", true)));
        when(consentArtefactRepository.getAllConsentArtefacts("shweta@ncg", limit, 0, null))
                .thenReturn(Mono.just(result));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/consent-artefacts").queryParam("limit", valueOf(limit)).build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ConsentArtefactResponse.class)
                .value(ConsentArtefactResponse::getConsentArtefacts, equalTo(response))
                .value(ConsentArtefactResponse::getLimit, Matchers.is(limit))
                .value(ConsentArtefactResponse::getSize, Matchers.is(1))
                .value(ConsentArtefactResponse::getOffset, Matchers.is(0));
    }

    @Test
    void shouldGetAllConsentArtefactsForStatus() {
        var token = string();
        var limit = 20;
        List<ConsentArtefactRepresentation> response = new ArrayList<>();
        var consentArtefactRepresentation = consentArtefactRepresentation().status(ConsentStatus.EXPIRED).build();
        response.add(consentArtefactRepresentation);
        var result = new ListResult<>(response, response.size());
        when(authenticator.verify(token)).thenReturn(Mono.just(new Caller("shweta@ncg", true)));
        when(consentArtefactRepository.getAllConsentArtefacts("shweta@ncg", limit, 0, "EXPIRED"))
                .thenReturn(Mono.just(result));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/consent-artefacts")
                        .queryParam("limit", valueOf(limit))
                        .queryParam("status", "EXPIRED")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ConsentArtefactResponse.class)
                .value(ConsentArtefactResponse::getConsentArtefacts, equalTo(response))
                .value(ConsentArtefactResponse::getLimit, Matchers.is(limit))
                .value(ConsentArtefactResponse::getSize, Matchers.is(1))
                .value(ConsentArtefactResponse::getOffset, Matchers.is(0));
    }
}
