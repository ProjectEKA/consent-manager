package in.projecteka.consentmanager.common.heartbeat;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import in.projecteka.consentmanager.DbOptions;
import in.projecteka.consentmanager.clients.model.Error;
import in.projecteka.consentmanager.clients.model.ErrorCode;
import in.projecteka.consentmanager.clients.properties.IdentityServiceProperties;
import in.projecteka.consentmanager.common.cache.RedisOptions;
import in.projecteka.consentmanager.common.heartbeat.model.HeartbeatResponse;
import in.projecteka.consentmanager.common.heartbeat.model.Status;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.util.concurrent.TimeoutException;

import static in.projecteka.consentmanager.common.Constants.GUAVA;
import static in.projecteka.consentmanager.common.Constants.SERVICE_DOWN;
import static in.projecteka.consentmanager.common.heartbeat.model.Status.UP;
import static java.lang.String.valueOf;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.time.LocalDateTime.now;
import static java.time.ZoneOffset.UTC;
import static org.springframework.http.HttpMethod.GET;
import static reactor.core.publisher.Mono.just;

@AllArgsConstructor
public class Heartbeat {
    private final IdentityServiceProperties identityServiceProperties;
    private final DbOptions dbOptions;
    private final RabbitmqOptions rabbitmqOptions;
    private final RedisOptions redisOptions;
    private final CacheMethodProperty cacheMethodProperty;

    public Mono<HeartbeatResponse> getStatus() {
        try {
            return isPostgresUp() && isKeycloakUp() && isRabbitMQUp() && isCacheUp()
                   ? just(HeartbeatResponse.builder().timeStamp(now(UTC).toString()).status(UP).build())
                   : just(HeartbeatResponse.builder()
                           .timeStamp(now(UTC).toString())
                           .status(Status.DOWN)
                           .error(Error.builder().code(ErrorCode.SERVICE_DOWN).message(SERVICE_DOWN).build())
                           .build());
        } catch (IOException | TimeoutException e) {
            return just(HeartbeatResponse.builder()
                    .timeStamp(now(UTC).toString())
                    .status(Status.DOWN)
                    .error(Error.builder().code(ErrorCode.SERVICE_DOWN).message(SERVICE_DOWN).build())
                    .build());
        }
    }

    private boolean isCacheUp() throws IOException {
        return cacheMethodProperty.getMethodName().equalsIgnoreCase(GUAVA)
                || checkConnection(redisOptions.getHost(), redisOptions.getPort());
    }

    private boolean isRabbitMQUp() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitmqOptions.getHost());
        factory.setPort(rabbitmqOptions.getPort());
        try (Connection connection = factory.newConnection()) {
            return connection.isOpen();
        }
    }

    private boolean isKeycloakUp() throws IOException {
        var siteUrl = new URL(identityServiceProperties.getBaseUrl());
        HttpURLConnection httpURLConnection = (HttpURLConnection) siteUrl.openConnection();
        httpURLConnection.setRequestMethod(valueOf(GET));
        httpURLConnection.connect();
        return httpURLConnection.getResponseCode() == HTTP_OK;
    }

    private boolean isPostgresUp() throws IOException {
        return checkConnection(dbOptions.getHost(), dbOptions.getPort());
    }

    private boolean checkConnection(String host, int port) throws IOException {
        SocketAddress socketAddress = new InetSocketAddress(host, port);
        try (Socket socket = new Socket()) {
            socket.connect(socketAddress);
            return socket.isConnected();
        }
    }
}
