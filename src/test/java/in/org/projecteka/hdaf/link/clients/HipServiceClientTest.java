package in.org.projecteka.hdaf.link.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.org.projecteka.hdaf.clients.HipServiceClient;
import in.org.projecteka.hdaf.link.discovery.model.patient.request.PatientRequest;
import in.org.projecteka.hdaf.link.discovery.model.patient.response.HipPatientResponse;
import in.org.projecteka.hdaf.link.discovery.model.patient.response.Patient;
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
import java.util.List;

import static in.org.projecteka.hdaf.link.TestBuilders.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


public class HipServiceClientTest {
    @Captor
    private ArgumentCaptor<ClientRequest> captor;
    private HipServiceClient hipServiceClient;
    @Mock
    private ExchangeFunction exchangeFunction;

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);
        WebClient.Builder webClientBuilder = WebClient.builder()
                .exchangeFunction(exchangeFunction);
        hipServiceClient = new HipServiceClient(webClientBuilder);
    }


    @Test
    void shouldDiscoverPatients() throws IOException {
        Patient expectedPatient = patientInResponse().display("Patient Name").careContexts(List.of(careContext().display("Care context 1").build())).build();
        HipPatientResponse hipPatientResponse = hipPatientResponse().patient(expectedPatient).build();
        String patientResponseBody = new ObjectMapper().writeValueAsString(hipPatientResponse);
        when(exchangeFunction.exchange(captor.capture())).thenReturn(Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(patientResponseBody).build()));

        PatientRequest patientRequest = patientRequest().patient(patientInRequest().build()).transactionId("transaction-id-1").build();
        StepVerifier.create(hipServiceClient.patientFor(patientRequest, "http://hip-url/"))
                .assertNext(response -> {
                    assertThat(response.getPatient().getDisplay()).isEqualTo(expectedPatient.getDisplay());
                    assertThat(response.getPatient().getReferenceNumber()).isEqualTo(expectedPatient.getReferenceNumber());
                    assertThat(response.getPatient().getMatchedBy()).isEqualTo(expectedPatient.getMatchedBy());
                    assertThat(response.getPatient().getCareContexts().get(0).getDisplay()).isEqualTo(expectedPatient.getCareContexts().get(0).getDisplay());
                })
                .verifyComplete();

        assertThat(captor.getValue().url().toString()).isEqualTo("http://hip-url/patients/discover/");
    }
}
