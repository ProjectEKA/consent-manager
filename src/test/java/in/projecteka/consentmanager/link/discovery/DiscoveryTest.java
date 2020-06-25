package in.projecteka.consentmanager.link.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.DiscoveryServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.properties.LinkServiceProperties;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.link.discovery.model.patient.request.PatientIdentifier;
import in.projecteka.consentmanager.link.discovery.model.patient.request.PatientIdentifierType;
import in.projecteka.consentmanager.link.discovery.model.patient.response.DiscoveryResult;
import in.projecteka.consentmanager.link.discovery.model.patient.response.GatewayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.UUID;

import static in.projecteka.consentmanager.link.discovery.TestBuilders.address;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.discoveryResponse;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.identifier;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.patientIdentifierBuilder;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.patientInResponse;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.provider;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.string;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.telecom;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.user;
import static java.util.List.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

public class DiscoveryTest {

    @Mock
    CentralRegistry centralRegistry;

    @Mock
    UserServiceClient userServiceClient;

    @Mock
    DiscoveryServiceClient discoveryServiceClient;

    @Mock
    DiscoveryRepository discoveryRepository;

    LinkServiceProperties linkServiceProperties = new LinkServiceProperties("http://tmc.gov.in/ncg-gateway", 1000);

    @Mock
    CacheAdapter<String,String> discoveryResults;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void returnProvidersWithOfficial() {
        var discovery = new Discovery(
                userServiceClient,
                discoveryServiceClient,
                discoveryRepository,
                centralRegistry,
                linkServiceProperties,
                discoveryResults);
        var address = address().use("work").build();
        var telecommunication = telecom().use("work").build();
        var identifier = identifier().use(
                in.projecteka.consentmanager.clients.model.Identifier.IdentifierType.OFFICIAL.toString()).build();
        var provider = provider()
                .addresses(of(address))
                .telecoms(of(telecommunication))
                .identifiers(of(identifier))
                .name("Max")
                .build();
        when(centralRegistry.providersOf(eq("Max"))).thenReturn(Flux.just(provider));

        StepVerifier.create(discovery.providersFrom("Max"))
                .expectNext(Transformer.to(provider))
                .verifyComplete();
    }


    @Test
    public void shouldGetRequestAlreadyPresentError() {
        String providerId = "1";
        String userName = "1";
        var transactionId = UUID.randomUUID();
        var requestId = UUID.randomUUID();
        var discovery = new Discovery(
                userServiceClient,
                discoveryServiceClient,
                discoveryRepository,
                centralRegistry,
                linkServiceProperties,
                discoveryResults);

        when(discoveryRepository.getIfPresent(requestId)).thenReturn(Mono.just(transactionId.toString()));

        StepVerifier.create(
                discovery.patientInHIP(userName, Collections.emptyList(), providerId, transactionId, requestId)
                        .subscriberContext(context -> context.put(AUTHORIZATION, string())))
                .expectErrorMatches(error -> ((ClientError) error)
                        .getError()
                        .getError()
                        .getMessage()
                        .equals("A request with this request id already exists."))
                .verify();
    }

    @Test
    public void returnEmptyProvidersWhenOfficialIdentifierIsUnavailable() {
        var discovery = new Discovery(
                userServiceClient,
                discoveryServiceClient,
                discoveryRepository,
                centralRegistry,
                linkServiceProperties,
                discoveryResults);
        var address = address().use("work").build();
        var telecommunication = telecom().use("work").build();
        var identifier = identifier().build();
        var provider = provider()
                .addresses(of(address))
                .telecoms(of(telecommunication))
                .identifiers(of(identifier))
                .name("Max")
                .build();
        when(centralRegistry.providersOf(eq("Max"))).thenReturn(Flux.just(provider));

        StepVerifier.create(discovery.providersFrom("Max"))
                .verifyComplete();
    }

    @Test
    public void patientForGivenHIPIdAndPatientId() throws JsonProcessingException {
        var hipId = string();
        var transactionId = UUID.randomUUID();
        var requestId = UUID.randomUUID();
        var patientId = string();
        var user = user().identifier("1").name("first name").phone("+91-9999999999").build();
        PatientIdentifier ncp1008 = patientIdentifierBuilder().type(PatientIdentifierType.MR).value("NCP1008").build();
        var unverifiedIdentifiers = Collections.singletonList(ncp1008);
        UUID gatewayOnDiscoverRequestId = UUID.randomUUID();
        var gatewayResponse = GatewayResponse.builder().requestId(requestId.toString()).build();
        var patientInResponse = patientInResponse()
                .display("John Doe")
                .referenceNumber("123")
                .matchedBy(of())
                .careContexts(of())
                .build();

        DiscoveryResult discoveryResult = DiscoveryResult.builder()
                .patient(patientInResponse)
                .requestId(gatewayOnDiscoverRequestId)
                .transactionId(transactionId)
                .resp(gatewayResponse)
                .build();
        String discoveryResultInCache = new ObjectMapper().writeValueAsString(discoveryResult);
        when(discoveryRepository.getIfPresent(requestId)).thenReturn(Mono.empty());
        when(userServiceClient.userOf(eq(patientId))).thenReturn(Mono.just(user));
        when(discoveryServiceClient.requestPatientFor(any(), eq(hipId)))
                .thenReturn(Mono.just(true));
        when(discoveryResults.get(requestId.toString())).thenReturn(Mono.just(discoveryResultInCache));
        when(discoveryRepository.insert(hipId, patientId, transactionId, requestId)).thenReturn(Mono.empty());
        var discoveryResponse = discoveryResponse()
                .patient(patientInResponse)
                .transactionId(transactionId)
                .build();
        var discovery = new Discovery(userServiceClient,
                discoveryServiceClient,
                discoveryRepository,
                centralRegistry,
                linkServiceProperties,
                discoveryResults);
        StepVerifier.create(
                discovery.patientInHIP(patientId, unverifiedIdentifiers, hipId, transactionId, requestId)
                        .subscriberContext(cxt -> cxt.put(AUTHORIZATION, string())))
                .expectNext(discoveryResponse)
                .verifyComplete();
    }
}