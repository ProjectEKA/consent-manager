package in.projecteka.consentmanager.link.discovery;

import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.DiscoveryServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.link.ClientError;
import in.projecteka.consentmanager.link.discovery.model.Identifier;
import in.projecteka.consentmanager.link.discovery.model.Provider;
import in.projecteka.consentmanager.link.discovery.model.User;
import in.projecteka.consentmanager.link.discovery.model.patient.request.Patient;
import in.projecteka.consentmanager.link.discovery.model.patient.request.PatientRequest;
import in.projecteka.consentmanager.link.discovery.model.patient.response.DiscoveryResponse;
import in.projecteka.consentmanager.link.discovery.model.patient.response.PatientResponse;
import in.projecteka.consentmanager.link.discovery.repository.DiscoveryRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class Discovery {

    private static final String MOBILE = "MOBILE";
    private final ClientRegistryClient clientRegistryClient;
    private UserServiceClient userServiceClient;
    private DiscoveryServiceClient discoveryServiceClient;
    private DiscoveryRepository discoveryRepository;

    public Discovery(
            ClientRegistryClient clientRegistryClient,
            UserServiceClient userServiceClient,
            DiscoveryServiceClient discoveryServiceClient,
            DiscoveryRepository discoveryRepository) {
        this.clientRegistryClient = clientRegistryClient;
        this.userServiceClient = userServiceClient;
        this.discoveryServiceClient = discoveryServiceClient;
        this.discoveryRepository = discoveryRepository;
    }

    public Flux<ProviderRepresentation> providersFrom(String name) {
        return clientRegistryClient.providersOf(name)
                .filter(this::isValid)
                .map(Transformer::to);
    }

    public Mono<DiscoveryResponse> patientFor(String providerId, String userName, String transactionId) {
        return userWith(userName)
                .flatMap(user -> providerUrl(providerId)
                        .switchIfEmpty(Mono.error(ClientError.unableToConnectToProvider()))
                        .flatMap(url -> patientIn(url, user, transactionId)
                                .flatMap(patientResponse -> insertDiscoveryRequest(patientResponse, providerId, userName, transactionId))));
    }

    private Mono<Provider> providerWith(String providerId) {
        return clientRegistryClient.providerWith(providerId);
    }

    private Mono<User> userWith(String patientId) {
        return userServiceClient.userOf(patientId);
    }

    private Mono<String> providerUrl(String providerId) {
        return providerWith(providerId)
                .flatMap(provider -> provider.getIdentifiers()
                        .stream()
                        .filter(Identifier::isOfficial)
                        .findFirst()
                        .map(identifier -> Mono.just(identifier.getSystem()))
                        .orElse(Mono.empty()));
    }

    private Mono<PatientResponse> patientIn(String url, User user, String transactionId) {
        var phoneNumber = in.projecteka.consentmanager.link.discovery.model.patient.request.Identifier.builder()
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
        return discoveryServiceClient.patientFor(patientRequest, url);
    }

    private Mono<DiscoveryResponse> insertDiscoveryRequest(PatientResponse patientResponse, String providerId, String patientId, String transactionId) {
        return discoveryRepository.insert(providerId, patientId, transactionId).
                then(Mono.just(DiscoveryResponse.
                        builder().
                        patient(patientResponse.getPatient()).
                        transactionId(transactionId)
                        .build()));
    }

    private boolean isValid(Provider provider) {
        return provider.getIdentifiers().stream().anyMatch(Identifier::isOfficial);
    }
}