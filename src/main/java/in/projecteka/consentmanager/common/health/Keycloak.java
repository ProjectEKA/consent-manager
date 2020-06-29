package in.projecteka.consentmanager.common.health;

import in.projecteka.consentmanager.clients.properties.IdentityServiceProperties;
import lombok.AllArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@AllArgsConstructor
public class Keycloak implements HealthIndicator {
    private IdentityServiceProperties identityServiceProperties;

    @Override
    public Health health() {
        try {
            return isKeycloakUp();
        } catch (Exception e) {
            return Health.down().withDetail("Error", "Keycloak unavailable").build();
        }
    }

    private Health isKeycloakUp() throws IOException {
        URL siteUrl = new URL(identityServiceProperties.getBaseUrl());
        HttpURLConnection httpURLConnection = (HttpURLConnection) siteUrl.openConnection();
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.connect();
        int responseCode = httpURLConnection.getResponseCode();
        if (responseCode == 200) {
            return Health.up().build();
        }
        return Health.down().withDetail("Error", "Keycloak is down").build();
    }
}
