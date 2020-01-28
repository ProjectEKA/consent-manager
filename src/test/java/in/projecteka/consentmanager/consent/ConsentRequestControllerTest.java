package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.model.Provider;
import in.projecteka.consentmanager.clients.model.User;
import in.projecteka.consentmanager.consent.model.response.ConsentRequestDetail;
import in.projecteka.consentmanager.consent.model.response.ConsentRequestsRepresentation;
import in.projecteka.consentmanager.consent.model.response.RequestCreatedRepresentation;
import in.projecteka.consentmanager.consent.repository.ConsentArtefactRepository;
import in.projecteka.consentmanager.consent.repository.ConsentRequestRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@WebFluxTest(ConsentRequestController.class)
@ContextConfiguration(initializers = ConsentRequestControllerTest.PropertyInitializer.class)
@Import({ConsentManagerTestConfiguration.class, ConsentRequestRepository.class, ConsentManager.class, ClientRegistryClient.class, UserServiceClient.class, ConsentServiceProperties.class, ConsentArtefactRepository.class})
public class ConsentRequestControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ConsentRequestRepository repository;

    @MockBean
    private ConsentArtefactRepository consentArtefactRepository;

    @MockBean
    private ClientRegistryClient providerClient;

    @MockBean
    private UserServiceClient userServiceClient;

    @Test
    public void shouldAcceptConsentRequest() {
        when(repository.insert(any(), any())).thenReturn(Mono.empty());
        when(providerClient.providerWith(eq("MAX-ID"))).thenReturn(Mono.just(new Provider()));
        when(providerClient.providerWith(eq("TMH-ID"))).thenReturn(Mono.just(new Provider()));
        when(userServiceClient.userOf(eq("batman@ncg"))).thenReturn(Mono.just(new User()));

        String body = "" +
                "{\n" +
                "  \"consent\": {\n" +
                "    \"purpose\": {\n" +
                "      \"text\": \"For Clinical Reference\",\n" +
                "      \"code\": \"CLINICAL\",\n" +
                "      \"refUri\": \"http://nha.gov.in/value-set/purpose.txt\"\n" +
                "    },\n" +
                "    \"patient\": {\n" +
                "      \"id\": \"batman@ncg\"\n" +
                "    },\n" +
                "    \"hip\": {\n" +
                "      \"id\": \"TMH-ID\",\n" +
                "      \"name\": \"TMH\"\n" +
                "    },\n" +
                "    \"hiu\": {\n" +
                "      \"id\": \"MAX-ID\",\n" +
                "      \"name\": \"MAX\"\n" +
                "    },\n" +
                "    \"requester\": {\n" +
                "      \"name\": \"Dr Ramandeep\",\n" +
                "      \"identifier\": {\n" +
                "        \"value\": \"MCI-10\",\n" +
                "        \"type\": \"Oncologist\",\n" +
                "        \"system\": \"http://mci.org/\"\n" +
                "      }\n" +
                "    },\n" +
                "    \"hiTypes\": [\n" +
                "      \"Condition\",\n" +
                "      \"Observation\"\n" +
                "    ],\n" +
                "    \"permission\": {\n" +
                "      \"accessMode\": \"VIEW\",\n" +
                "      \"dateRange\": {\n" +
                "        \"from\": \"2021-01-16T07:23:41.305Z\",\n" +
                "        \"to\": \"2021-01-16T07:35:41.305Z\"\n" +
                "      },\n" +
                "      \"dataExpiryAt\": \"2022-01-16T07:23:41.305Z\",\n" +
                "      \"frequency\": {\n" +
                "        \"unit\": \"DAY\",\n" +
                "        \"value\": 1\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        webTestClient.post()
                .uri("/consent-requests")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "somevalue")
                .body(BodyInserters.fromValue(body))
                .exchange()
                .expectStatus().isOk()
                .expectBody(RequestCreatedRepresentation.class)
                .value(response -> response.getConsentRequestId(), Matchers.notNullValue());
    }

    @Test
    public void shouldGetConsentRequests() {
        List<ConsentRequestDetail> requests = new ArrayList<>();
        when(repository.requestsForPatient("Ganesh@ncg", 20, 0)).thenReturn(Mono.just(requests));
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/consent-requests").queryParam("limit", "20").build())
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "R2FuZXNoQG5jZw==")
                .exchange()
                .expectStatus().isOk()
                .expectBody(ConsentRequestsRepresentation.class)
                .value(response -> response.getLimit(), Matchers.is(20))
                .value(response -> response.getOffset(), Matchers.is(0))
                .value(response -> response.getRequests().size(), Matchers.is(0));
    }


    public static class PropertyInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues values = TestPropertyValues.of(
                    Stream.of(
                            "consentmanager.consentservice.maxPageSize=50"
                    )
            );
            values.applyTo(applicationContext);
        }
    }
}
