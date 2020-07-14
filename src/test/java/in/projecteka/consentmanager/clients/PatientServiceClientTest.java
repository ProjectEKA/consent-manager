package in.projecteka.consentmanager.clients;

import in.projecteka.consentmanager.clients.properties.LinkServiceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.Collections;

import static in.projecteka.consentmanager.clients.TestBuilders.string;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class PatientServiceClientTest {
    @Captor
    private ArgumentCaptor<ClientRequest> captor;
    private PatientServiceClient patientServiceClient;
    @Mock
    private ExchangeFunction exchangeFunction;

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);
        WebClient.Builder webClientBuilder = WebClient.builder()
                .exchangeFunction(exchangeFunction);
        LinkServiceProperties serviceProperties = new LinkServiceProperties("http://user-service/", 1000);
        patientServiceClient = new PatientServiceClient(webClientBuilder.build(),
                () -> Mono.just(string()), serviceProperties.getUrl());
    }

    @Test
    void shouldGetCareContexts() throws IOException, InterruptedException {
        String patientLinkJson = "{\n" +
                "  \"patient\": {\n" +
                "    \"id\": \"string\",\n" +
                "    \"firstName\": \"string\",\n" +
                "    \"lastName\": \"string\",\n" +
                "    \"links\": [\n" +
                "      {\n" +
                "        \"hip\": {\n" +
                "          \"id\": \"TMH\",\n" +
                "          \"name\": \"Tata Memorial Hospital\"\n" +
                "        },\n" +
                "        \"referenceNumber\": \"patientX@tmh\",\n" +
                "        \"display\": \"Patient X at TMH\",\n" +
                "        \"careContexts\": [\n" +
                "          {\n" +
                "            \"referenceNumber\": \"patientX.OpdContext\",\n" +
                "            \"display\": \"Patient X OPD\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";


        when(exchangeFunction.exchange(captor.capture()))
                .thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body(patientLinkJson).build()));

        StepVerifier.create(
                patientServiceClient.retrievePatientLinks("patientAuthToken"))
                .assertNext(linkedCareContexts -> {
                    assertThat(linkedCareContexts.hasCCReferences("TMH", Collections.emptyList())).isTrue();
                    assertThat(linkedCareContexts.hasCCReferences("MAX", Collections.emptyList())).isFalse();
                    assertThat(
                            linkedCareContexts.hasCCReferences("TMH", Collections.singletonList("patientX.OpdContext")))
                            .isTrue();
                    assertThat(
                            linkedCareContexts.hasCCReferences("MAX", Collections.singletonList("patientX.OpdContext")))
                            .isFalse();
                })
                .verifyComplete();
    }
}
