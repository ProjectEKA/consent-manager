package in.org.projecteka.hdaf.link.discovery;

import in.org.projecteka.hdaf.clients.ClientRegistryClient;
import in.org.projecteka.hdaf.clients.HipServiceClient;
import in.org.projecteka.hdaf.clients.UserServiceClient;
import in.org.projecteka.hdaf.link.discovery.model.Address;
import in.org.projecteka.hdaf.link.discovery.model.Phone;
import in.org.projecteka.hdaf.link.discovery.model.Provider;
import in.org.projecteka.hdaf.link.discovery.model.Telecom;
import in.org.projecteka.hdaf.link.discovery.model.User;
import in.org.projecteka.hdaf.link.discovery.model.patient.request.Identifier;
import in.org.projecteka.hdaf.link.discovery.model.patient.request.Patient;
import in.org.projecteka.hdaf.link.discovery.model.patient.request.PatientRequest;
import in.org.projecteka.hdaf.link.discovery.model.patient.response.DiscoveryResponse;
import in.org.projecteka.hdaf.link.discovery.model.patient.response.PatientResponse;
import in.org.projecteka.hdaf.link.discovery.repository.DiscoveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import java.util.UUID;
import static in.org.projecteka.hdaf.link.discovery.TestBuilders.address;
import static in.org.projecteka.hdaf.link.discovery.TestBuilders.discoveryResponse;
import static in.org.projecteka.hdaf.link.discovery.TestBuilders.identifier;
import static in.org.projecteka.hdaf.link.discovery.TestBuilders.patientIdentifier;
import static in.org.projecteka.hdaf.link.discovery.TestBuilders.patientInResponse;
import static in.org.projecteka.hdaf.link.discovery.TestBuilders.patientRequest;
import static in.org.projecteka.hdaf.link.discovery.TestBuilders.patientResponse;
import static in.org.projecteka.hdaf.link.discovery.TestBuilders.provider;
import static in.org.projecteka.hdaf.link.discovery.TestBuilders.providerIdentifier;
import static in.org.projecteka.hdaf.link.discovery.TestBuilders.telecom;
import static in.org.projecteka.hdaf.link.discovery.TestBuilders.user;
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
    HipServiceClient hipServiceClient;

    @Mock
    DiscoveryRepository discoveryRepository;

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void returnProvidersWithOfficial() {
        var discovery = new Discovery(clientRegistryClient, userServiceClient, hipServiceClient, discoveryRepository);
        var address = address().use("work").build();
        var telecommunication = telecom().use("work").build();
        var identifier = identifier().use(in.org.projecteka.hdaf.link.discovery.model.Identifier.IdentifierType.OFFICIAL.toString()).build();
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
        var discovery = new Discovery(clientRegistryClient, userServiceClient, hipServiceClient, discoveryRepository);
        Address address = address().use("work").build();
        Telecom telecom = telecom().use("work").build();
        in.org.projecteka.hdaf.link.discovery.model.patient.response.Patient patientInResponse = patientInResponse()
                .display("John Doe")
                .referenceNumber("123")
                .matchedBy(of())
                .careContexts(of())
                .build();
        PatientResponse patientResponse = patientResponse().patient(patientInResponse).build();
        Phone phone = Phone.builder().countryCode("+91").number("9999999999").build();
        User user = user().identifier("1").firstName("first name").phone(phone).build();
        String hipClientUrl = "http://localhost:8001";
        Provider provider = provider()
                .addresses(of(address))
                .telecoms(of(telecom))
                .identifiers(of(providerIdentifier().system(hipClientUrl).use("official").build()))
                .name("Max")
                .build();
        Identifier identifier = patientIdentifier().type("MOBILE").value("+919999999999").build();
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
        DiscoveryResponse discoveryResponse = discoveryResponse().patient(patientResponse.getPatient()).transactionId(transactionId).build();

        when(clientRegistryClient.providerWith(eq(providerId))).thenReturn(Mono.just(provider));
        when(userServiceClient.userOf(eq(patientId))).thenReturn(Mono.just(user));
        when(hipServiceClient.patientFor(eq(patientRequest), eq(hipClientUrl))).thenReturn(Mono.just(patientResponse));
        when(discoveryRepository.insert(providerId, patientId, transactionId)).thenReturn(Mono.empty());

        StepVerifier.create(discovery.patientFor(providerId, patientId, transactionId))
                .expectNext(discoveryResponse)
                .verifyComplete();
    }

    @Test
    public void shouldGetInvalidHipErrorWhenIdentifierIsNotOfficial() {
        String providerId = "1";
        String userName = "1";
        var discovery = new Discovery(clientRegistryClient, userServiceClient, hipServiceClient, discoveryRepository);
        Address address = address().use("work").build();
        Telecom telecom = telecom().use("work").build();
        Phone phone = Phone.builder().build();
        User user = user().identifier("1").firstName("first name").phone(phone).build();
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
                .expectErrorMatches(error -> error.equals(new Throwable("Invalid HIP")));
    }

    @Test
    public void returnEmptyProvidersWhenOfficialIdentifierIsUnavailable() {
        var discovery = new Discovery(clientRegistryClient, userServiceClient, hipServiceClient, discoveryRepository);
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