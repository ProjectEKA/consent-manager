package in.projecteka.consentmanager.link.link;

import in.projecteka.consentmanager.clients.LinkServiceClient;
import in.projecteka.consentmanager.clients.model.CareContextRepresentation;
import in.projecteka.consentmanager.clients.model.PatientLinkRequest;
import in.projecteka.consentmanager.clients.model.PatientLinkResponse;
import in.projecteka.consentmanager.clients.model.PatientRepresentation;
import in.projecteka.consentmanager.clients.properties.LinkServiceProperties;
import in.projecteka.consentmanager.common.ServiceAuthentication;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.link.link.model.Hip;
import in.projecteka.consentmanager.link.link.model.Links;
import in.projecteka.consentmanager.link.link.model.PatientLinksResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import static in.projecteka.consentmanager.link.link.TestBuilders.links;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientLinkRequest;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientLinks;
import static in.projecteka.consentmanager.link.link.TestBuilders.patientRepresentation;
import static in.projecteka.consentmanager.link.link.TestBuilders.provider;
import static java.util.List.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class LinkTest {

    @Mock
    private ServiceAuthentication serviceAuthentication;

    @Mock
    private LinkServiceClient linkServiceClient;

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private CacheAdapter<String, String> linkResults;

    @BeforeEach
    public void setUp() {
        initMocks(this);
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
        var patientLinks = patientLinks().id(patientId).links(listOfLinks).build();
        var provider = provider().build();
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
        LinkServiceProperties linkServiceProperties = new LinkServiceProperties("http://tmc.gov.in/ncg-gateway", 1000);
        var link = new Link(linkServiceClient,
                linkRepository,
                serviceAuthentication,
                linkServiceProperties,
                linkResults);

        StepVerifier.create(link.getLinkedCareContexts(patientId))
                .expectNext(patientLinksResponse)
                .verifyComplete();
    }

    @Test
    public void confirmLinkCareContexts() {
        LinkServiceProperties linkServiceProperties = new LinkServiceProperties("http://tmc.gov.in/ncg-gateway", 1000);
        var link = new Link(linkServiceClient,
                linkRepository,
                serviceAuthentication,
                linkServiceProperties,
                linkResults);
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
        when(linkRepository.insertToLink(eq(hipId), eq(patientId), eq(patientLinkRequest.getLinkRefNumber()), any()))
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
}
