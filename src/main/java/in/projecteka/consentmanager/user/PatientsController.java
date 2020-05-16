package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.ClientError;
import in.projecteka.consentmanager.clients.model.Error;
import in.projecteka.consentmanager.clients.model.ErrorRepresentation;
import in.projecteka.consentmanager.clients.model.Session;
import in.projecteka.consentmanager.common.Caller;
import in.projecteka.consentmanager.common.cache.CacheAdapter;
import in.projecteka.consentmanager.user.model.CreatePinRequest;
import in.projecteka.consentmanager.user.model.GenerateOtpRequest;
import in.projecteka.consentmanager.user.model.GenerateOtpResponse;
import in.projecteka.consentmanager.user.model.Identifier;
import in.projecteka.consentmanager.user.model.IdentifierType;
import in.projecteka.consentmanager.user.model.OtpMediumType;
import in.projecteka.consentmanager.user.model.OtpVerification;
import in.projecteka.consentmanager.user.model.Profile;
import in.projecteka.consentmanager.user.model.SignUpRequest;
import in.projecteka.consentmanager.user.model.Token;
import in.projecteka.consentmanager.user.model.UpdateUserRequest;
import in.projecteka.consentmanager.user.model.UserSignUpEnquiry;
import in.projecteka.consentmanager.user.model.ValidatePinRequest;
import in.projecteka.consentmanager.user.model.ChangePinRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

