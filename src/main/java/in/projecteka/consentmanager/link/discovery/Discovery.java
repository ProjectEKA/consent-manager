package in.projecteka.consentmanager.link.discovery;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.DiscoveryServiceClient;
import in.projecteka.consentmanager.clients.ErrorMap;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.model.Error;
import in.projecteka.consentmanager.clients.model.ErrorRepresentation;
import in.projecteka.consentmanager.clients.model.Identifier;
import in.projecteka.consentmanager.clients.model.Provider;
import in.projecteka.consentmanager.clients.model.RespError;
import in.projecteka.consentmanager.clients.model.User;
import in.projecteka.consentmanager.clients.properties.LinkServiceProperties;
import in.projecteka.consentmanager.common.CentralRegistry;
import in.projecteka.consentmanager.common.DelayTimeoutException;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.link.discovery.model.patient.request.Patient;
import in.projecteka.consentmanager.link.discovery.model.patient.request.PatientIdentifier;
import in.projecteka.consentmanager.link.discovery.model.patient.request.PatientRequest;
import in.projecteka.consentmanager.link.discovery.model.patient.response.DiscoveryResponse;
import in.projecteka.consentmanager.link.discovery.model.patient.response.DiscoveryResult;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static in.projecteka.consentmanager.common.CustomScheduler.scheduleThis;
import static in.projecteka.consentmanager.common.Serializer.from;
import static in.projecteka.consentmanager.common.Serializer.tryTo;

@AllArgsConstructor
public class Discovery {
	private static final String MOBILE = "MOBILE";
	private final UserServiceClient userServiceClient;
	private final DiscoveryServiceClient discoveryServiceClient;
	private final DiscoveryRepository discoveryRepository;
	private final CentralRegistry centralRegistry;
	private final LinkServiceProperties serviceProperties;
	private final CacheAdapter<String, String> discoveryResults;

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

	public Mono<DiscoveryResponse> patientInHIP(String userName,
	                                            List<PatientIdentifier> unverifiedIdentifiers,
	                                            String providerId,
	                                            UUID transactionId,
	                                            UUID requestId) {

		return Mono.just(requestId)
				.filterWhen(this::validateRequest)
				.switchIfEmpty(Mono.error(ClientError.requestAlreadyExists()))
				.flatMap(val -> userWith(userName))
				.flatMap(user -> scheduleThis(discoveryServiceClient.requestPatientFor(
						requestFor(user, transactionId, unverifiedIdentifiers, requestId),
						providerId))
						.timeout(Duration.ofMillis(getExpectedFlowResponseDuration()))
						.responseFrom(discard -> Mono.defer(() -> discoveryResults.get(requestId.toString()))))
				.onErrorResume(DelayTimeoutException.class, discard -> Mono.error(ClientError.gatewayTimeOut()))
				.flatMap(response -> tryTo(response, DiscoveryResult.class).map(Mono::just).orElse(Mono.empty()))
				.flatMap(discoveryResult -> {
					if (discoveryResult.getError() != null) {
						logger.error("[Discovery] Patient care-contexts discovery resulted in error {}", discoveryResult.getError());
						return Mono.error(new ClientError(HttpStatus.NOT_FOUND, cmErrorRepresentation(discoveryResult.getError())));
					}
					if (discoveryResult.getPatient() == null) {
						logger.error("[Discovery] Patient care-contexts discovery should have returned in " +
								"Patient reference with care context details or error caused." +
								"Gateway requestId {}", discoveryResult.getRequestId());
						return Mono.error(ClientError.invalidResponseFromHIP());
					}
					return Mono.just(DiscoveryResponse.builder()
							.patient(discoveryResult.getPatient())
							.transactionId(transactionId)
							.build());
				})
				.doOnSuccess(r -> discoveryRepository
						.insert(providerId, userName, transactionId, requestId)
						.subscribe());
	}

	private ErrorRepresentation cmErrorRepresentation(RespError respError) {
		Error error = Error.builder()
				.code(ErrorMap.hipToCmError(respError.getCode()))
				.message(respError.getMessage())
				.build();
		return ErrorRepresentation.builder().error(error).build();
	}

	public Mono<Void> onDiscoverPatientCareContexts(DiscoveryResult discoveryResult) {
		if (discoveryResult.hasResponseId()) {
			return discoveryResults.put(discoveryResult.getResp().getRequestId(), from(discoveryResult));
		}
		logger.error("[Discovery] Received a discovery response from Gateway without original request Id mentioned.{}",
				discoveryResult.getRequestId());
		return Mono.error(ClientError.unprocessableEntity());
	}

	private long getExpectedFlowResponseDuration() {
		return serviceProperties.getTxnTimeout();
	}

	private Mono<Boolean> validateRequest(UUID requestId) {
		return discoveryRepository.getIfPresent(requestId)
				.map(Objects::isNull)
				.switchIfEmpty(Mono.just(true));
	}

	private Mono<User> userWith(String patientId) {
		return userServiceClient.userOf(patientId);
	}

	private PatientRequest requestFor(User user,
									  UUID transactionId,
									  List<PatientIdentifier> unverifiedIdentifiers,
									  UUID requestId) {
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
				.name(user.getName().createFullName())
				.gender(user.getGender())
				.yearOfBirth(user.getDateOfBirth().getYear())
				.verifiedIdentifiers(List.of(phoneNumber))
				.unverifiedIdentifiers(unverifiedIds)
				.build();
		return PatientRequest.builder()
				.patient(patient)
				.requestId(requestId)
				.transactionId(transactionId)
				.timestamp(java.time.Instant.now().toString())
				.build();
	}

	private boolean isValid(Provider provider) {
		return provider.getIdentifiers().stream().anyMatch(Identifier::isOfficial);
	}
}
