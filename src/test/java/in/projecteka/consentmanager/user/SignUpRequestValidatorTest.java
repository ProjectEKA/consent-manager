package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.NullableConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static in.projecteka.consentmanager.user.TestBuilders.signUpRequest;
import static org.assertj.core.api.Assertions.assertThat;

class SignUpRequestValidatorTest {

    @Test
    void returnValidSignUpRequestWithAllFields() {
        var signUpRequest = signUpRequest()
                .password("aB1 #afasas")
                .name("onlyAlphabets")
                .yearOfBirth(LocalDate.now().getYear())
                .userName("usernameWithAlphabetsAnd1@ncg")
                .build();


        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");

        assertThat(requestValidation.isValid()).isTrue();
    }

    @Test
    void returnValidSignUpRequestWithOptionalDateOfBirth() {
        var signUpRequest = signUpRequest()
                .password("aB1#afasas")
                .name("onlyAlphabets")
                .yearOfBirth(LocalDate.now().getYear())
                .userName("usernameWithAlphabetsAnd1@ncg")
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");

        assertThat(requestValidation.isValid()).isTrue();
    }

    @Test
    void returnValidSignUpRequestWithFutureYearOfBirth() {
        var signUpRequest = signUpRequest()
                .password("aB1#afasas")
                .name("onlyAlphabets")
                .yearOfBirth(LocalDate.now().plusYears(1).getYear())
                .userName("usernameWithAlphabetsAnd1@ncg")
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");

        assertThat(requestValidation.isValid()).isFalse();
    }

    @ParameterizedTest(name = "Empty name")
    @CsvSource({
            ",",
            "empty",
            "null"
    })
    void returnInValidSignUpRequestWithEmpty(@ConvertWith(NullableConverter.class) String name) {
        var signUpRequest = signUpRequest()
                .password("aB1#afasas")
                .name(name)
                .yearOfBirth(LocalDate.now().getYear())
                .userName("usernameWithAlphabetsAnd1@ncg")
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");

        assertThat(requestValidation.isValid()).isFalse();
        assertThat(requestValidation.getError())
                .isSubsetOf("Name can't be empty");
    }

    @Test
    void returnInValidSignUpRequestWithEmptyGender() {
        var signUpRequest = signUpRequest()
                .password("aB1#afasas")
                .name("onlyAlphabets")
                .yearOfBirth(LocalDate.now().getYear())
                .userName("usernameWithAlphabetsAnd1@ncg")
                .gender(null)
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");

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
    void returnInValidSignUpRequestWithEmptyUser(@ConvertWith(NullableConverter.class) String name) {
        var signUpRequest = signUpRequest()
                .password("aB1#afasas")
                .name("onlyAlphabets")
                .yearOfBirth(LocalDate.now().getYear())
                .userName(name)
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");

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
    void returnInValidSignUpRequestWithRandomValues(@ConvertWith(NullableConverter.class) String name) {
        var signUpRequest = signUpRequest()
                .password("aB1#afasas")
                .name("onlyAlphabets")
                .yearOfBirth(LocalDate.now().getYear())
                .userName(name)
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");

        assertThat(requestValidation.isValid()).isFalse();
    }

    @Test
    void returnInValidSignUpRequestWithUserDoesNotEndWithProvider() {
        var signUpRequest = signUpRequest()
                .password("aB1#afasas")
                .name("onlyAlphabets")
                .yearOfBirth(LocalDate.now().getYear())
                .userName("userDoesNotEndWith")
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");

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
    void returnInValidSignUpRequestWithUserLesserThanMinimumLength(String username) {
        var signUpRequest = signUpRequest()
                .password("aB1#afasas")
                .name("onlyAlphabets")
                .yearOfBirth(LocalDate.now().getYear())
                .userName(username)
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");

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
    void returnInValidSignUpRequestWithWeak(String password) {
        var signUpRequest = signUpRequest()
                .password(password)
                .name("onlyAlp habets")
                .yearOfBirth(LocalDate.now().getYear())
                .userName("username@ncg")
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");

        assertThat(requestValidation.isValid()).isFalse();
    }

    @ParameterizedTest(name = "Empty password")
    @CsvSource({
            ",",
            "empty",
            "null"
    })
    void returnInValidSignUpRequestWithEmptyPassword(@ConvertWith(NullableConverter.class) String password) {
        var signUpRequest = signUpRequest()
                .password(password)
                .name("onlyAlphabets")
                .yearOfBirth(LocalDate.now().getYear())
                .userName("username@ncg")
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");

        assertThat(requestValidation.isValid()).isFalse();
        assertThat(requestValidation.getError())
                .isSubsetOf("password can't be empty");
    }

    @Test
    void returnInValidSignUpRequestWithYearOfBirthGreaterThan120() {
        var signUpRequest = signUpRequest()
                .password("aB1 #afasas")
                .name("onlyAlphabets")
                .yearOfBirth(LocalDate.now().getYear()-121)
                .userName("usernameWithAlphabetsAnd1@ncg")
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");

        assertThat(requestValidation.isValid()).isFalse();
        assertThat(requestValidation.getError())
                .isSubsetOf("Year of birth can't be in future or older than 120 years");
    }

    @Test
    void returnInValidSignUpRequestWithYearOfBirthInFuture() {
        var signUpRequest = signUpRequest()
                .password("aB1 #afasas")
                .name("onlyAlphabets")
                .yearOfBirth(LocalDate.now().getYear()+1)
                .userName("usernameWithAlphabetsAnd1@ncg")
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");

        assertThat(requestValidation.isValid()).isFalse();
        assertThat(requestValidation.getError())
                .isSubsetOf("Year of birth can't be in future or older than 120 years");
    }

    @Test
    void returnInValidSignUpRequestWithYearOfBirthIsNull() {
        var signUpRequest = signUpRequest()
                .password("aB1 #afasas")
                .name("onlyAlphabets")
                .yearOfBirth(null)
                .userName("usernameWithAlphabetsAnd1@ncg")
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");

        assertThat(requestValidation.isValid()).isTrue();
    }
}