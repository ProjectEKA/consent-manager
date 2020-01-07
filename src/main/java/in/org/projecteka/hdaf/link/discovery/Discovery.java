package in.org.projecteka.hdaf.link.discovery;

import in.org.projecteka.hdaf.clients.ClientRegistryClient;
import in.org.projecteka.hdaf.clients.HipServiceClient;
import in.org.projecteka.hdaf.clients.UserServiceClient;
import in.org.projecteka.hdaf.link.discovery.model.Identifier;
import in.org.projecteka.hdaf.link.discovery.model.patient.request.Patient;
import in.org.projecteka.hdaf.link.discovery.model.patient.request.PatientRequest;
import in.org.projecteka.hdaf.link.discovery.model.patient.response.PatientResponse;
import in.org.projecteka.hdaf.link.discovery.model.Provider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public class Discovery {

    private final ClientRegistryClient client;
    private UserServiceClient userServiceClient;
    private HipServiceClient hipServiceClient;

    public Discovery(
            ClientRegistryClient client,
            UserServiceClient userServiceClient,
            HipServiceClient hipServiceClient) {
        this.client = client;
        this.userServiceClient = userServiceClient;
        this.hipServiceClient = hipServiceClient;
    }

    public Flux<ProviderRepresentation> providersFrom(String name) {
        return client.providersOf(name)
                .filter(this::isValid)
                .map(Transformer::to);
    }

    public Mono<PatientResponse> patientFor(String providerId, String patientId) {
        return userServiceClient.userOf(patientId)
                .flatMap(user -> client.providerOf(providerId)
                        .map(provider -> provider.getIdentifiers()
                                .stream()
                                .filter(identifier -> identifier.getUse().equals("official"))
                                .findFirst()
                                .map(Identifier::getSystem))
                        .flatMap(s -> s.map(url -> {
                            in.org.projecteka.hdaf.link.discovery.model.patient.request.Identifier phoneNumber = in.org.projecteka.hdaf.link.discovery.model.patient.request.Identifier.builder()
                                    .type("MOBILE")
                                    .value(user.getPhoneNumber())
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

                            PatientRequest patientRequest = PatientRequest.builder().patient(patient).transactionId("transaction-id").build();
                            return hipServiceClient.patientFor(patientRequest, url);
                        }).orElse(Mono.error(new Throwable("Invalid HIP")))));
    }

    private boolean isValid(Provider provider) {
        return provider.getIdentifiers().stream().anyMatch(Identifier::isOfficial);
    }
}
