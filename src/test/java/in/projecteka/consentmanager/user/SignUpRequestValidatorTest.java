package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.NullableConverter;
import in.projecteka.consentmanager.user.model.IdentifierType;
import in.projecteka.consentmanager.user.model.SignUpIdentifier;
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

//    @Test
//    void returnValidSignUpRequestWithAllFields() {
//        var signUpRequest = signUpRequest()
//                .password("aB1 #afasas")
//                .name(patientName().first("Alan").build())
//                .dateOfBirth(dateOfBirth().date(1).month(11).year(1998).build())
//                .username("usernameWithAlphabetsAnd1@ncg")
//                .build();
//
//        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");
//
//        assertThat(requestValidation.isValid()).isTrue();
//    }
//
//    @Test
//    void returnValidSignUpRequestWithOptionalDateOfBirth() {
//        var signUpRequest = signUpRequest()
//                .password("aB1#afasas")
//                .name(patientName().first("Alan").build())
//                .dateOfBirth(dateOfBirth().year(null).build())
//                .username("usernameWithAlphabetsAnd1@ncg")
//                .build();
//
//        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");
//
//        assertThat(requestValidation.isValid()).isTrue();
//    }
//
//    @Test
//    void returnInValidSignUpRequestWithFutureYearOfBirth() {
//        var signUpRequest = signUpRequest()
//                .password("aB1#afasas")
//                .name(patientName().first("Alan").build())
//                .dateOfBirth(dateOfBirth().date(1).month(11).year(LocalDate.now().getYear() + 1).build())
//                .username("usernameWithAlphabetsAnd1@ncg")
//                .build();
//
//        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");
//
//        assertThat(requestValidation.isValid()).isFalse();
//    }
//
//    @Test
//    void returnInValidSignUpRequestWithEmptyName() {
//        var signUpRequest = signUpRequest()
//                .password("aB1#afasas")
//                .name(patientName().first("").build())
//                .dateOfBirth(dateOfBirth().date(1).month(11).year(1987).build())
//                .username("usernameWithAlphabetsAnd1@ncg")
//                .build();
//
//        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");
//
//        assertThat(requestValidation.isValid()).isFalse();
//        assertThat(requestValidation.getError())
//                .isSubsetOf("Name can't be empty");
//    }
//
//    @Test
//    void returnInValidSignUpRequestWithEmptyGender() {
//        var signUpRequest = signUpRequest()
//                .password("aB1#afasas")
//                .name(patientName().first("Alan").build())
//                .dateOfBirth(dateOfBirth().date(1).month(11).year(1997).build())
//                .username("usernameWithAlphabetsAnd1@ncg")
//                .gender(null)
//                .build();
//
//        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");
//
//        assertThat(requestValidation.isValid()).isFalse();
//        assertThat(requestValidation.getError())
//                .isSubsetOf("gender can't be empty");
//    }
//
//    @ParameterizedTest(name = "Empty first name")
//    @CsvSource({
//            ",",
//            "empty",
//            "null"
//    })
//    void returnInValidSignUpRequestWithEmptyUser(@ConvertWith(NullableConverter.class) String name) {
//        var signUpRequest = signUpRequest()
//                .password("aB1#afasas")
//                .name(patientName().first("Alan").build())
//                .dateOfBirth(dateOfBirth().date(1).month(11).year(1997).build())
//                .username(name)
//                .build();
//
//        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");
//
//        assertThat(requestValidation.isValid()).isFalse();
//        assertThat(requestValidation.getError())
//                .isSubsetOf("username can't be empty");
//    }
//
//    @ParameterizedTest(name = "Random user name")
//    @CsvSource({
//            "username with spaces@ncg",
//            "username#2e2@ncg",
//            "username<>afasfa<script>@ncg"
//    })
//    void returnInValidSignUpRequestWithRandomValues(@ConvertWith(NullableConverter.class) String name) {
//        var signUpRequest = signUpRequest()
//                .password("aB1#afasas")
//                .name(patientName().first("Alan").build())
//                .dateOfBirth(dateOfBirth().date(1).month(11).year(1996).build())
//                .username(name)
//                .build();
//
//        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");
//
//        assertThat(requestValidation.isValid()).isFalse();
//    }
//
//    @Test
//    void returnInValidSignUpRequestWithUserDoesNotEndWithProvider() {
//        var signUpRequest = signUpRequest()
//                .password("aB1#afasas")
//                .name(patientName().first("abc").build())
//                .dateOfBirth(dateOfBirth().year(1999).build())
//                .username("userDoesNotEndWith")
//                .build();
//
//        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");
//
//        assertThat(requestValidation.isValid()).isFalse();
//        assertThat(requestValidation.getError())
//                .isSubsetOf("username does not end with @ncg");
//    }
//
//    @ParameterizedTest(name = "Username with length issues")
//    @CsvSource({
//            "u@ncg",
//            "reallyreallyreallyreallyreallyreallyreallyreally" +
//                    "reallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreally" +
//                    "reallyreallybiggerusernameWhichIsBiggerThanOneFiftyCharacters@ncg"
//    })
//    void returnInValidSignUpRequestWithUserLesserThanMinimumLength(String username) {
//        var signUpRequest = signUpRequest()
//                .password("aB1#afasas")
//                .name(patientName().first("Alan").build())
//                .dateOfBirth(dateOfBirth().date(1).month(11).year(1997).build())
//                .username(username)
//                .build();
//
//        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");
//
//        assertThat(requestValidation.isValid()).isFalse();
//        assertThat(requestValidation.getError())
//                .isSubsetOf("username should be between 3 and 150 characters");
//    }
//
//
//    @ParameterizedTest(name = "weak password")
//    @CsvSource({
//            "weak_without_caps_or_numeric",
//            "weak_Without_numeric",
//            "weakWithout1SpecialCharacters",
//            "345aA#afaf"
//    })
//    void returnInValidSignUpRequestWithWeak(String password) {
//        var signUpRequest = signUpRequest()
//                .password(password)
//                .name(patientName().first("Alan").build())
//                .dateOfBirth(dateOfBirth().date(1).month(11).year(1997).build())
//                .username("username@ncg")
//                .build();
//
//        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");
//
//        assertThat(requestValidation.isValid()).isFalse();
//    }
//
//    @ParameterizedTest(name = "Empty password")
//    @CsvSource({
//            ",",
//            "empty",
//            "null"
//    })
//    void returnInValidSignUpRequestWithEmptyPassword(@ConvertWith(NullableConverter.class) String password) {
//        var signUpRequest = signUpRequest()
//                .password(password)
//                .name(patientName().first("Alan").build())
//                .dateOfBirth(dateOfBirth().year(1997).build())
//                .username("username@ncg")
//                .build();
//
//        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");
//
//        assertThat(requestValidation.isValid()).isFalse();
//        assertThat(requestValidation.getError())
//                .isSubsetOf("password can't be empty");
//    }
//
//    @Test
//    void returnInValidSignUpRequestWithYearOfBirthGreaterThan120() {
//        var signUpRequest = signUpRequest()
//                .password("aB1 #afasas")
//                .name(patientName().first("Alan").build())
//                .dateOfBirth(dateOfBirth().date(1).month(11).year(LocalDate.now().getYear() - 121).build())
//                .username("usernameWithAlphabetsAnd1@ncg")
//                .build();
//
//        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");
//
//        assertThat(requestValidation.isValid()).isFalse();
//        assertThat(requestValidation.getError())
//                .isSubsetOf("Year of birth can't be in future or older than 120 years");
//    }
//
//    @Test
//    public void shouldReturnInvalidForMultipleABIds() {
//        SignUpIdentifier identifier1 = new SignUpIdentifier(IdentifierType.ABPMJAYID.name(), "ab1");
//        SignUpIdentifier identifier2 = new SignUpIdentifier(IdentifierType.ABPMJAYID.name(), "ab2");
//        List<SignUpIdentifier> multipleAbIds = Arrays.asList(identifier1, identifier2);
//        var validation = SignUpRequestValidator.validateUnVerifiedIdentifiers(multipleAbIds);
//        assertThat(validation.isValid()).isFalse();
//    }
//
//    @Test
//    public void shouldReturnInvalidForIncorrectABId() {
//        SignUpIdentifier identifier1 = new SignUpIdentifier(IdentifierType.ABPMJAYID.name(), "pabcd1234");
//        List<SignUpIdentifier> multipleAbIds = Collections.singletonList(identifier1);
//        var validation = SignUpRequestValidator.validateUnVerifiedIdentifiers(multipleAbIds);
//        assertThat(validation.isValid()).isFalse();
//    }
//
//    @Test
//    public void shouldReturnValidForCorrectABId() {
//        SignUpIdentifier identifier1 = new SignUpIdentifier(IdentifierType.ABPMJAYID.name(), "P1234ABCD");
//        List<SignUpIdentifier> multipleAbIds = Collections.singletonList(identifier1);
//        var validation = SignUpRequestValidator.validateUnVerifiedIdentifiers(multipleAbIds);
//        assertThat(validation.isValid()).isTrue();
//    }
//
//    @Test
//    public void shouldReturnInvalidForInvalidIdentifier() {
//        SignUpIdentifier identifier1 = new SignUpIdentifier("foobar", "P1234ABCD");
//        List<SignUpIdentifier> multipleAbIds = Collections.singletonList(identifier1);
//        var validation = SignUpRequestValidator.validateUnVerifiedIdentifiers(multipleAbIds);
//        assertThat(validation.isValid()).isFalse();
//    }
}