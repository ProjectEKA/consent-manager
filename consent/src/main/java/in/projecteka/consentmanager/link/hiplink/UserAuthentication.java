package in.projecteka.consentmanager.link.hiplink;

import in.projecteka.consentmanager.link.hiplink.model.request.AuthInitRequest;
import in.projecteka.consentmanager.user.model.UserAuthConfirmRequest;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class UserAuthentication {
    HIPLinkInitAction initAction;
    private final Logger logger = LoggerFactory.getLogger(UserAuthentication.class);

    public Mono<Void> authInit(AuthInitRequest request) {
        logger.info("User auth initialization");
        return initAction.execute(request);
    }

    public Mono<Void> confirmAuthFor(UserAuthConfirmRequest request) {
        return Mono.empty();
    }
}
