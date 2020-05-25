package in.projecteka.consentmanager.link.link;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.LinkServiceClient;
import in.projecteka.consentmanager.clients.model.CareContextRepresentation;
import in.projecteka.consentmanager.clients.model.Identifier;
import in.projecteka.consentmanager.clients.model.PatientLinkReferenceResponse;
import in.projecteka.consentmanager.clients.model.PatientLinkRequest;
import in.projecteka.consentmanager.clients.model.PatientLinkResponse;
import in.projecteka.consentmanager.clients.model.PatientRepresentation;
import in.projecteka.consentmanager.clients.properties.LinkServiceProperties;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.link.link.model.Hip;
import in.projecteka.consentmanager.link.link.model.Links;
import in.projecteka.consentmanager.link.link.model.PatientLinkReferenceRequest;
import in.projecteka.consentmanager.link.link.model.PatientLinksResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static in.projecteka.consentmanager.link.link.TestBuilders.address;
import static in.projecteka.consentmanager.link.link.TestBuilders.identifier;
import static in.projecteka.consentmanager.link.link.TestBuilders.links;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientLinkReferenceRequest;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientLinkReferenceResponse;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientLinkRequest;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientLinks;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientRepresentation;
import static in.projecteka.consentmanager.link.link.TestBuilders.provider;
import static in.projecteka.consentmanager.link.link.TestBuilders.string;
import static in.projecteka.consentmanager.link.link.TestBuilders.telecom;
import static in.projecteka.consentmanager.link.link.Transformer.toHIPPatient;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class LinkTest {

    @Mock
    private CentralRegistry clientRegistryClient;

    @Mock
    private LinkServiceClient linkServiceClient;

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private CacheAdapter<String, String> linkResults;

    @Mock
    private CacheAdapter<String,String> patientLinkReferenceResults;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void createsLinkReference() {
        LinkServiceProperties linkServiceProperties = new LinkServiceProperties("http://tmc.gov.in/ncg-gateway", 1000);
        var link = new Link(linkServiceClient, linkRepository, clientRegistryClient, linkServiceProperties, linkResults, patientLinkReferenceResults);
        var address = address().use("work").build();
        var telecommunication = telecom().use("work").build();
        String providerUrl = "http://localhost:8001";
        var identifier = identifier().use(Identifier.IdentifierType.OFFICIAL.toString()).system(providerUrl).build();
        var provider = provider()
                .addresses(of(address))
                .telecoms(of(telecommunication))
                .identifiers(of(identifier))
                .name("Max")
                .build();
        PatientLinkReferenceResponse patientLinkReferenceResponse = patientLinkReferenceResponse().build();
        String patientId = "patient";
        PatientLinkReferenceRequest patientLinkReferenceRequest = patientLinkReferenceRequest().build();
        var patientLinkReferenceRequestForHIP = new in.projecteka.consentmanager.clients.model.PatientLinkReferenceRequest(
                patientLinkReferenceRequest.getRequestId().toString(),
                patientLinkReferenceRequest.getTransactionId(),
                toHIPPatient(patientId, patientLinkReferenceRequest.getPatient()));
        String hipId = "10000005";
        var token = string();
        patientLinkReferenceResponse.setTransactionId(patientLinkReferenceRequest.getTransactionId());

        when(clientRegistryClient.authenticate()).thenReturn(Mono.just(token));
        when(linkServiceClient.linkPatientEnquiry(patientLinkReferenceRequestForHIP, providerUrl, token))
                .thenReturn(Mono.just(patientLinkReferenceResponse));
        when(clientRegistryClient.providerWith(eq(hipId))).thenReturn(Mono.just(provider));
        when(linkRepository.getHIPIdFromDiscovery(patientLinkReferenceRequest.getTransactionId()))
                .thenReturn(Mono.just(hipId));
        when(linkRepository.insertToLinkReference(patientLinkReferenceResponse, hipId,
                patientLinkReferenceRequest.getRequestId())).thenReturn(Mono.empty());
        when(linkRepository.selectLinkReference(patientLinkReferenceRequest.getRequestId()))
                .thenReturn(Mono.empty());

        StepVerifier.create(link.patientWith(patientId, patientLinkReferenceRequest))
                .expectNext(patientLinkReferenceResponse)
                .verifyComplete();
    }

    @Test
    public void shouldGetSystemUrlForOfficialIdentifier() {
        var token = string();
        LinkServiceProperties linkServiceProperties = new LinkServiceProperties("http://tmc.gov.in/ncg-gateway", 1000);
        var link = new Link(linkServiceClient, linkRepository, clientRegistryClient, linkServiceProperties, linkResults, patientLinkReferenceResults)
        var address = address().use("work").build();
        var telecommunication = telecom().use("work").build();
        String providerUrl = "http://localhost:8001";
        var identifier1 =
                identifier().use("personal").system("personalUrl").build();
        var identifier2 =
                identifier().use(Identifier.IdentifierType.OFFICIAL.toString()).system(providerUrl).build();
        var provider =
                provider()
                        .addresses(of(address))
                        .telecoms(of(telecommunication))
                        .identifiers(of(identifier1, identifier2))
                        .name("Max")
                        .build();

        String hipId = "10000005";
        when(clientRegistryClient.authenticate()).thenReturn(Mono.just(token));
        when(clientRegistryClient.providerWith(eq(hipId))).thenReturn(Mono.just(provider));
        PatientLinkReferenceResponse patientLinkReferenceResponse =
                PatientLinkReferenceResponse.builder().build();
        String patientId = "patient";
        PatientLinkReferenceRequest patientLinkReferenceRequest = patientLinkReferenceRequest().build();
        var patientLinkReferenceRequestForHIP =
                new in.projecteka.consentmanager.clients.model.PatientLinkReferenceRequest(
                        patientLinkReferenceRequest.getRequestId().toString(),
                        patientLinkReferenceRequest.getTransactionId(),
                        toHIPPatient(patientId, patientLinkReferenceRequest.getPatient()));

        when(linkServiceClient.linkPatientEnquiry(patientLinkReferenceRequestForHIP, providerUrl, token))
                .thenReturn(Mono.just(patientLinkReferenceResponse));
        when(linkRepository.getHIPIdFromDiscovery(patientLinkReferenceRequest.getTransactionId()))
                .thenReturn(Mono.just(hipId));
        when(linkRepository.insertToLinkReference(patientLinkReferenceResponse, hipId,
                patientLinkReferenceRequest.getRequestId())).thenReturn(Mono.empty());
        when(linkRepository.selectLinkReference(patientLinkReferenceRequest.getRequestId()))
                .thenReturn(Mono.empty());

        StepVerifier.create(link.patientWith(patientId, patientLinkReferenceRequest))
                .expectNext(patientLinkReferenceResponse)
                .verifyComplete();
        verify(linkServiceClient).linkPatientEnquiry(patientLinkReferenceRequestForHIP, providerUrl, token);
    }

    @Test
    public void shouldGetErrorWhenProviderUrlIsEmpty() {
        LinkServiceProperties linkServiceProperties = new LinkServiceProperties("http://tmc.gov.in/ncg-gateway", 1000);
        var link = new Link(linkServiceClient, linkRepository, clientRegistryClient, linkServiceProperties, linkResults, patientLinkReferenceResults);
        var address = address().use("work").build();
        var telecommunication = telecom().use("work").build();
        var provider =
                provider()
                        .addresses(of(address))
                        .telecoms(of(telecommunication))
                        .identifiers(Collections.emptyList())
                        .name("Max")
                        .build();
        String patientId = "patient";
        PatientLinkReferenceRequest patientLinkReferenceRequest = patientLinkReferenceRequest().build();
        ClientError clientError = ClientError.unableToConnectToProvider();

        String hipId = "10000005";
        when(clientRegistryClient.providerWith(eq(hipId))).thenReturn(Mono.just(provider));
        when(linkRepository.getHIPIdFromDiscovery(patientLinkReferenceRequest.getTransactionId()))
                .thenReturn(Mono.just(hipId));
        when(linkRepository.selectLinkReference(patientLinkReferenceRequest.getRequestId()))
                .thenReturn(Mono.empty());

        StepVerifier.create(link.patientWith(patientId, patientLinkReferenceRequest))
                .expectErrorSatisfies(error -> {
                    assertThat(((ClientError) error).getError()).isEqualTo(clientError.getError());
                    assertThat(((ClientError) error).getHttpStatus()).isEqualTo(clientError.getHttpStatus());
                })
                .verify();
    }

    @Test
    public void linksPatientsCareContexts() {
        LinkServiceProperties linkServiceProperties = new LinkServiceProperties("http://tmc.gov.in/ncg-gateway", 1000);
        var link = new Link(linkServiceClient, linkRepository, clientRegistryClient, linkServiceProperties, linkResults, patientLinkReferenceResults);
        var address = address().use("work").build();
        var telecommunication = telecom().use("work").build();
        String providerUrl = "http://localhost:8001";
        var identifier =
                identifier().use(Identifier.IdentifierType.OFFICIAL.toString()).system(providerUrl).build();
        var provider =
                provider()
                        .addresses(of(address))
                        .telecoms(of(telecommunication))
                        .identifiers(of(identifier))
                        .name("Max")
                        .build();
        PatientLinkResponse patientLinkResponse =
                PatientLinkResponse.builder().build();
        PatientLinkRequest patientLinkRequest = patientLinkRequest().build();
        String linkRefNumber = "link-ref-num";
        String patientId = "patient";

        var token = string();
        when(clientRegistryClient.authenticate()).thenReturn(Mono.just(token));
        when(linkServiceClient.linkPatientConfirmation(linkRefNumber, patientLinkRequest, providerUrl, token))
                .thenReturn(Mono.just(patientLinkResponse));
        String hipId = "10000005";
        when(clientRegistryClient.providerWith(eq(hipId))).thenReturn(Mono.just(provider));
        String transactionId = "transactionId";
        when(linkRepository.getTransactionIdFromLinkReference(linkRefNumber)).thenReturn(Mono.just(transactionId));
        when(linkRepository.getHIPIdFromDiscovery(transactionId)).thenReturn(Mono.just(hipId));
        when(linkRepository.insertToLink(hipId, patientId, linkRefNumber, patientLinkResponse.getPatient())).thenReturn(Mono.empty());

        StepVerifier.create(link.verifyToken(linkRefNumber, patientLinkRequest, patientId))
                .expectNext(patientLinkResponse)
                .verifyComplete();
    }

    @Test
    public void shouldReturnLinkedCareContext() {
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
        var patientLinks = patientLinks()
                .id(patientId)
                .links(listOfLinks)
                .build();
        var provider =
                provider().build();

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
        when(clientRegistryClient.providerWith(eq(hipId))).thenReturn(Mono.just(provider));
        LinkServiceProperties linkServiceProperties = new LinkServiceProperties("http://tmc.gov.in/ncg-gateway", 1000);
        var link = new Link(linkServiceClient, linkRepository, clientRegistryClient, linkServiceProperties, linkResults, patientLinkReferenceResults);

        StepVerifier.create(link.getLinkedCareContexts(patientId))
                .expectNext(patientLinksResponse)
                .verifyComplete();
    }

    @Test
    public void confirmLinkCareContexts() {
        LinkServiceProperties linkServiceProperties = new LinkServiceProperties("http://tmc.gov.in/ncg-gateway", 1000);
        var link = new Link(linkServiceClient, linkRepository, clientRegistryClient, linkServiceProperties, linkResults, patientLinkReferenceResults);
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

        String transactionId = "transactionId";
        when(linkRepository.getTransactionIdFromLinkReference(patientLinkRequest.getLinkRefNumber())).thenReturn(Mono.just(transactionId));
        when(linkRepository.getHIPIdFromDiscovery(transactionId)).thenReturn(Mono.just(hipId));
        when(linkServiceClient.confirmPatientLink(any(), eq(hipId))).thenReturn(Mono.just(Boolean.TRUE));
        when(linkResults.get(any())).thenReturn(Mono.just(linkConfirmationResult));
        when(linkRepository.insertToLink(eq(hipId), eq(patientId), eq(patientLinkRequest.getLinkRefNumber()), any())).thenReturn(Mono.empty());

        CareContextRepresentation cc001 = CareContextRepresentation.builder().referenceNumber("CC001").display("Episode 001").build();
        PatientRepresentation patient = PatientRepresentation.builder().referenceNumber("HID-001").display("Patient with HID 001").careContexts(Arrays.asList(cc001)).build();
        PatientLinkResponse response = PatientLinkResponse.builder().patient(patient).build();
        StepVerifier.create(link.verifyLinkToken(patientId, patientLinkRequest))
                .expectNext(response)
                .verifyComplete();
    }
}
