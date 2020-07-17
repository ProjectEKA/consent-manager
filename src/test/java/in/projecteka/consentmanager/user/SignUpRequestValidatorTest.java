package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.NullableConverter;
import in.projecteka.consentmanager.user.model.IdentifierType;
import in.projecteka.consentmanager.user.model.SignUpIdentifier;
import in.projecteka.consentmanager.user.model.UpdateLoginDetailsRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static in.projecteka.consentmanager.user.TestBuilders.patientName;
import static in.projecteka.consentmanager.user.TestBuilders.signUpRequest;
import static in.projecteka.consentmanager.user.TestBuilders.dateOfBirth;
import static org.assertj.core.api.Assertions.assertThat;

class SignUpRequestValidatorTest {

    @Test
    void returnValidSignUpRequestWithAllFields() {
        var signUpRequest = signUpRequest()
                .name(patientName().first("Alan").build())
                .dateOfBirth(dateOfBirth().date(1).month(11).year(1998).build())
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest);

        assertThat(requestValidation.isValid()).isTrue();
    }

    @Test
    void returnValidSignUpRequestWithOptionalDateOfBirth() {
        var signUpRequest = signUpRequest()
                .name(patientName().first("Alan").build())
                .dateOfBirth(dateOfBirth().year(null).build())
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest);

        assertThat(requestValidation.isValid()).isTrue();
    }

    @Test
    void returnInValidSignUpRequestWithFutureYearOfBirth() {
        var signUpRequest = signUpRequest()
                .name(patientName().first("Alan").build())
                .dateOfBirth(dateOfBirth().date(1).month(11).year(LocalDate.now().getYear() + 1).build())
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest);

        assertThat(requestValidation.isValid()).isFalse();
    }

    @Test
    void returnInValidSignUpRequestWithEmptyName() {
        var signUpRequest = signUpRequest()
                .name(patientName().first("").build())
                .dateOfBirth(dateOfBirth().date(1).month(11).year(1987).build())
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest);

        assertThat(requestValidation.isValid()).isFalse();
        assertThat(requestValidation.getError())
                .isSubsetOf("Name can't be empty");
    }

    @Test
    void returnInValidSignUpRequestWithEmptyGender() {
        var signUpRequest = signUpRequest()
                .name(patientName().first("Alan").build())
                .dateOfBirth(dateOfBirth().date(1).month(11).year(1997).build())
                .gender(null)
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest);

        assertThat(requestValidation.isValid()).isFalse();
        assertThat(requestValidation.getError())
                .isSubsetOf("gender can't be empty");
    }

    @ParameterizedTest(name = "Empty first name")
    @CsvSource({
            ",",
            "empty",
            "null"
    })
    void returnInValidLoginRequestWithEmptyUser(@ConvertWith(NullableConverter.class) String name) {
        var updateLoginRequest = UpdateLoginDetailsRequest.builder()
                .password("Test@1243").healthId("12345-12345-12345").cmId(name).build();

        var requestValidation = SignUpRequestValidator.validateLoginDetails(updateLoginRequest,"@ncg");

        assertThat(requestValidation.isValid()).isFalse();
        assertThat(requestValidation.getError())
                .isSubsetOf("username can't be empty");
    }

    @ParameterizedTest(name = "Random user name")
    @CsvSource({
            "username with spaces@ncg",
            "username#2e2@ncg",
            "username<>afasfa<script>@ncg"
    })
    void returnInValidLoginRequestWithRandomValues(@ConvertWith(NullableConverter.class) String name) {
        var updateLoginRequest = UpdateLoginDetailsRequest.builder()
                .password("Test@1243").healthId("12345-12345-12345").cmId(name).build();

        var requestValidation = SignUpRequestValidator.validateLoginDetails(updateLoginRequest,"@ncg");

        assertThat(requestValidation.isValid()).isFalse();
    }

    @Test
    void returnInValidSignUpRequestWithUserDoesNotEndWithProvider() {
        var updateLoginRequest = UpdateLoginDetailsRequest.builder()
                .password("Test@1243").healthId("12345-12345-12345").cmId("hinapatel45").build();


        var requestValidation = SignUpRequestValidator.validateLoginDetails(updateLoginRequest,"@ncg");

        assertThat(requestValidation.isValid()).isFalse();
        assertThat(requestValidation.getError())
                .isSubsetOf("username does not end with @ncg");
    }

    @ParameterizedTest(name = "Username with length issues")
    @CsvSource({
            "u@ncg",
            "reallyreallyreallyreallyreallyreallyreallyreally" +
                    "reallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreally" +
                    "reallyreallybiggerusernameWhichIsBiggerThanOneFiftyCharacters@ncg"
    })
    void returnInValidLoginRequestWithUserLesserThanMinimumLength(String username) {
        var updateLoginRequest = UpdateLoginDetailsRequest.builder()
                .password("Test@1243").healthId("12345-12345-12345").cmId(username).build();

        var requestValidation = SignUpRequestValidator.validateLoginDetails(updateLoginRequest,"@ncg");

        assertThat(requestValidation.isValid()).isFalse();
        assertThat(requestValidation.getError())
                .isSubsetOf("username should be between 3 and 150 characters");
    }


    @ParameterizedTest(name = "weak password")
    @CsvSource({
            "weak_without_caps_or_numeric",
            "weak_Without_numeric",
            "weakWithout1SpecialCharacters",
            "345aA#afaf"
    })
    void returnInValidLoginRequestWithWeakPassword(String password) {
        var updateLoginRequest = UpdateLoginDetailsRequest.builder()
                .password(password).healthId("12345-12345-12345").cmId("hinapatel3@ncg").build();

        var requestValidation = SignUpRequestValidator.validateLoginDetails(updateLoginRequest,"@ncg");

        assertThat(requestValidation.isValid()).isFalse();
    }

    @ParameterizedTest(name = "Empty password")
    @CsvSource({
            ",",
            "empty",
            "null"
    })
    void returnInValidLoginRequestWithEmptyPassword(@ConvertWith(NullableConverter.class) String password) {
        var updateLoginRequest = UpdateLoginDetailsRequest.builder()
                .password(password).healthId("12345-12345-12345").cmId("hinapatel3@ncg").build();

        var requestValidation = SignUpRequestValidator.validateLoginDetails(updateLoginRequest,"@ncg");

        assertThat(requestValidation.isValid()).isFalse();
        assertThat(requestValidation.getError())
                .isSubsetOf("password can't be empty");
    }

    @Test
    void returnInValidSignUpRequestWithYearOfBirthGreaterThan120() {
        var signUpRequest = signUpRequest()
                .name(patientName().first("Alan").build())
                .dateOfBirth(dateOfBirth().date(1).month(11).year(LocalDate.now().getYear() - 121).build())
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest);

        assertThat(requestValidation.isValid()).isFalse();
        assertThat(requestValidation.getError())
                .isSubsetOf("Year of birth can't be in future or older than 120 years");
    }

    @Test
    public void shouldReturnInvalidForMultipleABIds() {
        SignUpIdentifier identifier1 = new SignUpIdentifier(IdentifierType.ABPMJAYID.name(), "ab1");
        SignUpIdentifier identifier2 = new SignUpIdentifier(IdentifierType.ABPMJAYID.name(), "ab2");
        List<SignUpIdentifier> multipleAbIds = Arrays.asList(identifier1, identifier2);
        var validation = SignUpRequestValidator.validateUnVerifiedIdentifiers(multipleAbIds);
        assertThat(validation.isValid()).isFalse();
    }

    @Test
    public void shouldReturnInvalidForIncorrectABId() {
        SignUpIdentifier identifier1 = new SignUpIdentifier(IdentifierType.ABPMJAYID.name(), "pabcd1234");
        List<SignUpIdentifier> multipleAbIds = Collections.singletonList(identifier1);
        var validation = SignUpRequestValidator.validateUnVerifiedIdentifiers(multipleAbIds);
        assertThat(validation.isValid()).isFalse();
    }

    @Test
    public void shouldReturnValidForCorrectABId() {
        SignUpIdentifier identifier1 = new SignUpIdentifier(IdentifierType.ABPMJAYID.name(), "P1234ABCD");
        List<SignUpIdentifier> multipleAbIds = Collections.singletonList(identifier1);
        var validation = SignUpRequestValidator.validateUnVerifiedIdentifiers(multipleAbIds);
        assertThat(validation.isValid()).isTrue();
    }

    @Test
    public void shouldReturnInvalidForInvalidIdentifier() {
        SignUpIdentifier identifier1 = new SignUpIdentifier("foobar", "P1234ABCD");
        List<SignUpIdentifier> multipleAbIds = Collections.singletonList(identifier1);
        var validation = SignUpRequestValidator.validateUnVerifiedIdentifiers(multipleAbIds);
        assertThat(validation.isValid()).isFalse();
    }
}