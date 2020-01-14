package in.org.projecteka.hdaf.link.discovery;

import in.org.projecteka.hdaf.clients.ClientRegistryClient;
import in.org.projecteka.hdaf.clients.HipServiceClient;
import in.org.projecteka.hdaf.clients.UserServiceClient;
import in.org.projecteka.hdaf.link.discovery.model.Identifier;
import in.org.projecteka.hdaf.link.discovery.model.Provider;
import in.org.projecteka.hdaf.link.discovery.model.User;
import in.org.projecteka.hdaf.link.discovery.model.patient.request.Patient;
import in.org.projecteka.hdaf.link.discovery.model.patient.request.PatientRequest;
import in.org.projecteka.hdaf.link.discovery.model.patient.response.DiscoveryResponse;
import in.org.projecteka.hdaf.link.discovery.model.patient.response.PatientResponse;
import in.org.projecteka.hdaf.link.discovery.repository.DiscoveryRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public class Discovery {

    private static final String MOBILE = "MOBILE";
    private final ClientRegistryClient clientRegistryClient;
    private UserServiceClient userServiceClient;
    private HipServiceClient hipServiceClient;
    private DiscoveryRepository discoveryRepository;

    public Discovery(
            ClientRegistryClient clientRegistryClient,
            UserServiceClient userServiceClient,
            HipServiceClient hipServiceClient,
            DiscoveryRepository discoveryRepository) {
        this.clientRegistryClient = clientRegistryClient;
        this.userServiceClient = userServiceClient;
        this.hipServiceClient = hipServiceClient;
        this.discoveryRepository = discoveryRepository;
    }

    public Flux<ProviderRepresentation> providersFrom(String name) {
        return clientRegistryClient.providersOf(name)
                .filter(this::isValid)
                .map(Transformer::to);
    }

    public Mono<DiscoveryResponse> patientFor(String providerId, String userName, String transactionId) {
        return userWith(userName).flatMap(user -> providerWith(providerId)
                .map(this::getDiscoveryServiceUrl)
                .flatMap(optionalSystem -> optionalSystem.map(url -> patientIn(url, user, transactionId)
                        .flatMap(patientResponse -> insertDiscoveryRequest(patientResponse, providerId, userName, transactionId))
                ).orElse(Mono.error(new Throwable("Invalid HIP")))));
    }

    private Mono<Provider> providerWith(String providerId) {
        return clientRegistryClient.providerWith(providerId);
    }

    private Mono<User> userWith(String patientId) {
        return userServiceClient.userOf(patientId);
    }

    private Optional<String> getDiscoveryServiceUrl(Provider provider) {
        return provider.getIdentifiers()
                .stream()
                .filter(Identifier::isOfficial)
                .findFirst()
                .map(Identifier::getSystem);
    }

    private Mono<PatientResponse> patientIn(String url, User user, String transactionId) {
        var phoneNumber = in.org.projecteka.hdaf.link.discovery.model.patient.request.Identifier.builder()
                .type(MOBILE)
                .value(user.getPhone().getCountryCode() + user.getPhone().getNumber())
                .build();
        Patient patient = Patient.builder()
                .id(user.getIdentifier())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .verifiedIdentifiers(List.of(phoneNumber))
                .unVerifiedIdentifiers(List.of())
                .build();

        PatientRequest patientRequest = PatientRequest.builder().patient(patient).transactionId(transactionId).build();
        return hipServiceClient.patientFor(patientRequest, url);
    }

    private Mono<DiscoveryResponse> insertDiscoveryRequest(PatientResponse patientResponse, String providerId, String patientId, String transactionId) {
        return discoveryRepository.insert(providerId, patientId, transactionId).
                then(Mono.just(DiscoveryResponse.
                        builder().
                        patient(patientResponse.getPatient()).
                        transactionId(transactionId)
                        .build())
                );
    }

    private boolean isValid(Provider provider) {
        return provider.getIdentifiers().stream().anyMatch(Identifier::isOfficial);
    }
}