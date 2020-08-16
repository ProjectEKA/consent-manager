package in.projecteka.library.clients.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConstructorBinding;

@Getter
@AllArgsConstructor
@ConstructorBinding
public class ServiceCredential {
    private final String clientSecret;
    private final String clientId;
}
