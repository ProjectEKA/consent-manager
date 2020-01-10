package in.org.projecteka.hdaf.link.link;

import in.org.projecteka.hdaf.clients.ClientRegistryClient;
import in.org.projecteka.hdaf.link.ClientError;
import in.org.projecteka.hdaf.link.HIPClient;
import in.org.projecteka.hdaf.link.discovery.model.Identifier;
import in.org.projecteka.hdaf.link.link.model.PatientLinkReferenceRequest;
import in.org.projecteka.hdaf.link.link.model.PatientLinkReferenceResponse;
import in.org.projecteka.hdaf.link.link.model.PatientLinkRequest;
import in.org.projecteka.hdaf.link.link.model.PatientLinkResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static in.org.projecteka.hdaf.link.TestBuilders.*;
import static in.org.projecteka.hdaf.link.link.Transformer.toHIPPatient;
import static java.util.Arrays.asList;
import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class LinkTest {

  @Mock
  private ClientRegistryClient clientRegistryClient;

  @Mock
  private HIPClient hipClient;

  @BeforeEach
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void createsLinkReference() {
    var link = new Link(hipClient, clientRegistryClient);
    var address = address().use("work").build();
    var telecommunication = telecom().use("work").build();
    String providerUrl = "http://localhost:8001";
    var identifier =
        identifier().use(Identifier.IdentifierType.OFFICIAL.toString()).system(providerUrl).build();
    var provider =
        provider()
            .addresses(of(address))
            .telecoms(of(telecommunication))
            .identifiers(of(identifier))
            .name("Max")
            .build();

    when(clientRegistryClient.providerWith(eq("10000005"))).thenReturn(Mono.just(provider));

    PatientLinkReferenceResponse patientLinkReferenceResponse =
        PatientLinkReferenceResponse.builder().build();
    String patientId = "patient";
    PatientLinkReferenceRequest patientLinkReferenceRequest = patientLinkReferenceRequest().build();

      var patientLinkReferenceRequestForHIP =
        new in.org.projecteka.hdaf.link.link.model.hip.PatientLinkReferenceRequest(
            patientLinkReferenceRequest.getTransactionId(),
            toHIPPatient(patientId, patientLinkReferenceRequest.getPatient()));
    when(hipClient.linkPatientCareContext(patientLinkReferenceRequestForHIP, providerUrl))
        .thenReturn(Mono.just(patientLinkReferenceResponse));

    StepVerifier.create(link.patientWith(patientId, patientLinkReferenceRequest))
        .expectNext(patientLinkReferenceResponse)
        .verifyComplete();
  }

  @Test
  public void shouldGetSystemUrlForOfficialIdentifier() {
      var link = new Link(hipClient, clientRegistryClient);
      var address = address().use("work").build();
      var telecommunication = telecom().use("work").build();
      String providerUrl = "http://localhost:8001";
      var identifier1 =
              identifier().use("personal").system("personalUrl").build();
      var identifier2 =
              identifier().use(Identifier.IdentifierType.OFFICIAL.toString()).system(providerUrl).build();
      var provider =
              provider()
                      .addresses(of(address))
                      .telecoms(of(telecommunication))
                      .identifiers(of(identifier1, identifier2))
                      .name("Max")
                      .build();

      when(clientRegistryClient.providerWith(eq("10000005"))).thenReturn(Mono.just(provider));

      PatientLinkReferenceResponse patientLinkReferenceResponse =
              PatientLinkReferenceResponse.builder().build();
      String patientId = "patient";
      PatientLinkReferenceRequest patientLinkReferenceRequest = patientLinkReferenceRequest().build();

      var patientLinkReferenceRequestForHIP =
              new in.org.projecteka.hdaf.link.link.model.hip.PatientLinkReferenceRequest(
                      patientLinkReferenceRequest.getTransactionId(),
                      toHIPPatient(patientId, patientLinkReferenceRequest.getPatient()));
      when(hipClient.linkPatientCareContext(patientLinkReferenceRequestForHIP, providerUrl))
              .thenReturn(Mono.just(patientLinkReferenceResponse));

      StepVerifier.create(link.patientWith(patientId, patientLinkReferenceRequest))
              .expectNext(patientLinkReferenceResponse)
              .verifyComplete();

      verify(hipClient).linkPatientCareContext(patientLinkReferenceRequestForHIP, providerUrl);
  }

    @Test
    public void shouldGetErrorWhenProviderUrlIsEmpty() {
        var link = new Link(hipClient, clientRegistryClient);
        var address = address().use("work").build();
        var telecommunication = telecom().use("work").build();
        var provider =
                provider()
                        .addresses(of(address))
                        .telecoms(of(telecommunication))
                        .identifiers(asList())
                        .name("Max")
                        .build();

        when(clientRegistryClient.providerWith(eq("10000005"))).thenReturn(Mono.just(provider));

        String patientId = "patient";
        PatientLinkReferenceRequest patientLinkReferenceRequest = patientLinkReferenceRequest().build();

        ClientError clientError = ClientError.unableToConnectToProvider();
        StepVerifier.create(link.patientWith(patientId, patientLinkReferenceRequest))
                .expectErrorSatisfies(error -> {
                    assertThat(((ClientError)error).getError()).isEqualTo(clientError.getError());
                    assertThat(((ClientError)error).getHttpStatus()).isEqualTo(clientError.getHttpStatus());
                })
                .verify();
    }

  @Test
  public void linksPatientsCareContexts() {
      var link = new Link(hipClient, clientRegistryClient);
      var address = address().use("work").build();
      var telecommunication = telecom().use("work").build();
      String providerUrl = "http://localhost:8001";
      var identifier =
              identifier().use(Identifier.IdentifierType.OFFICIAL.toString()).system(providerUrl).build();
      var provider =
              provider()
                      .addresses(of(address))
                      .telecoms(of(telecommunication))
                      .identifiers(of(identifier))
                      .name("Max")
                      .build();
      when(clientRegistryClient.providerWith(eq("10000005"))).thenReturn(Mono.just(provider));

      PatientLinkResponse patientLinkResponse =
              PatientLinkResponse.builder().build();
      PatientLinkRequest patientLinkRequest = patientLinkRequest().build();
      String linkRefNumber = "link-ref-num";
      when(hipClient.validateToken(linkRefNumber, patientLinkRequest, providerUrl))
              .thenReturn(Mono.just(patientLinkResponse));

      StepVerifier.create(link.verifyToken(linkRefNumber, patientLinkRequest))
              .expectNext(patientLinkResponse)
              .verifyComplete();
  }
}
