package in.org.projecteka.hdaf.link.discovery;

import in.org.projecteka.hdaf.clients.ClientRegistryClient;
import in.org.projecteka.hdaf.clients.HipServiceClient;
import in.org.projecteka.hdaf.clients.UserServiceClient;
import in.org.projecteka.hdaf.link.discovery.model.Address;
import in.org.projecteka.hdaf.link.discovery.model.Provider;
import in.org.projecteka.hdaf.link.discovery.model.Telecom;
import in.org.projecteka.hdaf.link.discovery.model.User;
import in.org.projecteka.hdaf.link.discovery.model.patient.request.Identifier;
import in.org.projecteka.hdaf.link.discovery.model.patient.request.Patient;
import in.org.projecteka.hdaf.link.discovery.model.patient.request.PatientRequest;
import in.org.projecteka.hdaf.link.discovery.model.patient.response.PatientResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static in.org.projecteka.hdaf.link.discovery.TestBuilders.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class DiscoveryTest {

    @Mock
    ClientRegistryClient clientRegistryClient;

    @Mock
    UserServiceClient userServiceClient;

    @Mock
    HipServiceClient hipServiceClient;

    private Discovery discovery;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void providersOfCalledWithMax() {
        discovery = new Discovery(clientRegistryClient, userServiceClient, hipServiceClient);
        Address address = address().use("work").build();
        Telecom telecom = telecom().use("work").build();
        var provider = provider()
                .addresses(List.of(address))
                .telecoms(List.of(telecom))
                .name("Max")
                .build();
        when(clientRegistryClient.providersOf(eq("Max"))).thenReturn(Flux.just(provider));

        StepVerifier.create(discovery.providersFrom("Max"))
                .expectNext(Transformer.to(provider))
                .verifyComplete();
    }

    @Test
    public void patientForGivenProviderIdAndPatientId() {
        discovery = new Discovery(clientRegistryClient, userServiceClient, hipServiceClient);
        Address address = address().use("work").build();
        Telecom telecom = telecom().use("work").build();
        PatientResponse patientResponse = patientResponse().patient(new in.org.projecteka.hdaf.link.discovery.model.patient.response.Patient("123", "John Doe", List.of(), List.of())).build();
        User user = user().identifier("1").firstName("first name").phoneNumber("9999999999").build();
        String hipClientUrl = "http://localhost:8001";
        Provider provider = provider()
                .addresses(List.of(address))
                .telecoms(List.of(telecom))
                .identifiers(List.of(providerIdentifier().system(hipClientUrl).use("official").build()))
                .name("Max")
                .build();
        Identifier identifier = patientIdentifier().type("MOBILE").value("9999999999").build();
        Patient patient = Patient.builder()
                .id(user.getIdentifier())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .verifiedIdentifiers(List.of(identifier))
                .unVerifiedIdentifiers(List.of())
                .build();
        PatientRequest patientRequest = patientRequest().patient(patient).transactionId("transaction-id").build();

        when(clientRegistryClient.providerOf(eq("1"))).thenReturn(Mono.just(provider));
        when(userServiceClient.userOf(eq("1"))).thenReturn(Mono.just(user));
        when(hipServiceClient.patientFor(eq(patientRequest), eq(hipClientUrl))).thenReturn(Mono.just(patientResponse));

        StepVerifier.create(discovery.patientFor("1", "1"))
                .expectNext(patientResponse)
                .verifyComplete();
    }

    @Test
    public void shouldGetInvalidHipErrorWhenIdentifierIsNotOfficial() {
        discovery = new Discovery(clientRegistryClient, userServiceClient, hipServiceClient);
        Address address = address().use("work").build();
        Telecom telecom = telecom().use("work").build();
        User user = user().identifier("1").firstName("first name").phoneNumber("9999999999").build();
        String hipClientUrl = "http://localhost:8001";
        Provider provider = provider()
                .addresses(List.of(address))
                .telecoms(List.of(telecom))
                .identifiers(List.of(providerIdentifier().system(hipClientUrl).use("random").build()))
                .name("Max")
                .build();

        when(clientRegistryClient.providerOf(eq("1"))).thenReturn(Mono.just(provider));
        when(userServiceClient.userOf(eq("1"))).thenReturn(Mono.just(user));

        StepVerifier.create(discovery.patientFor("1", "1"))
                .expectErrorMatches(error -> error.equals(new Throwable("Invalid HIP")));
    }
}