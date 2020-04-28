package in.projecteka.consentmanager.user;

import com.google.common.base.Strings;
import in.projecteka.consentmanager.user.model.CoreSignUpRequest;
import in.projecteka.consentmanager.user.model.Gender;
import in.projecteka.consentmanager.user.model.Identifier;
import in.projecteka.consentmanager.user.model.IdentifierType;
import in.projecteka.consentmanager.user.model.SignUpIdentifier;
import in.projecteka.consentmanager.user.model.SignUpRequest;
import io.vavr.collection.CharSeq;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import org.passay.CharacterRule;
import org.passay.CharacterSequence;
import org.passay.EnglishCharacterData;
import org.passay.IllegalSequenceRule;
import org.passay.LengthRule;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.RuleResult;
import org.passay.RuleResultDetail;
import org.passay.SequenceData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class SignUpRequestValidator {

    private SignUpRequestValidator() {
    }

    private static final LocalDate TODAY = LocalDate.now();
    private static final String VALID_NAME_CHARS = "[a-zA-Z ]";
    private static final List<IdentifierType> UniqueIdentifiers = List.of(IdentifierType.ABPMJAYID);

    public static Validation<Seq<String>, CoreSignUpRequest> validate(SignUpRequest signUpRequest,
                                                                      String userIdSuffix) {
        return Validation.combine(
                validateName(signUpRequest.getName()),
                validate(signUpRequest.getGender()),
                validateUserName(signUpRequest.getUsername(), userIdSuffix),
                validatePassword(signUpRequest.getPassword()),
                validateYearOfBirth(signUpRequest.getYearOfBirth()),
                validateUnVerifiedIdentifiers(signUpRequest.getUnverifiedIdentifiers()))
                .ap((firstName, gender, username, password, dateOfBirth, unverifiedIdentifiers) ->
                        CoreSignUpRequest.builder()
                                .name(firstName)
                                .gender(gender)
                                .username(username)
                                .password(password)
                                .yearOfBirth(dateOfBirth)
                                .unverifiedIdentifiers(unverifiedIdentifiers)
                                .build());
    }

    protected static Validation<String, List<Identifier>> validateUnVerifiedIdentifiers(
            List<SignUpIdentifier> unverifiedIdentifiers) {
        if (unverifiedIdentifiers == null || unverifiedIdentifiers.isEmpty()) {
            return Validation.valid(new ArrayList<>());
        }

        var identifierValidated = new ArrayList<Identifier>();
        String error = "";
        for (var identifier : unverifiedIdentifiers) {
            try {
                IdentifierType identifierType = IdentifierType.valueOf(identifier.getType());
                if (identifierType.isValid(identifier.getValue())) {
                    identifierValidated.add(new Identifier(identifierType, identifier.getValue()));
                } else {
                    error = error.concat(format("{%s} is invalid value for type %s ",
                            identifier.getValue(),
                            identifierType));
                }
            } catch (IllegalArgumentException | NullPointerException ex) {
                error = error.concat(format("Invalid identifier type {%s} ", identifier.getType()));
            }
        }
        if (!error.equals("")) {
            return Validation.invalid(error);
        }

        List<IdentifierType> validatedIdentifierTypes = identifierValidated.stream()
                .map(identifier -> identifier.getType())
                .distinct()
                .collect(Collectors.toList());
        for (IdentifierType identifierType: validatedIdentifierTypes) {
            if (!UniqueIdentifiers.contains(identifierType)) {
                continue;
            }
            if (!isUnique(identifierValidated, identifierType)) {
                error = error.concat(
                        format("Only one identifier type {%s} is allowed", identifierType));
            }
        }
        if (!error.equals("")) {
            return Validation.invalid(error);
        }
        return Validation.valid(identifierValidated);
    }

    private static boolean isUnique(List<Identifier> unverifiedIdentifiers,
                                    IdentifierType identifierType) {
        return unverifiedIdentifiers.stream().
                filter(identifier -> identifier.getType().equals(identifierType))
                .count() == 1;
    }

    private static Validation<String, Gender> validate(Gender gender) {
        if (gender != null) {
            return Validation.valid(gender);
        }
        return Validation.invalid("gender can't be empty");
    }

    private static Validation<String, String> validateName(String name) {
        if (Strings.isNullOrEmpty(name)) {
            return Validation.invalid("Name can't be empty");
        }
        return allowed(VALID_NAME_CHARS, "name", name);
    }

    private static Validation<String, String> validateUserName(String username, String userIdSuffix) {
        final String VALID_USERNAME_CHARS = "[a-zA-Z@0-9.\\-]";
        if (Strings.isNullOrEmpty(username)) {
            return Validation.invalid("username can't be empty");
        }
        return allowed(VALID_USERNAME_CHARS, "username", username)
                .combine(endsWithProvider(username, userIdSuffix))
                .combine(lengthLimitFor(username.replace(userIdSuffix, "")))
                .ap((validCharacters, validEndsWith, validLength) -> username)
                .mapError(errors -> errors.reduce((left, right) -> format("%s, %s", left, right)));
    }

    private static Validation<String, String> endsWithProvider(String username, String userIdSuffix) {
        if (!username.endsWith(userIdSuffix)) {
            return Validation.invalid("username does not end with " + userIdSuffix);
        }
        return Validation.valid(username);
    }

    private static Validation<String, String> lengthLimitFor(String username) {
        if (username.length() < 3 || username.length() > 150) {
            return Validation.invalid(format("%s should be between %d and %d characters", "username", 3, 150));
        }
        return Validation.valid(username);
    }

    private static Validation<String, String> allowed(String characters, String fieldName, String value) {
        return CharSeq.of(value)
                .replaceAll(characters, "")
                .transform(seq -> seq.isEmpty()
                                  ? Validation.valid(value)
                                  : Validation.invalid(format("%s contains invalid characters: ' %s '",
                                          fieldName,
                                          seq.distinct().sorted())));
    }

    private static Validation<String, Integer> validateYearOfBirth(Integer year) {
        return year == null || ((year <= (TODAY.getYear())) && (year >= TODAY.getYear() - 120))
               ? Validation.valid(year)
               : Validation.invalid("Year of birth can't be in future or older than 120 years");
    }

    private static Validation<String, String> validatePassword(String password) {
        if (Strings.isNullOrEmpty(password)) {
            return Validation.invalid("password can't be empty");
        }
        PasswordValidator validator = new PasswordValidator(Arrays.asList(
                new LengthRule(8, 30),
                new CharacterRule(EnglishCharacterData.UpperCase, 1),
                new CharacterRule(EnglishCharacterData.LowerCase, 1),
                new CharacterRule(EnglishCharacterData.Digit, 1),
                new CharacterRule(EnglishCharacterData.Special, 1),
                new IllegalSequenceRule(new SequenceData() {
                    @Override
                    public String getErrorCode() {
                        return "cannot have three or more consecutive numbers";
                    }

                    @Override
                    public CharacterSequence[] getSequences() {
                        return new CharacterSequence[]{
                                new CharacterSequence("`1234567890-=")
                        };
                    }
                }, 3, false)));
        RuleResult result = validator.validate(new PasswordData(password));
        if (result.isValid()) {
            return Validation.valid(password);
        }
        var error = result.getDetails()
                .stream()
                .map(RuleResultDetail::toString)
                .reduce((left, right) -> format("%s, %s", left, right))
                .map(message -> format("password has following issues: %s", message))
                .orElse("password did not meet criteria");
        return Validation.invalid(error);
    }
}

