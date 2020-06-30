package in.projecteka.consentmanager.common.heartbeat;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import in.projecteka.consentmanager.DbOptions;
import in.projecteka.consentmanager.clients.model.Error;
import in.projecteka.consentmanager.clients.model.ErrorCode;
import in.projecteka.consentmanager.clients.properties.IdentityServiceProperties;
import in.projecteka.consentmanager.common.heartbeat.model.HeartbeatResponse;
import in.projecteka.consentmanager.common.heartbeat.model.Status;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

@AllArgsConstructor
public class Heartbeat {
    private IdentityServiceProperties identityServiceProperties;
    private DbOptions dbOptions;
    private RabbitmqOptions rabbitmqOptions;

    public Mono<HeartbeatResponse> getStatus() {
        try {
            return isPostgresUp() && isKeycloakUp() && isRabbitMQUp() && isRedisUp()
                    ? Mono.just(HeartbeatResponse.builder()
                    .timeStamp(Instant.now().toString())
                    .status(Status.UP)
                    .build())
                    : Mono.just(HeartbeatResponse.builder()
                    .timeStamp(Instant.now().toString())
                    .status(Status.DOWN)
                    .error(Error.builder().code(ErrorCode.SERVICE_DOWN).message("Service down").build())
                    .build());
        } catch (IOException | InterruptedException | TimeoutException e) {
            return Mono.just(HeartbeatResponse.builder()
                    .timeStamp(Instant.now().toString())
                    .status(Status.DOWN)
                    .error(Error.builder().code(ErrorCode.SERVICE_DOWN).message(e.getMessage()).build())
                    .build());
        }
    }

    private boolean isRedisUp() throws IOException, InterruptedException {
        String command = "redis-cli ping";
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec(command);
        int exitValue = process.waitFor();
        return exitValue == 0;
    }

    private boolean isRabbitMQUp() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitmqOptions.getHost());
        factory.setPort(rabbitmqOptions.getPort());
        Connection connection = factory.newConnection();
        return connection.isOpen();
    }

    private boolean isKeycloakUp() throws IOException {
        URL siteUrl = new URL(identityServiceProperties.getBaseUrl());
        HttpURLConnection httpURLConnection = (HttpURLConnection) siteUrl.openConnection();
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.connect();
        int responseCode = httpURLConnection.getResponseCode();
        return responseCode == 200;
    }

    private boolean isPostgresUp() throws IOException, InterruptedException {
        String cmd = String.format("pg_isready -h %s -p %s", dbOptions.getHost(), dbOptions.getPort());
        Runtime run = Runtime.getRuntime();
        Process pr = run.exec(cmd);
        int exitValue = pr.waitFor();
        return exitValue == 0;
    }
}
