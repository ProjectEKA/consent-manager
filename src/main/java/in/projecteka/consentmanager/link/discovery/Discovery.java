package in.projecteka.consentmanager.link.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.DiscoveryServiceClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.model.Identifier;
import in.projecteka.consentmanager.clients.model.Provider;
import in.projecteka.consentmanager.clients.model.User;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.link.discovery.model.patient.request.Patient;
import in.projecteka.consentmanager.link.discovery.model.patient.request.PatientIdentifier;
import in.projecteka.consentmanager.link.discovery.model.patient.request.PatientRequest;
import in.projecteka.consentmanager.link.discovery.model.patient.response.DiscoveryResponse;
import in.projecteka.consentmanager.link.discovery.model.patient.response.DiscoveryResult;
import in.projecteka.consentmanager.link.discovery.model.patient.response.PatientResponse;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
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
    private final GatewayServiceProperties gatewayServiceProperties;
    private final CacheAdapter<String,String> discoveryResults;

    private static final Logger logger = LoggerFactory.getLogger(Discovery.class);

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
                        .flatMap(tuple -> patientIn(providerId, tuple.getT2(), tuple.getT1(), transactionId, unverifiedIdentifiers))
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
                .flatMap(val ->
                        userWith(userName)
                                .flatMap(user -> discoveryServiceClient.requestPatientFor(
                                        requestFor(user, transactionId, unverifiedIdentifiers),
                                        gatewaySystemUrl(),
                                        providerId)
                                        .zipWith(Mono.delay(Duration.ofSeconds(getExpectedFlowResponseDuration())))
                                        .flatMap(tuple ->
                                                discoveryResults.get(transactionId.toString())
                                                        .switchIfEmpty(Mono.error(ClientError.gatewayTimeOut()))
                                                        .flatMap(dr -> resultFromHIP(dr))
                                        )))
                .switchIfEmpty(Mono.error(ClientError.networkServiceCallFailed()))
                .flatMap(discoveryResult -> {
                    if (discoveryResult.getError() != null) {
                        //Should we get the error and throw client error with the errors
                        logger.error("[Discovery] Patient care-contexts discovery resulted in error {}", discoveryResult.getError().getMessage());
                        return Mono.error(ClientError.patientNotFound());
                    }
                    if (discoveryResult.getPatient() == null){
                        return Mono.error(ClientError.patientNotFound());
                    }
                    return Mono.just(DiscoveryResponse.builder()
                            .patient(discoveryResult.getPatient())
                            .transactionId(transactionId)
                            .build());
                })
                .doOnSuccess(r -> discoveryRepository.insert(providerId, userName, transactionId, requestId));

    }

    private Mono<DiscoveryResult> resultFromHIP(String responseBody) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return Mono.just(objectMapper.readValue(responseBody, DiscoveryResult.class));
        } catch (JsonProcessingException e) {
            logger.error("[Discovery] Can not deserialize response from HIP", e);
        }
        return Mono.empty();
    }

    private long getExpectedFlowResponseDuration() {
        return gatewayServiceProperties.getResponseTimeout();
    }

    private String gatewaySystemUrl() {
        return gatewayServiceProperties.getBaseUrl();
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

    private Mono<PatientResponse> patientIn(String hipId, String hipSystemUrl, User user, UUID transactionId, List<PatientIdentifier> unverifiedIdentifiers) {
        var patientRequest = requestFor(user, transactionId, unverifiedIdentifiers);
        return discoveryServiceClient.patientFor(patientRequest, hipSystemUrl, hipId);
    }

    private PatientRequest requestFor(User user, UUID transactionId, List<PatientIdentifier> unverifiedIdentifiers) {
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

        return  PatientRequest.builder().patient(patient).requestId(transactionId).build();
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