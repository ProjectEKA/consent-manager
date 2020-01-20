package in.projecteka.consentmanager.consent;

import in.projecteka.consentmanager.clients.ClientRegistryClient;
import in.projecteka.consentmanager.clients.UserServiceClient;
import in.projecteka.consentmanager.clients.model.User;
import in.projecteka.consentmanager.consent.model.response.ConsentRequestResponse;
import in.projecteka.consentmanager.consent.repository.ConsentRequestRepository;
import in.projecteka.consentmanager.clients.model.Provider;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@WebFluxTest(ConsentRequestController.class)
@Import({ConsentRequestRepository.class, ConsentManager.class, ClientRegistryClient.class, UserServiceClient.class})
public class ConsentRequestControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ConsentRequestRepository repository;

    @MockBean
    private ClientRegistryClient providerClient;

    @MockBean
    private UserServiceClient userServiceClient;


    @Test
    public void shouldAcceptConsentRequest() {
        when(repository.insert(any(), any())).thenReturn(Mono.empty());
        when (providerClient.providerWith(eq("MAX-ID"))).thenReturn(Mono.just(new Provider()));
        when (providerClient.providerWith(eq("TMH-ID"))).thenReturn(Mono.just(new Provider()));
        when (userServiceClient.userOf(eq("batman@ncg"))).thenReturn(Mono.just(new User()));

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
                .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "somevalue")
                .body(BodyInserters.fromValue(body))
                .exchange()
                .expectStatus().isOk()
                .expectBody(ConsentRequestResponse.class)
                .value(response -> response.getConsentRequestId(), Matchers.notNullValue());



    }
}
