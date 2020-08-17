package in.projecteka.consentmanager.link.link;

import in.projecteka.consentmanager.clients.LinkServiceClient;
import in.projecteka.consentmanager.clients.model.CareContextRepresentation;
import in.projecteka.consentmanager.clients.model.PatientLinkRequest;
import in.projecteka.consentmanager.clients.model.PatientLinkResponse;
import in.projecteka.consentmanager.clients.model.PatientRepresentation;
import in.projecteka.consentmanager.link.Constants;
import in.projecteka.consentmanager.link.link.model.Hip;
import in.projecteka.consentmanager.link.link.model.LinkResponse;
import in.projecteka.consentmanager.link.link.model.Links;
import in.projecteka.consentmanager.link.link.model.PatientLinksResponse;
import in.projecteka.consentmanager.properties.LinkServiceProperties;
import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.common.ServiceAuthentication;
import in.projecteka.library.common.cache.CacheAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;

import static in.projecteka.consentmanager.link.Constants.HIP_INITIATED_ACTION_LINK;
import static in.projecteka.consentmanager.link.link.LinkTokenVerifier.ERROR_INVALID_TOKEN_NO_HIP_ID;
import static in.projecteka.consentmanager.link.link.LinkTokenVerifier.ERROR_INVALID_TOKEN_REQUIRED_ATTRIBUTES_NOT_PRESENT;
import static in.projecteka.consentmanager.link.link.LinkTokenVerifier.ERROR_TOKEN_IS_INVALID_OR_EXPIRED;
import static in.projecteka.consentmanager.link.link.TestBuilders.linkHipAction;
import static in.projecteka.consentmanager.link.link.TestBuilders.linkRequest;
import static in.projecteka.consentmanager.link.link.TestBuilders.links;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientLinkRequest;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientLinks;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientRepresentation;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class LinkTest {

    @Captor
    ArgumentCaptor<LinkResponse> linkResponseArgumentCaptor;
    Link link;

    @Mock
    private ServiceAuthentication serviceAuthentication;

    @Mock
    private LinkServiceClient linkServiceClient;

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private CacheAdapter<String, String> linkResults;

    @Mock
    private LinkTokenVerifier linkTokenVerifier;

    @BeforeEach
    void setUp() {
        initMocks(this);
        LinkServiceProperties linkServiceProperties = new LinkServiceProperties("http://tmc.gov.in/ncg-gateway",
                1000);
        link = Mockito.spy(new Link(linkServiceClient,
                linkRepository,
                serviceAuthentication,
                linkServiceProperties,
                linkResults,
                linkTokenVerifier));
    }

    @Test
    void shouldReturnLinkedCareContext() {
        var patientId = "5@ncg.com";
        String hipId = "10000005";
        var patientRepresentation = patientRepresentation().build();
        var links = links()
                .hip(Hip.builder()
                        .id(hipId)
                        .build())
                .patientRepresentations(patientRepresentation)
                .build();
        var listOfLinks = new ArrayList<Links>();
        listOfLinks.add(links);
        var patientLinks = patientLinks().id(patientId).links(listOfLinks).build();
        var linksResponse = links()
                .hip(Hip.builder()
                        .id(hipId)
                        .build())
                .patientRepresentations(patientRepresentation)
                .build();
        var listOfLinksResponse = new ArrayList<Links>();
        listOfLinksResponse.add(linksResponse);
        var patientLinksResponse = new PatientLinksResponse(patientLinks()
                .id(patientId)
                .links(listOfLinksResponse)
                .build());

        when(linkRepository.getLinkedCareContextsForAllHip(patientId)).thenReturn(Mono.just(patientLinks));

        StepVerifier.create(link.getLinkedCareContexts(patientId))
                .expectNext(patientLinksResponse)
                .verifyComplete();
    }

    @Test
    void confirmLinkCareContexts() {
        PatientLinkRequest patientLinkRequest = patientLinkRequest().build();
        String patientId = "patient";
        String hipId = "10000005";
        String linkConfirmationResult = "{\n" +
                "  \"requestId\": \"5f7a535d-a3fd-416b-b069-c97d021fbacd\",\n" +
                "  \"timestamp\": \"2020-05-25T15:03:44.557Z\",\n" +
                "  \"patient\": {\n" +
                "    \"referenceNumber\": \"HID-001\",\n" +
                "    \"display\": \"Patient with HID 001\",\n" +
                "    \"careContexts\": [\n" +
                "      {\n" +
                "        \"referenceNumber\": \"CC001\",\n" +
                "        \"display\": \"Episode 001\"\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  \"resp\": {\n" +
                "    \"requestId\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\"\n" +
                "  }\n" +
                "}";
        var transactionId = "transactionId";
        when(linkRepository.getTransactionIdFromLinkReference(patientLinkRequest.getLinkRefNumber()))
                .thenReturn(Mono.just(transactionId));
        when(linkRepository.getHIPIdFromDiscovery(transactionId)).thenReturn(Mono.just(hipId));
        when(linkServiceClient.confirmPatientLink(any(), eq(hipId))).thenReturn(Mono.just(Boolean.TRUE));
        when(linkResults.get(any())).thenReturn(Mono.just(linkConfirmationResult));
        when(linkRepository.insertToLink(eq(hipId),
                eq(patientId),
                eq(patientLinkRequest.getLinkRefNumber()),
                any(),
                eq(Constants.LINK_INITIATOR_CM)))
                .thenReturn(Mono.empty());
        var cc001 = CareContextRepresentation.builder().referenceNumber("CC001").display("Episode 001").build();
        var patient = PatientRepresentation.builder()
                .referenceNumber("HID-001")
                .display("Patient with HID 001")
                .careContexts(of(cc001))
                .build();
        var response = PatientLinkResponse.builder().patient(patient).build();

        StepVerifier.create(link.verifyLinkToken(patientId, patientLinkRequest))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void shouldCallGatewayAfterAddingCareContexts() {
        var linkRequest = linkRequest().build();
        var hipAction = linkHipAction().build();
        when(linkTokenVerifier.getHipIdFromToken(linkRequest.getLink().getAccessToken())).thenReturn(Mono.just(hipAction.getHipId()));
        when(linkTokenVerifier.validateSession(linkRequest.getLink().getAccessToken())).thenReturn(Mono.just(hipAction));
        when(linkTokenVerifier.validateHipAction(hipAction, HIP_INITIATED_ACTION_LINK)).thenReturn(Mono.just(hipAction));
        when(linkRepository.insertToLink(eq(hipAction.getHipId()), eq(hipAction.getPatientId()),
                eq(hipAction.getSessionId()),
                any(PatientRepresentation.class),
                eq(Constants.LINK_INITIATOR_HIP))).thenReturn(Mono.empty());
        when(linkRepository.incrementHipActionCounter(hipAction.getSessionId())).thenReturn(Mono.empty());
        when(linkServiceClient.sendLinkResponseToGateway(linkResponseArgumentCaptor.capture(),
                eq(hipAction.getHipId()))).thenReturn(Mono.empty());

        var producer = link.addCareContexts(linkRequest);
        StepVerifier.create(producer)
                .verifyComplete();

        verify(linkTokenVerifier).validateSession(linkRequest.getLink().getAccessToken());
        verify(linkRepository).insertToLink(
                eq(hipAction.getHipId()),
                eq(hipAction.getPatientId()),
                eq(hipAction.getSessionId()),
                any(PatientRepresentation.class),
                eq(Constants.LINK_INITIATOR_HIP));
        verify(linkServiceClient).sendLinkResponseToGateway(linkResponseArgumentCaptor.capture(),
                eq(hipAction.getHipId()));
        verify(linkRepository).incrementHipActionCounter(eq(hipAction.getSessionId()));
        assertThat(linkResponseArgumentCaptor.getValue().getAcknowledgement()).isNotNull();
        assertThat(linkResponseArgumentCaptor.getValue().getError()).isNull();
    }

    @Test
    void shouldCallGatewayWithErrorMessageWhenHIPIdIsInvalid() {
        var linkRequest = linkRequest().build();
        var hipAction = linkHipAction().build();
        when(linkTokenVerifier.getHipIdFromToken(linkRequest.getLink().getAccessToken())).thenReturn(Mono.just(hipAction.getHipId()));
        when(linkTokenVerifier.validateSession(linkRequest.getLink().getAccessToken())).thenReturn(Mono.error(ClientError.invalidToken(ERROR_TOKEN_IS_INVALID_OR_EXPIRED)));
        when(linkServiceClient.sendLinkResponseToGateway(linkResponseArgumentCaptor.capture(),
                eq(hipAction.getHipId()))).thenReturn(Mono.empty());

        var producer = link.addCareContexts(linkRequest);
        StepVerifier.create(producer)
                .verifyComplete();

        verify(linkTokenVerifier).validateSession(linkRequest.getLink().getAccessToken());
        verify(linkServiceClient).sendLinkResponseToGateway(linkResponseArgumentCaptor.capture(),
                eq(hipAction.getHipId()));
        assertThat(linkResponseArgumentCaptor.getValue().getError()).isNotNull();
        assertThat(linkResponseArgumentCaptor.getValue().getAcknowledgement()).isNull();
    }

    @Test
    void shouldThrowErrorMessageWhenTokenIsInvalid() {
        var linkRequest = linkRequest().build();
        var hipAction = linkHipAction().build();
        when(linkTokenVerifier.getHipIdFromToken(linkRequest.getLink().getAccessToken())).thenReturn(Mono.just(hipAction.getHipId()));
        when(linkTokenVerifier.validateSession(linkRequest.getLink().getAccessToken())).thenReturn(Mono.error(ClientError.invalidToken(ERROR_INVALID_TOKEN_REQUIRED_ATTRIBUTES_NOT_PRESENT)));
        when(linkServiceClient.sendLinkResponseToGateway(linkResponseArgumentCaptor.capture(),
                eq(hipAction.getHipId()))).thenReturn(Mono.empty());

        var producer = link.addCareContexts(linkRequest);
        StepVerifier.create(producer).verifyComplete();
        verify(linkTokenVerifier).validateSession(linkRequest.getLink().getAccessToken());
        verify(linkServiceClient).sendLinkResponseToGateway(linkResponseArgumentCaptor.capture(),
                eq(hipAction.getHipId()));
        assertThat(linkResponseArgumentCaptor.getValue().getError()).isNotNull();
        assertThat(linkResponseArgumentCaptor.getValue().getAcknowledgement()).isNull();
    }

    @Test
    void shouldThrowErrorWhenHipIsNotPresent() {
        var linkRequest = linkRequest().build();
        when(linkTokenVerifier.getHipIdFromToken(linkRequest.getLink().getAccessToken())).thenReturn(Mono.error(ClientError.invalidToken(ERROR_INVALID_TOKEN_NO_HIP_ID)));
        var producer = link.addCareContexts(linkRequest);
        StepVerifier.create(producer)
                .verifyErrorMatches(throwable -> throwable instanceof ClientError &&
                        ((ClientError) throwable).getHttpStatus() == HttpStatus.BAD_REQUEST);

    }
}
