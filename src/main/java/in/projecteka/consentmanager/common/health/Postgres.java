package in.projecteka.consentmanager.common.health;

import in.projecteka.consentmanager.DbOptions;
import lombok.AllArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.io.IOException;

@AllArgsConstructor
public class Postgres implements HealthIndicator {
    private DbOptions dbOptions;

    @Override
    public Health health() {
        try {
            return isPostgresUp();
        } catch (Exception e) {
            return Health.down().withDetail("Error", "Keycloak unavailable").build();
        }
    }

    private Health isPostgresUp() throws IOException, InterruptedException {
        String cmd = String.format("pg_isready -h %s -p %s", dbOptions.getHost(), dbOptions.getPort());
        Runtime run = Runtime.getRuntime();
        Process pr = run.exec(cmd);
        int exitValue = pr.waitFor();
        if (exitValue == 0) {
            return Health.up().build();
        }
        return Health.down().withDetail("Error", "Postgres is down").build();
    }
}
