package in.projecteka.consentmanager.user;

import com.google.common.base.Strings;
import in.projecteka.consentmanager.user.model.Gender;
import in.projecteka.consentmanager.user.model.SignUpRequest;
import io.vavr.collection.CharSeq;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import org.passay.DigitCharacterRule;
import org.passay.LengthRule;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.QwertySequenceRule;
import org.passay.RuleResult;
import org.passay.RuleResultDetail;
import org.passay.SpecialCharacterRule;
import org.passay.UppercaseCharacterRule;

import java.time.LocalDate;
import java.util.Arrays;

import static java.lang.String.format;

public class SignUpRequestValidator {

    private SignUpRequestValidator() {

    }

    private static final LocalDate TOMORROW = LocalDate.now().plusDays(1);
    private static final String VALID_NAME_CHARS = "[a-zA-Z]";
    private static final String PROVIDER = "@ncg";

    public static Validation<Seq<String>, SignUpRequest> validate(SignUpRequest signUpRequest) {
        return Validation.combine(
                validateFirstName(signUpRequest.getFirstName()),
                validateLastName(signUpRequest.getLastName()),
                validate(signUpRequest.getGender()),
                validateUserName(signUpRequest.getUserName()),
                validatePassword(signUpRequest.getPassword()),
                validateAge(signUpRequest.getDateOfBirth()))
                .ap((firstName, lastName, gender, username, password, dateOfBirth) -> SignUpRequest.builder()
                        .firstName(firstName)
                        .lastName(lastName)
                        .gender(gender)
                        .userName(username)
                        .password(password)
                        .dateOfBirth(dateOfBirth)
                        .build());
    }

    private static Validation<String, Gender> validate(Gender gender) {
        if (gender != null) {
            return Validation.valid(gender);
        }
        return Validation.invalid("gender can't be empty");
    }

    private static Validation<String, String> validateLastName(String lastName) {
        if (Strings.isNullOrEmpty(lastName)) {
            return Validation.valid(lastName);
        }
        return allowed(VALID_NAME_CHARS, "last name", lastName);
    }

    private static Validation<String, String> validateFirstName(String firstName) {
        if (Strings.isNullOrEmpty(firstName)) {
            return Validation.invalid("first name can't be empty");
        }
        return allowed(VALID_NAME_CHARS, "first name", firstName);
    }

    private static Validation<String, String> validateUserName(String username) {
        final String VALID_USERNAME_CHARS = "[a-zA-Z@0-9.\\-]";
        if (Strings.isNullOrEmpty(username)) {
            return Validation.invalid("username can't be empty");
        }
        return allowed(VALID_USERNAME_CHARS, "username", username)
                .combine(endsWithProvider(username))
                .combine(lengthLimitFor(username.replace(PROVIDER, "")))
                .ap((validCharacters, validEndsWith, validLength) -> username)
                .mapError(errors -> errors.reduce((left, right) -> format("%s, %s", left, right)));
    }

    private static Validation<String, String> endsWithProvider(String username) {
        if (!username.endsWith(PROVIDER)) {
            return Validation.invalid("username does not end with @ncg");
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

    private static Validation<String, LocalDate> validateAge(LocalDate dateOfBirth) {
        return dateOfBirth == null ||
                       dateOfBirth.isBefore(TOMORROW)
               ? Validation.valid(dateOfBirth)
               : Validation.invalid("Date of birth can't be in future");
    }

    private static Validation<String, String> validatePassword(String password) {
        if (Strings.isNullOrEmpty(password)) {
            return Validation.invalid("password can't be empty");
        }
        PasswordValidator validator = new PasswordValidator(Arrays.asList(
                new LengthRule(8, 30),
                new UppercaseCharacterRule(1),
                new DigitCharacterRule(1),
                new SpecialCharacterRule(1),
                new QwertySequenceRule(3, false)));
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
