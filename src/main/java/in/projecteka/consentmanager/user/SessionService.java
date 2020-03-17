package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.common.TokenService;
import in.projecteka.consentmanager.user.model.SessionRequest;
import lombok.AllArgsConstructor;
import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class SessionService {

    private final TokenService tokenService;
    private final Logger logger = Logger.getLogger(SessionService.class);

    public Mono<Session> forNew(SessionRequest request) {
        if (StringUtils.isEmpty(request.getUserName()) || StringUtils.isEmpty(request.getPassword()))
            return Mono.error(ClientError.unAuthorizedRequest());
        return tokenService.tokenForUser(request.getUserName(), request.getPassword())
                .doOnError(logger::error)
                .onErrorResume(error -> Mono.error(ClientError.unAuthorizedRequest()));
    }
}
