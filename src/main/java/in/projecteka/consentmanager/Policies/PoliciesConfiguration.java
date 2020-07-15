package in.projecteka.consentmanager.Policies;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PoliciesConfiguration {
    @Bean
    public PolicyService policyService() {
        return new PolicyService();
    }
}
