package in.projecteka.consentmanager.userauth;

import in.projecteka.consentmanager.clients.UserAuthServiceClient;
import in.projecteka.consentmanager.userauth.model.AuthInitRequest;
import in.projecteka.consentmanager.userauth.model.AuthMode;
import in.projecteka.consentmanager.userauth.model.FetchAuthModesRequest;
import in.projecteka.consentmanager.userauth.model.FetchAuthModesResponse;
import in.projecteka.consentmanager.userauth.model.AuthPurpose;
import in.projecteka.consentmanager.userauth.model.Requester;
import in.projecteka.consentmanager.userauth.model.UserAuthConfirmRequest;
import in.projecteka.library.clients.model.GatewayResponse;
import in.projecteka.library.clients.model.RespError;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.UUID;

import static in.projecteka.library.clients.model.ErrorCode.INVALID_PURPOSE;

@AllArgsConstructor
public class UserAuthentication {
    HIPLinkInitAction initAction;
    UserAuthServiceClient serviceClient;
    private final Logger logger = LoggerFactory.getLogger(UserAuthentication.class);

    public Mono<Void> authInit(AuthInitRequest request) {
        logger.info("User auth initialization");
        return initAction.execute(request);
    }

    public Mono<Void> fetchAuthModes(FetchAuthModesRequest request) {
        Requester requester = request.getQuery().getRequester();
        var purpose = request.getQuery().getPurpose();
        if (purpose.equals(AuthPurpose.INVALID_PURPOSE)) {
            logger.error("[UserService] -> Invalid Purpose specified for fetchAuthModes. Request Id {}", request.getRequestId());
            sendAuthFetchErrorToRequester(requester, request.getRequestId());
        } else {
            sendAuthFetchResultToRequester(request);
        }
        return Mono.empty();
    }

    private void sendAuthFetchResultToRequester(FetchAuthModesRequest request) {
        var requester = request.getQuery().getRequester();
        var authResponse = FetchAuthModesResponse.AuthResponse.builder()
                .modes(Collections.singletonList(AuthMode.MOBILE_OTP))
                .purpose(request.getQuery().getPurpose())
                .build();
        var response = FetchAuthModesResponse
                .builder()
                .requestId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                .auth(authResponse)
                .error(null)
                .resp(new GatewayResponse(request.getRequestId()))
                .build();
        serviceClient.sendAuthModesResponseToGateway(response,
                requester.getType().getRoutingKey(),
                requester.getId()).subscribe();

    }

    private void sendAuthFetchErrorToRequester(Requester requester, String requestId) {
        var response = FetchAuthModesResponse
                .builder()
                .requestId(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now(ZoneOffset.UTC))
                .auth(null)
                .error(new RespError(INVALID_PURPOSE.getValue(), "Invalid purpose"))
                .resp(new GatewayResponse(requestId))
                .build();
        serviceClient.sendAuthModesResponseToGateway(response,
                requester.getType().getRoutingKey(),
                requester.getId()).subscribe();
    }

    public Mono<Void> authConfirm(UserAuthConfirmRequest request) {
        //To-do
        logger.info("Confirming authentication {}", request);
        return Mono.empty();
    }
}
