package in.projecteka.consentmanager.link.discovery;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.DiscoveryServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.model.Identifier;
import in.projecteka.consentmanager.clients.model.Provider;
import in.projecteka.consentmanager.clients.model.User;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.link.discovery.model.patient.request.Patient;
import in.projecteka.consentmanager.link.discovery.model.patient.request.PatientIdentifier;
import in.projecteka.consentmanager.link.discovery.model.patient.request.PatientRequest;
import in.projecteka.consentmanager.link.discovery.model.patient.response.DiscoveryResponse;
import in.projecteka.consentmanager.link.discovery.model.patient.response.PatientResponse;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@AllArgsConstructor
public class Discovery {

    private static final String MOBILE = "MOBILE";
    private final UserServiceClient userServiceClient;
    private final DiscoveryServiceClient discoveryServiceClient;
    private final DiscoveryRepository discoveryRepository;
    private final CentralRegistry centralRegistry;

    public Flux<ProviderRepresentation> providersFrom(String name) {
        return centralRegistry.providersOf(name)
                .filter(this::isValid)
                .map(Transformer::to);
    }

    public Mono<ProviderRepresentation> providerBy(String id) {
        return centralRegistry.providerWith(id)
                .filter(this::isValid)
                .map(Transformer::to);
    }

    public Mono<DiscoveryResponse> patientFor(String userName,
                                              List<PatientIdentifier> unverifiedIdentifiers,
                                              String providerId,
                                              UUID transactionId,
                                              UUID requestId) {
        return Mono.just(requestId)
                .filterWhen(this::validateRequest)
                .switchIfEmpty(Mono.error(ClientError.requestAlreadyExists()))
                .flatMap(val -> userWith(userName)
                        .zipWith(providerUrl(providerId))
                        .switchIfEmpty(Mono.error(ClientError.unableToConnectToProvider()))
                        .flatMap(tuple -> patientIn(tuple.getT2(), tuple.getT1(), transactionId, unverifiedIdentifiers))
                        .flatMap(patientResponse ->
                                insertDiscoveryRequest(patientResponse,
                                        providerId,
                                        userName,
                                        transactionId,
                                        requestId)));
    }

    public Mono<DiscoveryResponse> patientInHIP(String userName,
                                              List<PatientIdentifier> unverifiedIdentifiers,
                                              String providerId,
                                              UUID transactionId,
                                              UUID requestId) {
        return Mono.just(requestId)
                .filterWhen(this::validateRequest)
                .switchIfEmpty(Mono.error(ClientError.requestAlreadyExists()))
                .flatMap(val -> userWith(userName)
                                    .zipWith(gatewaySystemUrl())
                                    .flatMap(userAndGateway -> patientIn(userAndGateway.getT2(), userAndGateway.getT1(), transactionId, unverifiedIdentifiers))
                                    .flatMap(patientResponse ->
                                            insertDiscoveryRequest(patientResponse,
                                                    providerId,
                                                    userName,
                                                    transactionId,
                                                    requestId)));
    }

    private Mono<String> gatewaySystemUrl() {
        return Mono.just("http://tmc.gov.in/ncg-gateway");
    }


    private Mono<Boolean> validateRequest(UUID requestId) {
        return discoveryRepository.getIfPresent(requestId)
                .map(Objects::isNull)
                .switchIfEmpty(Mono.just(true));
    }

    private Mono<User> userWith(String patientId) {
        return userServiceClient.userOf(patientId);
    }

    private Mono<String> providerUrl(String providerId) {
        return centralRegistry.providerWith(providerId)
                .flatMap(provider -> provider.getIdentifiers()
                        .stream()
                        .filter(Identifier::isOfficial)
                        .findFirst()
                        .map(identifier -> Mono.just(identifier.getSystem()))
                        .orElse(Mono.empty()));
    }

    private Mono<PatientResponse> patientIn(String hipSystemUrl, User user, UUID transactionId, List<PatientIdentifier> unverifiedIdentifiers) {
        var phoneNumber = in.projecteka.consentmanager.link.discovery.model.patient.request.Identifier.builder()
                .type(MOBILE)
                .value(user.getPhone())
                .build();
        List<in.projecteka.consentmanager.link.discovery.model.patient.request.Identifier> unverifiedIds =
                (unverifiedIdentifiers == null || unverifiedIdentifiers.isEmpty())
                        ? Collections.emptyList()
                        : unverifiedIdentifiers.stream().map(patientIdentifier ->
                        in.projecteka.consentmanager.link.discovery.model.patient.request.Identifier.builder()
                                .type(patientIdentifier.getType().toString())
                                .value(patientIdentifier.getValue())
                                .build()).collect(Collectors.toList());
        Patient patient = Patient.builder()
                .id(user.getIdentifier())
                .name(user.getName())
                .gender(user.getGender())
                .yearOfBirth(user.getYearOfBirth())
                .verifiedIdentifiers(List.of(phoneNumber))
                .unverifiedIdentifiers(unverifiedIds)
                .build();

        var patientRequest = PatientRequest.builder().patient(patient).requestId(transactionId).build();
        return discoveryServiceClient.patientFor(patientRequest, hipSystemUrl);
    }

    private Mono<DiscoveryResponse> insertDiscoveryRequest(PatientResponse patientResponse,
                                                           String providerId,
                                                           String patientId,
                                                           UUID transactionId,
                                                           UUID requestId) {
        return discoveryRepository.insert(providerId, patientId, transactionId, requestId).
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