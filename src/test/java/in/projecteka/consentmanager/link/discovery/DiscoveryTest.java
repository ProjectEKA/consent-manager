package in.projecteka.consentmanager.link.discovery;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.DiscoveryServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.model.Address;
import in.projecteka.consentmanager.clients.model.Provider;
import in.projecteka.consentmanager.clients.model.Telecom;
import in.projecteka.consentmanager.clients.model.User;
import in.projecteka.consentmanager.link.discovery.model.patient.request.Identifier;
import in.projecteka.consentmanager.link.discovery.model.patient.request.Patient;
import in.projecteka.consentmanager.link.discovery.model.patient.request.PatientRequest;
import in.projecteka.consentmanager.link.discovery.model.patient.response.DiscoveryResponse;
import in.projecteka.consentmanager.link.discovery.model.patient.response.PatientResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static in.projecteka.consentmanager.link.discovery.TestBuilders.address;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.discoveryResponse;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.identifier;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.patientIdentifier;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.patientInResponse;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.patientRequest;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.patientResponse;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.provider;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.providerIdentifier;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.telecom;
import static in.projecteka.consentmanager.link.discovery.TestBuilders.user;
import static java.util.List.of;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class DiscoveryTest {

    @Mock
    ClientRegistryClient clientRegistryClient;

    @Mock
    UserServiceClient userServiceClient;

    @Mock
    DiscoveryServiceClient discoveryServiceClient;

    @Mock
    DiscoveryRepository discoveryRepository;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void returnProvidersWithOfficial() {
        var discovery = new Discovery(
                clientRegistryClient,
                userServiceClient,
                discoveryServiceClient,
                discoveryRepository);
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
        when(clientRegistryClient.providersOf(eq("Max"))).thenReturn(Flux.just(provider));

        StepVerifier.create(discovery.providersFrom("Max"))
                .expectNext(Transformer.to(provider))
                .verifyComplete();
    }

    @Test
    public void patientForGivenProviderIdAndPatientId() {
        String providerId = "1";
        String transactionId = "transaction-id";
        String patientId = "1";
        var discovery = new Discovery(
                clientRegistryClient,
                userServiceClient,
                discoveryServiceClient,
                discoveryRepository);
        Address address = address().use("work").build();
        Telecom telecom = telecom().use("work").build();
        in.projecteka.consentmanager.link.discovery.model.patient.response.Patient patientInResponse = patientInResponse()
                .display("John Doe")
                .referenceNumber("123")
                .matchedBy(of())
                .careContexts(of())
                .build();
        PatientResponse patientResponse = patientResponse().patient(patientInResponse).build();
        User user = user().identifier("1").firstName("first name").phone("+91-9999999999").build();
        String hipClientUrl = "http://localhost:8001";
        Provider provider = provider()
                .addresses(of(address))
                .telecoms(of(telecom))
                .identifiers(of(providerIdentifier().system(hipClientUrl).use("official").build()))
                .name("Max")
                .build();
        Identifier identifier = patientIdentifier().type("MOBILE").value("+91-9999999999").build();
        Patient patient = Patient.builder()
                .id(user.getIdentifier())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .verifiedIdentifiers(of(identifier))
                .unVerifiedIdentifiers(of())
                .build();
        PatientRequest patientRequest = patientRequest().patient(patient).transactionId(transactionId).build();
        DiscoveryResponse discoveryResponse = discoveryResponse()
                .patient(patientResponse.getPatient())
                .transactionId(transactionId)
                .build();

        when(clientRegistryClient.providerWith(eq(providerId))).thenReturn(Mono.just(provider));
        when(userServiceClient.userOf(eq(patientId))).thenReturn(Mono.just(user));
        when(discoveryServiceClient.patientFor(eq(patientRequest), eq(hipClientUrl)))
                .thenReturn(Mono.just(patientResponse));
        when(discoveryRepository.insert(providerId, patientId, transactionId)).thenReturn(Mono.empty());

        StepVerifier.create(discovery.patientFor(providerId, patientId, transactionId))
                .expectNext(discoveryResponse)
                .verifyComplete();
    }

    @Test
    public void shouldGetInvalidHipErrorWhenIdentifierIsNotOfficial() {
        String providerId = "1";
        String userName = "1";
        var discovery = new Discovery(
                clientRegistryClient,
                userServiceClient,
                discoveryServiceClient,
                discoveryRepository);
        Address address = address().use("work").build();
        Telecom telecom = telecom().use("work").build();
        User user = user().identifier("1").firstName("first name").build();
        String hipClientUrl = "http://localhost:8001";
        Provider provider = provider()
                .addresses(of(address))
                .telecoms(of(telecom))
                .identifiers(of(providerIdentifier().system(hipClientUrl).use("random").build()))
                .name("Max")
                .build();

        when(clientRegistryClient.providerWith(eq(providerId))).thenReturn(Mono.just(provider));
        when(userServiceClient.userOf(eq(userName))).thenReturn(Mono.just(user));

        StepVerifier.create(discovery.patientFor(providerId, userName, UUID.randomUUID().toString()))
                .expectErrorMatches(error -> ((ClientError) error)
                        .getError()
                        .getError()
                        .getMessage()
                        .equals("Cannot process the request at the moment, please try later."))
                .verify();
    }

    @Test
    public void returnEmptyProvidersWhenOfficialIdentifierIsUnavailable() {
        var discovery = new Discovery(
                clientRegistryClient,
                userServiceClient,
                discoveryServiceClient,
                discoveryRepository);
        var address = address().use("work").build();
        var telecommunication = telecom().use("work").build();
        var identifier = identifier().build();
        var provider = provider()
                .addresses(of(address))
                .telecoms(of(telecommunication))
                .identifiers(of(identifier))
                .name("Max")
                .build();
        when(clientRegistryClient.providersOf(eq("Max"))).thenReturn(Flux.just(provider));

        StepVerifier.create(discovery.providersFrom("Max"))
                .verifyComplete();
    }
}