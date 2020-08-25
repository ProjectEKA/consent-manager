package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.user.model.ChangePinRequest;
import in.projecteka.consentmanager.user.model.CreatePinRequest;
import in.projecteka.consentmanager.user.model.GenerateOtpRequest;
import in.projecteka.consentmanager.user.model.GenerateOtpResponse;
import in.projecteka.consentmanager.user.model.Identifier;
import in.projecteka.consentmanager.user.model.IdentifierGroup;
import in.projecteka.consentmanager.user.model.IdentifierType;
import in.projecteka.consentmanager.user.model.InitiateCmIdRecoveryRequest;
import in.projecteka.consentmanager.user.model.LoginModeResponse;
import in.projecteka.consentmanager.user.model.OtpAttempt;
import in.projecteka.consentmanager.user.model.OtpMediumType;
import in.projecteka.consentmanager.user.model.OtpVerification;
import in.projecteka.consentmanager.user.model.Profile;
import in.projecteka.consentmanager.user.model.RecoverCmIdResponse;
import in.projecteka.consentmanager.user.model.SendOtpAction;
import in.projecteka.consentmanager.user.model.SignUpRequest;
import in.projecteka.consentmanager.user.model.Token;
import in.projecteka.consentmanager.user.model.UpdatePasswordRequest;
import in.projecteka.consentmanager.user.model.UpdateUserRequest;
import in.projecteka.consentmanager.user.model.UserSignUpEnquiry;
import in.projecteka.consentmanager.user.model.ValidatePinRequest;
import in.projecteka.library.clients.model.ClientError;
import in.projecteka.library.clients.model.Error;
import in.projecteka.library.clients.model.ErrorRepresentation;
import in.projecteka.library.clients.model.Session;
import in.projecteka.library.common.Caller;
import in.projecteka.library.common.cache.CacheAdapter;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static in.projecteka.consentmanager.user.model.OtpAttempt.Action.OTP_REQUEST_RECOVER_PASSWORD;
import static in.projecteka.consentmanager.user.model.SendOtpAction.RECOVER_PASSWORD;
import static in.projecteka.library.clients.model.ClientError.invalidRequester;
import static in.projecteka.library.clients.model.ErrorCode.INVALID_REQUESTER;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping(Constants.BASE_PATH_PATIENTS_APIS)
@AllArgsConstructor
@ConditionalOnExpression("${consentmanager.userservice.enabled:true}")
public class PatientController {
    private final ProfileService profileService;
    private final TransactionPinService transactionPinService;
    private final SignUpService signupService;
    private final UserService userService;
    private final CacheAdapter<String, String> usedTokens;
    private final LockedUserService lockedUserService;

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping(Constants.APP_PATH_CREATE_PIN)
    public Mono<Void> pin(@RequestBody CreatePinRequest createPinRequest) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getUsername)
                .flatMap(userName -> transactionPinService.createPinFor(userName, createPinRequest.getPin()));
    }

    @PostMapping(Constants.APP_PATH_FORGET_PIN_GENERATE_OTP)
    @ResponseStatus(CREATED)
    public Mono<GenerateOtpResponse> generateOtpForForgetPin() {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getUsername)
                .flatMap(userService::userWith)
                .flatMap(user -> getGenerateOtpResponseFor(
                        new UserSignUpEnquiry(IdentifierType.MOBILE.toString(), user.getPhone()),
                        user.getIdentifier(),
                        OtpAttempt.Action.OTP_REQUEST_FORGOT_CONSENT_PIN,
                        SendOtpAction.FORGOT_CONSENT_PIN));
    }

    @PostMapping(Constants.APP_PATH_FORGET_PIN_VALIDATE_OTP)
    public Mono<Token> verifyOtpForForgetPin(@RequestBody OtpVerification otpVerification) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getUsername)
                .flatMap(username -> userService.verifyOtpForForgotConsentPin(otpVerification));
    }

    @PutMapping(Constants.APP_PATH_RESET_PIN)
    @ResponseStatus(NO_CONTENT)
    public Mono<Void> resetConsentPin(@RequestBody ChangePinRequest changePinRequest,
                                      @RequestHeader(name = "Authorization") String token) {
        var sessionId = signupService.sessionFrom(token);
        return Mono.justOrEmpty(sessionId)
                .flatMap(signupService::getUserName)
                .flatMap(username -> transactionPinService.changeTransactionPinFor(username, changePinRequest.getPin()))
                .then(Mono.defer(() -> signupService.removeOf(sessionId)));
    }

    @GetMapping(Constants.APP_PATH_GET_PROFILE)
    public Mono<Profile> profileFor() {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getUsername)
                .flatMap(profileService::profileFor);
    }

    @GetMapping(Constants.APP_PATH_GET_PROFILE_LOGINMODE)
    public Mono<LoginModeResponse> fetchLoginMode(@RequestParam String userName) {
        return userService.getLoginMode(userName);
    }

    @PostMapping(Constants.APP_PATH_VERIFY_PIN)
    public Mono<Token> validatePin(@Valid @RequestBody ValidatePinRequest request) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .flatMap(caller -> transactionPinService.validatePinFor(caller.getUsername(),
                        request.getPin(),
                        request.getRequestId(),
                        request.getScope()));
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(Constants.APP_PATH_CREATE_USER)
    public Mono<Void> create(@RequestBody SignUpRequest request,
                             @RequestHeader(name = "Authorization") String token) {
        var signUpRequests = SignUpRequestValidator.validate(request, userService.getUserIdSuffix());
        return signUpRequests.isValid()
                ? Mono.justOrEmpty(signupService.sessionFrom(token))
                .flatMap(sessionId -> userService.create(signUpRequests.get(), sessionId)
                        .then(signupService.removeOf(sessionId)))
                : Mono.error(new ClientError(BAD_REQUEST,
                new ErrorRepresentation(new Error(INVALID_REQUESTER,
                        signUpRequests.getError().reduce((left, right) -> format("%s, %s", left, right))))));
    }

    @PostMapping(Constants.APP_PATH_GENERATE_OTP)
    @ResponseStatus(CREATED)
    public Mono<GenerateOtpResponse> generateOtp(@RequestBody GenerateOtpRequest request) {
        return profileService.profileFor(request.getUsername())
                .switchIfEmpty(Mono.error(ClientError.userNotFound()))
                .flatMap(this::getVerifiedIdentifier)
                .switchIfEmpty(Mono.error(ClientError.unAuthorized()))
                .flatMap(verifiedIdentifier -> getGenerateOtpResponseFor(verifiedIdentifier,
                        request.getUsername(),
                        OTP_REQUEST_RECOVER_PASSWORD,
                        RECOVER_PASSWORD))
                .switchIfEmpty(Mono.error(ClientError.failedToGenerateOtp()));
    }

    @PostMapping(Constants.APP_PATH_VERIFY_OTP)
    public Mono<Token> verifyOtp(@RequestBody OtpVerification request) {
        return userService.verifyOtpForForgetPassword(request);
    }

    private Mono<GenerateOtpResponse> getGenerateOtpResponseFor(UserSignUpEnquiry userSignUpEnquiry,
                                                                String userName,
                                                                OtpAttempt.Action otpAttemptAction,
                                                                SendOtpAction sendOtpAction) {
        return userService.sendOtpFor(userSignUpEnquiry, userName, otpAttemptAction, sendOtpAction)
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
        if (userSignUpEnquiry.getIdentifierType().equals(IdentifierType.MOBILE.toString())
                && maskedIdentifier != null
                && !maskedIdentifier.equals("")) {
            return maskedIdentifier.replaceAll("\\d(?=(?:\\D*\\d){4})", "*");
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

    private Mono<UserSignUpEnquiry> getVerifiedIdentifier(Profile profile) {
        if (profile.getVerifiedIdentifiers() != null && !profile.getVerifiedIdentifiers().isEmpty()) {
            List<Identifier> mobileIdentifiers = profile.getVerifiedIdentifiers()
                    .stream()
                    .filter(identifier -> identifier.getType() == IdentifierType.MOBILE)
                    .collect(Collectors.toList());
            var verifiedIdentifier = mobileIdentifiers.isEmpty()
                    ? profile.getVerifiedIdentifiers().get(0)
                    : mobileIdentifiers.get(0);

            return Mono.just(UserSignUpEnquiry.builder()
                    .identifier(verifiedIdentifier.getValue())
                    .identifierType(verifiedIdentifier.getType().toString())
                    .build());
        }

        return Mono.error(ClientError.unAuthorized());
    }

    @PutMapping(Constants.APP_PATH_RESET_PASSWORD)
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

    @PutMapping(Constants.APP_PATH_UPDATE_PROFILE_PASSWORD)
    public Mono<Session> updatePassword(@RequestBody UpdatePasswordRequest request) {
        var updatePasswordRequest = SignUpRequestValidator.validatePassword(request.getNewPassword());
        return updatePasswordRequest.isValid()
                ? ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .map(Caller::getUsername)
                .flatMap(lockedUserService::validateLogin)
                .flatMap(userName -> Mono.defer(() -> userService.updatePassword(request, userName)))
                : Mono.error(invalidRequester(updatePasswordRequest.getError()));
    }

    @PostMapping(Constants.APP_PATH_CHANGE_PIN)
    public Mono<Void> changeTransactionPin(@RequestBody ChangePinRequest request) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (Caller) securityContext.getAuthentication().getPrincipal())
                .flatMap(caller ->
                        transactionPinService.changeTransactionPinFor(caller.getUsername(),
                                request.getPin())
                                .switchIfEmpty(Mono.defer(() -> usedTokens.put(caller.getSessionId(), ""))));
    }

    @PostMapping(Constants.APP_PATH_PROFILE_RECOVERY_INIT)
    public Mono<GenerateOtpResponse> initiateCmIdRecovery(@RequestBody InitiateCmIdRecoveryRequest request) {
        return isValidRecoveryRequest(request)
                .switchIfEmpty(Mono.error(ClientError.invalidRecoveryRequest()))
                .flatMap(userService::getPatientByDetails)
                .switchIfEmpty(Mono.defer(() -> Mono.error(ClientError.noPatientFound())))
                .flatMap(user -> getGenerateOtpResponseFor(
                        UserSignUpEnquiry.builder()
                                .identifierType(IdentifierType.MOBILE.toString())
                                .identifier(user.getPhone())
                                .build(),
                        user.getIdentifier(),
                        OtpAttempt.Action.OTP_REQUEST_RECOVER_CM_ID,
                        SendOtpAction.RECOVER_CM_ID))
                .switchIfEmpty(Mono.defer(() -> Mono.error(ClientError.failedToGenerateOtp())));
    }

    private boolean isInvalidIdentifierMapped(List<Identifier> identifiers, IdentifierGroup identifierGroup) {
        return identifiers.stream()
                .anyMatch(identifier -> !identifier.getType().getIdentifierGroup().equals(identifierGroup)
                        || !identifier.getType().isValid(identifier.getValue()));
    }

    private Mono<InitiateCmIdRecoveryRequest> isValidRecoveryRequest(InitiateCmIdRecoveryRequest request) { //breakdown
        Predicate<InitiateCmIdRecoveryRequest> areMandatoryFieldsNull = req -> req.getName() == null
                || req.getGender() == null
                || !IdentifierUtils.isIdentifierTypePresent(req.getVerifiedIdentifiers(), IdentifierType.MOBILE);
        Predicate<InitiateCmIdRecoveryRequest> isInvalidVerifiedIdentifierMapped = req ->
                isInvalidIdentifierMapped(req.getVerifiedIdentifiers(), IdentifierGroup.VERIFIED_IDENTIFIER);
        Predicate<InitiateCmIdRecoveryRequest> isInvalidUnverifiedIdentifierMapped = req ->
                isInvalidIdentifierMapped(req.getUnverifiedIdentifiers(), IdentifierGroup.UNVERIFIED_IDENTIFIER);
        return areMandatoryFieldsNull
                .or(isInvalidVerifiedIdentifierMapped)
                .or(isInvalidUnverifiedIdentifierMapped).test(request)
                ? Mono.empty()
                : Mono.just(request);
    }

    @PostMapping(Constants.APP_PATH_PROFILE_RECOVERY_CONFIRM)
    public Mono<RecoverCmIdResponse> verifyOtpAndRecoverCmId(@RequestBody OtpVerification request) {
        return userService.verifyOtpForRecoverCmId(request);
    }
}
