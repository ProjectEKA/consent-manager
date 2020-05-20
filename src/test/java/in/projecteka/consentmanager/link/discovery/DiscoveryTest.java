package in.projecteka.consentmanager.link.discovery;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.DiscoveryServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.model.Address;
import in.projecteka.consentmanager.clients.model.Provider;
import in.projecteka.consentmanager.clients.model.Telecom;
import in.projecteka.consentmanager.clients.model.User;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.link.discovery.model.patient.request.Patient;
import in.projecteka.consentmanager.link.discovery.model.patient.request.PatientIdentifier;
import in.projecteka.consentmanager.link.discovery.model.patient.request.PatientIdentifierType;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

import static in.projecteka.consentmanager.link.discovery.TestBuilders.address;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.discoveryResponse;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.identifier;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.patientIdentifier;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.patientIdentifierBuilder;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.patientInResponse;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.patientRequest;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.patientResponse;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.provider;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.providerIdentifier;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.string;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.telecom;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.user;
import static java.util.List.of;
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

    @Mock
    GatewayServiceProperties gatewayServiceProperties;

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
                gatewayServiceProperties,
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

    public void patientForGivenProviderIdAndPatientId() {
        var providerId = string();
        var transactionId = UUID.randomUUID();
        var requestId = UUID.randomUUID();
        var patientId = string();
        var discovery = new Discovery(userServiceClient,
                                    discoveryServiceClient,
                                    discoveryRepository,
                                    centralRegistry,
                                    gatewayServiceProperties,
                                    discoveryResults);
        var address = address().use("work").build();
        var telecom = telecom().use("work").build();
        var patientInResponse = patientInResponse()
                .display("John Doe")
                .referenceNumber("123")
                .matchedBy(of())
                .careContexts(of())
                .build();
        var patientResponse = patientResponse().patient(patientInResponse).build();
        var user = user().identifier("1").name("first name").phone("+91-9999999999").build();
        var hipClientUrl = "http://localhost:8001";
        var provider = provider()
                .addresses(of(address))
                .telecoms(of(telecom))
                .identifiers(of(providerIdentifier().system(hipClientUrl).use("official").build()))
                .name("Max")
                .build();
        var identifier = patientIdentifier().type("MOBILE").value("+91-9999999999").build();
        PatientIdentifier ncp1008 = patientIdentifierBuilder().type(PatientIdentifierType.MR).value("NCP1008").build();
        var unverifiedIdentifiers = Collections.singletonList(ncp1008);
        var unverifiedIds = unverifiedIdentifiers.stream().map(patientIdentifier ->
                in.projecteka.consentmanager.link.discovery.model.patient.request.Identifier.builder()
                        .type(patientIdentifier.getType().toString())
                        .value(patientIdentifier.getValue())
                        .build()).collect(Collectors.toList());
        var patient = Patient.builder()
                .id(user.getIdentifier())
                .name(user.getName())
                .gender(user.getGender())
                .yearOfBirth(user.getYearOfBirth())
                .verifiedIdentifiers(of(identifier))
                .unverifiedIdentifiers(unverifiedIds)
                .build();
        var patientRequest = patientRequest().patient(patient).requestId(transactionId).build();
        var discoveryResponse = discoveryResponse()
                .patient(patientResponse.getPatient())
                .transactionId(transactionId)
                .build();

        when(centralRegistry.providerWith(eq(providerId))).thenReturn(Mono.just(provider));
        when(userServiceClient.userOf(eq(patientId))).thenReturn(Mono.just(user));
        when(discoveryServiceClient.patientFor(eq(patientRequest), eq(hipClientUrl), eq(providerId)))
                .thenReturn(Mono.just(patientResponse));
        when(discoveryRepository.insert(providerId, patientId, transactionId, requestId)).thenReturn(Mono.empty());
        when(discoveryRepository.getIfPresent(requestId)).thenReturn(Mono.empty());

        StepVerifier.create(
                discovery.patientFor(patientId, unverifiedIdentifiers, providerId, transactionId, requestId)
                        .subscriberContext(cxt -> cxt.put(AUTHORIZATION, string())))
                .expectNext(discoveryResponse)
                .verifyComplete();
    }

    @Test
    public void shouldGetInvalidHipErrorWhenIdentifierIsNotOfficial() {
        String providerId = "1";
        String userName = "1";
        var transactionId = UUID.randomUUID();
        var requestId = UUID.randomUUID();
        var discovery = new Discovery(
                userServiceClient,
                discoveryServiceClient,
                discoveryRepository,
                centralRegistry,
                gatewayServiceProperties,
                discoveryResults);
        Address address = address().use("work").build();
        Telecom telecom = telecom().use("work").build();
        User user = user().identifier("1").name("first name").build();
        String hipClientUrl = "http://localhost:8001";
        Provider provider = provider()
                .addresses(of(address))
                .telecoms(of(telecom))
                .identifiers(of(providerIdentifier().system(hipClientUrl).use("random").build()))
                .name("Max")
                .build();

        when(centralRegistry.providerWith(eq(providerId))).thenReturn(Mono.just(provider));
        when(userServiceClient.userOf(eq(userName))).thenReturn(Mono.just(user));
        when(discoveryRepository.getIfPresent(requestId)).thenReturn(Mono.empty());

        StepVerifier.create(
                discovery.patientFor(userName, Collections.emptyList(), providerId, transactionId, requestId)
                        .subscriberContext(context -> context.put(AUTHORIZATION, string())))
                .expectErrorMatches(error -> ((ClientError) error)
                        .getError()
                        .getError()
                        .getMessage()
                        .equals("Cannot process the request at the moment, please try later."))
                .verify();
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
                gatewayServiceProperties,
                discoveryResults);

        when(discoveryRepository.getIfPresent(requestId)).thenReturn(Mono.just(transactionId.toString()));

        StepVerifier.create(
                discovery.patientFor(userName, Collections.emptyList(), providerId, transactionId, requestId)
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
                gatewayServiceProperties,
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

}