import static in.projecteka.consentmanager.clients.ClientError.invalidRequester;
import static in.projecteka.consentmanager.clients.model.ErrorCode.INVALID_REQUESTER;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/patients")
@AllArgsConstructor
public class PatientsController {
    private final ProfileService profileService;
    private final TransactionPinService transactionPinService;
    private final SignUpService signupService;
    private final UserService userService;
    private final CacheAdapter<String, String> usedTokens;

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/pin")
    public Mono<Void> pin(@RequestBody CreatePinRequest createPinRequest) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getUsername)
                .flatMap(userName -> transactionPinService.createPinFor(userName, createPinRequest.getPin()));
    }

    @GetMapping("/me")
    public Mono<Profile> profileFor() {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getUsername)
                .flatMap(profileService::profileFor);
    }

    @PostMapping("/verify-pin")
    public Mono<Token> validatePin(@Valid @RequestBody ValidatePinRequest request) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .flatMap(caller -> transactionPinService.validatePinFor(caller.getUsername(),
                        request.getPin(),
                        request.getRequestId(),
                        request.getScope()));
    }

    @PostMapping("/profile")
    public Mono<Session> create(@RequestBody SignUpRequest request,
                                @RequestHeader(name = "Authorization") String token) {
        var signUpRequests = SignUpRequestValidator.validate(request, userService.getUserIdSuffix());
        return signUpRequests.isValid()
                ? Mono.justOrEmpty(signupService.sessionFrom(token))
                .flatMap(sessionId -> userService.create(signUpRequests.get(), sessionId)
                        .zipWith(Mono.just(sessionId))
                        .flatMap(tuple -> signupService.removeOf(tuple.getT2()).thenReturn(tuple.getT1())))
                : Mono.error(new ClientError(BAD_REQUEST,
                new ErrorRepresentation(new Error(INVALID_REQUESTER,
                        signUpRequests.getError().reduce((left, right) -> format("%s, %s", left, right))))));
    }

    @PostMapping("/generateotp")
    @ResponseStatus(CREATED)
    public Mono<GenerateOtpResponse> generateOtp(@RequestBody GenerateOtpRequest request) {
        return profileService.profileFor(request.getUsername())
                .switchIfEmpty(Mono.error(ClientError.userNotFound()))
                .flatMap(this::getVerfiedIdentifier)
                .switchIfEmpty(Mono.error(ClientError.unAuthorized()))
                .flatMap(verifiedIdentifier -> getGenerateOtpResponse(verifiedIdentifier, request.getUsername()))
                .switchIfEmpty(Mono.error(ClientError.failedToGenerateOtp()));
    }

    @PostMapping("/verifyotp")
    public Mono<Token> verifyOtp(@RequestBody OtpVerification request) {
        return userService.verifyOtp(request);
    }

    private Mono<GenerateOtpResponse> getGenerateOtpResponse(UserSignUpEnquiry userSignUpEnquiry, String userName) {
        return userService.sendOtpForPasswordChange(userSignUpEnquiry, userName)
                .flatMap(signUpSession -> {
                    OtpMediumType otpMediumType = getOtpMedium(userSignUpEnquiry);
                    String maskedIdentifier = maskedIdentifier(userSignUpEnquiry);

                    if (signUpSession == null || otpMediumType == null || maskedIdentifier == null) {
                        return Mono.empty();
                    }
                    return Mono.just(GenerateOtpResponse.builder()
                            .sessionId(signUpSession.getSessionId())
                            .otpMediumValue(maskedIdentifier)
                            .otpMedium(otpMediumType.toString())
                            .expiryInMinutes(userService.getExpiryInMinutes())
                            .build());
                });
    }

    private String maskedIdentifier(UserSignUpEnquiry userSignUpEnquiry) {
        String maskedIdentifier = userSignUpEnquiry.getIdentifier();
        if (userSignUpEnquiry.getIdentifierType().equals(IdentifierType.MOBILE.toString())) {
            if (maskedIdentifier != null && !maskedIdentifier.equals("")) {
                return maskedIdentifier.replaceAll("\\d(?=(?:\\D*\\d){4})", "*");
            }
        }
        return null;
    }

    private OtpMediumType getOtpMedium(UserSignUpEnquiry userSignUpEnquiry) {
        String identifierType = userSignUpEnquiry.getIdentifierType();
        if (identifierType != null && identifierType.equals(IdentifierType.MOBILE.toString())) {
            return OtpMediumType.MOBILE;
        }
        return null;
    }

    private Mono<UserSignUpEnquiry> getVerfiedIdentifier(Profile profile) {
        Identifier verifiedIdentifier;
        if (profile.getVerifiedIdentifiers() != null && !profile.getVerifiedIdentifiers().isEmpty()) {
            List<Identifier> mobileIdentifiers = profile.getVerifiedIdentifiers().stream().filter(identifier ->
                    identifier.getType() == IdentifierType.MOBILE).collect(Collectors.toList());
            verifiedIdentifier = mobileIdentifiers.isEmpty() ?
                    profile.getVerifiedIdentifiers().get(0) : mobileIdentifiers.get(0);

            return Mono.just(UserSignUpEnquiry.builder()
                    .identifier(verifiedIdentifier.getValue())
                    .identifierType(verifiedIdentifier.getType().toString())
                    .build());
        }

        return Mono.error(ClientError.unAuthorized());
    }

    @PutMapping("/profile/reset-password")
    public Mono<Session> update(@RequestBody UpdateUserRequest request,
                                @RequestHeader(name = "Authorization") String token) {
        var updateUserRequests = SignUpRequestValidator.validatePassword(request.getPassword());
        return updateUserRequests.isValid()
                ? Mono.justOrEmpty(signupService.sessionFrom(token))
                .flatMap(sessionId -> userService.update(request, sessionId)
                        .zipWith(Mono.just(sessionId))
                        .flatMap(tuple -> signupService.removeOf(tuple.getT2()).thenReturn(tuple.getT1())))
                : Mono.error(invalidRequester(updateUserRequests.getError()));
    }

    @PostMapping("/change-pin")
    public Mono<Void> changeTransactionPin(@RequestBody ChangePinRequest request) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .flatMap(caller ->
                        transactionPinService.changeTransactionPinFor(caller.getUsername(),
                                request.getPin())
                                .switchIfEmpty(Mono.defer(() -> usedTokens.put(caller.getSessionId(), ""))));
    }
}
