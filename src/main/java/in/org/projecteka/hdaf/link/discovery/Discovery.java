package in.org.projecteka.hdaf.link.discovery;

import in.org.projecteka.hdaf.clients.ClientRegistryClient;
import in.org.projecteka.hdaf.clients.HipServiceClient;
import in.org.projecteka.hdaf.link.discovery.model.patient.PatientRequest;
import in.org.projecteka.hdaf.clients.UserServiceClient;
import in.org.projecteka.hdaf.link.discovery.model.Identifier;
import in.org.projecteka.hdaf.link.discovery.model.patient.Patient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;

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
                .map(Transformer::to);
    }

    public Mono<Patient> patientFor(String providerId, String patientId) {
        return client.providerOf(providerId)
                                .map(provider -> provider.getIdentifiers()
                                        .stream()
                                        .filter(identifier -> identifier.getSystem().equals("http://localhost:8081"))
                                        .findFirst()
                                        .map(Identifier::getSystem))
                                        .flatMap(s -> s.map(url ->
                                        hipServiceClient.patientFor(new PatientRequest("John", Arrays.asList(new in.org.projecteka.hdaf.link.discovery.model.patient.Identifier("Mobile", "9999999999"))), url))
                                            .orElse(Mono.error(new Throwable("Invalid HIP"))));
//        return userServiceClient.userOf(patientId)
//                .flatMap(user ->
//                        client.providerOf(providerId)
//                                .map(provider -> provider.getIdentifiers()
//                                        .stream()
//                                        .filter(identifier -> identifier.getSystem().equals("asa"))
//                                        .findFirst()
//                                        .map(Identifier::getSystem))
//                                        .flatMap(s -> s.map(url ->
//                                        hipServiceClient.patientFor(new PatientRequest(), url))
//                                            .orElse(Mono.error(new Throwable("Invalid HIP")))));
    }
}
