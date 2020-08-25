package in.projecteka.consentmanager.link.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import in.projecteka.consentmanager.clients.DiscoveryServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.link.discovery.model.patient.request.PatientIdentifier;
import in.projecteka.consentmanager.link.discovery.model.patient.request.PatientIdentifierType;
import in.projecteka.consentmanager.link.discovery.model.patient.response.CareContext;
import in.projecteka.consentmanager.link.discovery.model.patient.response.DiscoveryResult;
import in.projecteka.consentmanager.link.discovery.model.patient.response.GatewayResponse;
import in.projecteka.consentmanager.properties.LinkServiceProperties;
import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.clients.model.ErrorCode;
import in.projecteka.library.clients.model.Identifier;
import in.projecteka.library.clients.model.PatientName;
import in.projecteka.library.clients.model.RespError;
import in.projecteka.library.common.CentralRegistry;
import in.projecteka.library.common.cache.CacheAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.UUID;

import static in.projecteka.consentmanager.clients.TestBuilders.address;
import static in.projecteka.consentmanager.clients.TestBuilders.identifier;
import static in.projecteka.consentmanager.clients.TestBuilders.patientInResponse;
import static in.projecteka.consentmanager.clients.TestBuilders.provider;
import static in.projecteka.consentmanager.clients.TestBuilders.telecom;
import static in.projecteka.consentmanager.common.TestBuilders.OBJECT_MAPPER;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.discoveryResponse;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.discoveryResult;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.patient;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.patientIdentifierBuilder;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.string;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.user;
import static in.projecteka.library.common.Serializer.from;
import static java.util.Arrays.asList;
import static java.util.List.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

class DiscoveryTest {

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
    CacheAdapter<String, String> discoveryResults;

    private Discovery discovery;

    @BeforeEach
    void setUp() {
        initMocks(this);
        discovery = new Discovery(
                userServiceClient,
                discoveryServiceClient,
                discoveryRepository,
                centralRegistry,
                linkServiceProperties,
                discoveryResults);
    }

