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
                .yearOfBirth(1990)
                .userName("usernameWithAlphabetsAnd1@ncg")
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest);

        assertThat(requestValidation.isValid()).isTrue();
    }

    @ParameterizedTest(name = "Optional last name")
    @CsvSource({
            ",",
            "empty",
            "null"
    })
    void returnValidSignUpRequestWithOptional(@ConvertWith(NullableConverter.class) String lastName) {
        var signUpRequest = signUpRequest()
                .password("aB1#afasas")
                .name("onlyAlphabets")
                .yearOfBirth(1990)
                .userName("usernameWithAlphabetsAnd1@ncg")
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest);

        assertThat(requestValidation.isValid()).isTrue();
    }

    @Test
    void returnValidSignUpRequestWithOptionalDateOfBirth() {
        var signUpRequest = signUpRequest()
                .password("aB1#afasas")
                .name("onlyAlphabets")
                .yearOfBirth(1990)
                .userName("usernameWithAlphabetsAnd1@ncg")
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest);

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

        var requestValidation = SignUpRequestValidator.validate(signUpRequest);

        assertThat(requestValidation.isValid()).isFalse();
    }

    @ParameterizedTest(name = "Empty first name")
    @CsvSource({
            ",",
            "empty",
            "null"
    })
    void returnInValidSignUpRequestWithEmpty(@ConvertWith(NullableConverter.class) String firstName) {
        var signUpRequest = signUpRequest()
                .password("aB1#afasas")
                .name(firstName)
                .yearOfBirth(1990)
                .userName("usernameWithAlphabetsAnd1@ncg")
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest);

        assertThat(requestValidation.isValid()).isFalse();
        assertThat(requestValidation.getError())
                .isSubsetOf("Name can't be empty");
    }

    @Test
    void returnInValidSignUpRequestWithEmptyGender() {
        var signUpRequest = signUpRequest()
                .password("aB1#afasas")
                .name("onlyAlphabets")
                .yearOfBirth(1990)
                .userName("usernameWithAlphabetsAnd1@ncg")
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
    void returnInValidSignUpRequestWithEmptyUser(@ConvertWith(NullableConverter.class) String name) {
        var signUpRequest = signUpRequest()
                .password("aB1#afasas")
                .name("onlyAlphabets")
                .yearOfBirth(1990)
                .userName(name)
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest);

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
                .yearOfBirth(1990)
                .userName(name)
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest);

        assertThat(requestValidation.isValid()).isFalse();
    }

    @Test
    void returnInValidSignUpRequestWithUserDoesNotEndWithProvider() {
        var signUpRequest = signUpRequest()
                .password("aB1#afasas")
                .name("onlyAlphabets")
                .yearOfBirth(1990)
                .userName("userDoesNotEndWith")
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest);

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
                .yearOfBirth(1990)
                .userName(username)
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest);

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
                .name("onlyAlphabets")
                .yearOfBirth(1990)
                .userName("username@ncg")
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest);

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
                .yearOfBirth(1990)
                .userName("username@ncg")
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest);

        assertThat(requestValidation.isValid()).isFalse();
        assertThat(requestValidation.getError())
                .isSubsetOf("password can't be empty");
    }
}