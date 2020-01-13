package in.org.projecteka.hdaf.link.link;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.util.stream.Stream;

import static in.org.projecteka.hdaf.link.link.TestBuilders.errorRepresentation;
import static in.org.projecteka.hdaf.link.link.TestBuilders.identifier;
import static in.org.projecteka.hdaf.link.link.TestBuilders.patientLinkReferenceRequest;
import static in.org.projecteka.hdaf.link.link.TestBuilders.patientLinkReferenceResponse;
import static in.org.projecteka.hdaf.link.link.TestBuilders.patientLinkResponse;
import static in.org.projecteka.hdaf.link.link.TestBuilders.provider;
import static java.util.List.of;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ContextConfiguration(initializers = LinkUserJourneyTest.ContextInitializer.class)
public class LinkUserJourneyTest {
  private static MockWebServer clientRegistryServer = new MockWebServer();
  private static MockWebServer hipServer = new MockWebServer();

  @Autowired private WebTestClient webTestClient;

  @Test
  public void shouldGetLinkReference() throws IOException {
    var official = identifier().use("official").system(hipServer.url("").toString()).build();
    var provider = provider().identifiers(of(official)).build();
    var providerAsJson = new ObjectMapper().writeValueAsString(provider);
    clientRegistryServer.enqueue(
        new MockResponse().setHeader("Content-Type", "application/json").setBody(providerAsJson));

    var linkReference = patientLinkReferenceResponse().build();
    var linkReferenceJson = new ObjectMapper().writeValueAsString(linkReference);
    hipServer.enqueue(
        new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody(linkReferenceJson));
    webTestClient
        .post()
        .uri("/patients/link")
        .header("Authorization", "MTIzNDU2Nzg5")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(patientLinkReferenceRequest().build())
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody()
        .json(linkReferenceJson);
  }

    @Test
    public void shouldGivePatientNotFound() throws IOException {
        var official = identifier().use("official").system(hipServer.url("").toString()).build();
        var provider = provider().identifiers(of(official)).build();
        var providerAsJson = new ObjectMapper().writeValueAsString(provider);
        clientRegistryServer.enqueue(
                new MockResponse().setHeader("Content-Type", "application/json").setBody(providerAsJson));

        var errorResponse = errorRepresentation().build();
        var errorResponseJson = new ObjectMapper().writeValueAsString(errorResponse);
        hipServer.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setStatus("HTTP/1.1 404")
                        .setBody(errorResponseJson));
        webTestClient
                .post()
                .uri("/patients/link")
                .header("Authorization", "MTIzNDU2Nzg5")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(patientLinkReferenceRequest().build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .json(errorResponseJson);
    }

    @Test
    public void shouldLinkCareContexts() throws IOException {
        var official = identifier().use("official").system(hipServer.url("").toString()).build();
        var provider = provider().identifiers(of(official)).build();
        var providerAsJson = new ObjectMapper().writeValueAsString(provider);
        clientRegistryServer.enqueue(
                new MockResponse().setHeader("Content-Type", "application/json").setBody(providerAsJson));

        var linkRes = patientLinkResponse().build();
        var linkResJson = new ObjectMapper().writeValueAsString(linkRes);
        hipServer.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(linkResJson));
        webTestClient
                .post()
                .uri("/patients/link/link-ref-num")
                .header("Authorization", "MTIzNDU2Nzg5")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(patientLinkReferenceRequest().build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json(linkResJson);
    }

    @Test
    public void shouldGiveOtpInvalidError() throws IOException {
        var official = identifier().use("official").system(hipServer.url("").toString()).build();
        var provider = provider().identifiers(of(official)).build();
        var providerAsJson = new ObjectMapper().writeValueAsString(provider);
        clientRegistryServer.enqueue(
                new MockResponse().setHeader("Content-Type", "application/json").setBody(providerAsJson));

        var errorResponse = errorRepresentation().build();
        var errorResponseJson = new ObjectMapper().writeValueAsString(errorResponse);
        hipServer.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setStatus("HTTP/1.1 404")
                        .setBody(errorResponseJson));
        webTestClient
                .post()
                .uri("/patients/link/link-ref-num")
                .header("Authorization", "MTIzNDU2Nzg5")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(patientLinkReferenceRequest().build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .json(errorResponseJson);
    }

    @AfterAll
    public static void tearDown() throws IOException {
        clientRegistryServer.shutdown();
        hipServer.shutdown();
    }

  public static class ContextInitializer
      implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      TestPropertyValues values =
          TestPropertyValues.of(
              Stream.of("hdaf.clientregistry.url=" + clientRegistryServer.url("")));
      values.applyTo(applicationContext);
    }
  }
}