    @Test
    void returnProvidersWithOfficial() {
        var address = address().use("work").build();
        var telecommunication = telecom().use("work").build();
        var identifier = identifier().use(Identifier.IdentifierType.OFFICIAL.toString()).build();
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
    void shouldGetRequestAlreadyPresentError() {
        String providerId = "1";
        String userName = "1";
        var transactionId = UUID.randomUUID();
        var requestId = UUID.randomUUID();

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
    void returnEmptyProvidersWhenOfficialIdentifierIsUnavailable() {
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
    void patientForGivenHIPIdAndPatientId() throws JsonProcessingException {
        var hipId = string();
        var transactionId = UUID.randomUUID();
        var requestId = UUID.randomUUID();
        var patientId = string();
        PatientName name = PatientName.builder().first("first name").middle(null).last(null).build();
        var user = user().identifier("1").name(name).phone("+91-9999999999").build();
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
        String discoveryResultInCache = OBJECT_MAPPER.writeValueAsString(discoveryResult);
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
        StepVerifier.create(
                discovery.patientInHIP(patientId, unverifiedIdentifiers, hipId, transactionId, requestId)
                        .subscriberContext(cxt -> cxt.put(AUTHORIZATION, string())))
                .expectNext(discoveryResponse)
                .verifyComplete();
    }

    @Test
    void shouldGiveErrorIfThereWasErrorInPatientDiscovery() throws JsonProcessingException {
        var hipId = string();
        var transactionId = UUID.randomUUID();
        var requestId = UUID.randomUUID();
        var patientId = string();
        PatientName name = PatientName.builder().first("first name").middle(null).last(null).build();
        var user = user().identifier("1").name(name).phone("+91-9999999999").build();
        PatientIdentifier ncp1008 = patientIdentifierBuilder().type(PatientIdentifierType.MR).value("NCP1008").build();
        var unverifiedIdentifiers = Collections.singletonList(ncp1008);

        DiscoveryResult discoveryResult = DiscoveryResult.builder()
                .error(RespError
                        .builder()
                        .code(ErrorCode.INVALID_DISCOVERY.getValue())
                        .message("Patient Not Found")
                        .build())
                .build();

        String errorInDiscovery = OBJECT_MAPPER.writeValueAsString(discoveryResult);
        when(discoveryRepository.getIfPresent(requestId)).thenReturn(Mono.empty());
        when(userServiceClient.userOf(eq(patientId))).thenReturn(Mono.just(user));
        when(discoveryServiceClient.requestPatientFor(any(), eq(hipId)))
                .thenReturn(Mono.just(true));
        when(discoveryResults.get(requestId.toString())).thenReturn(Mono.just(errorInDiscovery));
        when(discoveryRepository.insert(hipId, patientId, transactionId, requestId)).thenReturn(Mono.empty());

        StepVerifier.create(
                discovery.patientInHIP(patientId, unverifiedIdentifiers, hipId, transactionId, requestId)
                        .subscriberContext(cxt -> cxt.put(AUTHORIZATION, string())))
                .expectErrorMatches(
                        err -> err instanceof ClientError &&
                                ((ClientError) err).getError().getError().getMessage().equals("Patient Not Found")
                ).verify();
    }

    @Test
    void shouldPutDiscoveryResultInTheCache() {
        DiscoveryResult discoveryResult = discoveryResult().build();

        when(discoveryResults.put(anyString(), anyString())).thenReturn(Mono.empty());
        StepVerifier.create(
                discovery.onDiscoverPatientCareContexts(discoveryResult)
        ).verifyComplete();

        verify(discoveryResults, times(1)).put(discoveryResult.getResp().getRequestId(), from(discoveryResult));
    }

    @Test
    void shouldGiveErrorWhenInvalidRequestId() {
        DiscoveryResult discoveryResult = discoveryResult().resp(null).build();

        when(discoveryResults.put(anyString(), anyString())).thenReturn(Mono.empty());
        StepVerifier.create(
                discovery.onDiscoverPatientCareContexts(discoveryResult)
        ).expectError(ClientError.class).verify();

        verify(discoveryResults, never()).put(anyString(), anyString());
    }

    @Test
    void shouldGiveErrorWhenDiscoveryDoesNotHavePatient() {
        DiscoveryResult discoveryResult = discoveryResult().patient(null).build();
        DiscoveryResult errorDiscovery = DiscoveryResult.builder()
                .error(RespError.builder()
                        .code(ErrorCode.INVALID_DISCOVERY.getValue())
                        .message("Could not find the user details")
                        .build())
                .build();


        when(discoveryResults.put(anyString(), anyString())).thenReturn(Mono.empty());
        StepVerifier.create(
                discovery.onDiscoverPatientCareContexts(discoveryResult)
        ).expectErrorMatches(err -> err instanceof ClientError &&
                ((ClientError) err).getError().getError().getMessage().equals("Patient Details not found")).verify();

        verify(discoveryResults, times(1)).put(discoveryResult.getResp().getRequestId(), from(errorDiscovery));
    }

    @Test
    void shouldGiveErrorWhenDiscoveryPatientReferenceIsEmpty() {
        DiscoveryResult discoveryResult = discoveryResult().patient(
                patient().referenceNumber(null).build())
                .build();

        DiscoveryResult errorDiscovery = DiscoveryResult.builder()
                .error(RespError.builder()
                        .code(ErrorCode.INVALID_DISCOVERY.getValue())
                        .message("Could not find the user details")
                        .build())
                .build();

        when(discoveryResults.put(anyString(), anyString())).thenReturn(Mono.empty());
        StepVerifier.create(
                discovery.onDiscoverPatientCareContexts(discoveryResult)
        ).expectErrorMatches(err -> err instanceof ClientError &&
                ((ClientError) err).getError().getError().getMessage().equals("Patient Reference should not be blank")).verify();

        verify(discoveryResults, times(1)).put(discoveryResult.getResp().getRequestId(), from(errorDiscovery));
    }

    @Test
    void shouldGiveErrorWhenDiscoveryCareContextIsNull() {
        DiscoveryResult discoveryResult = discoveryResult().patient(
                patient().careContexts(null).build())
                .build();

        DiscoveryResult errorDiscovery = DiscoveryResult.builder()
                .error(RespError.builder()
                        .code(ErrorCode.INVALID_DISCOVERY.getValue())
                        .message("Could not find the user details")
                        .build())
                .build();

        when(discoveryResults.put(anyString(), anyString())).thenReturn(Mono.empty());
        StepVerifier.create(
                discovery.onDiscoverPatientCareContexts(discoveryResult)
        ).expectErrorMatches(err -> err instanceof ClientError &&
                ((ClientError) err).getError().getError().getMessage().equals("Care contexts should not be null")).verify();

        verify(discoveryResults, times(1)).put(discoveryResult.getResp().getRequestId(), from(errorDiscovery));
    }

    @Test
    void shouldGiveErrorWhenOneOfTheCareContextsReferencesIsEmpty() {
        CareContext careContext1 = CareContext.builder().referenceNumber("ABC123").build();
        CareContext careContext2 = CareContext.builder().referenceNumber("").build();

        DiscoveryResult discoveryResult = discoveryResult().patient(
                patient().careContexts(asList(careContext1, careContext2)).build())
                .build();

        DiscoveryResult errorDiscovery = DiscoveryResult.builder()
                .error(RespError.builder()
                        .code(ErrorCode.INVALID_DISCOVERY.getValue())
                        .message("Could not find the user details")
                        .build())
                .build();

        when(discoveryResults.put(anyString(), anyString())).thenReturn(Mono.empty());
        StepVerifier.create(
                discovery.onDiscoverPatientCareContexts(discoveryResult)
        ).expectErrorMatches(err -> err instanceof ClientError &&
                ((ClientError) err).getError().getError().getMessage().equals("All the care contexts should have valid references")).verify();

        verify(discoveryResults, times(1)).put(discoveryResult.getResp().getRequestId(), from(errorDiscovery));
    }

    @Test
    void shouldNotThrowErrorWhenCareContextIsEmptyList() {
        DiscoveryResult discoveryResult = discoveryResult().patient(
                patient().careContexts(Collections.emptyList()).build())
                .build();

        when(discoveryResults.put(anyString(), anyString())).thenReturn(Mono.empty());
        StepVerifier.create(
                discovery.onDiscoverPatientCareContexts(discoveryResult)
        ).verifyComplete();

        verify(discoveryResults, times(1)).put(discoveryResult.getResp().getRequestId(), from(discoveryResult));
    }
}