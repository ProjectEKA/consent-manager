package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.NullableConverter;
import in.projecteka.consentmanager.user.model.DateOfBirth;
import in.projecteka.consentmanager.user.model.IdentifierType;
import in.projecteka.consentmanager.user.model.PatientName;
import in.projecteka.consentmanager.user.model.SignUpIdentifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static in.projecteka.consentmanager.user.TestBuilders.signUpRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class SignUpRequestValidatorTest {

    @Mock
    private PatientName patientName;

    @Mock
    private DateOfBirth dateOfBirth;

    @BeforeAll
    public void setUp(){
        when(patientName.getFName()).thenReturn("User");
        when(patientName.getMName()).thenReturn("Name");
        when(patientName.getLName()).thenReturn("withAlphabet");

        when(dateOfBirth.getDate()).thenReturn(LocalDate.now().getDayOfMonth());
        when(dateOfBirth.getMonth()).thenReturn(LocalDate.now().getMonthValue());
        when(dateOfBirth.getYear()).thenReturn(LocalDate.now().getYear());
    }

    @Test
    void returnValidSignUpRequestWithAllFields() {
        var signUpRequest = signUpRequest()
                .password("aB1 #afasas")
                .name(patientName)
                .dateOfBirth(dateOfBirth)
                .username("usernameWithAlphabetsAnd1@ncg")
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");

        assertThat(requestValidation.isValid()).isTrue();
    }

    @Test
    void returnValidSignUpRequestWithOptionalDateOfBirth() {
        when(dateOfBirth.getDate()).thenReturn(null);
        when(dateOfBirth.getMonth()).thenReturn(null);
        when(dateOfBirth.getYear()).thenReturn(null);

        var signUpRequest = signUpRequest()
                .password("aB1#afasas")
                .name(patientName)
                .dateOfBirth(dateOfBirth)
                .username("usernameWithAlphabetsAnd1@ncg")
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");

        assertThat(requestValidation.isValid()).isTrue();
    }

    @Test
    void returnInValidSignUpRequestWithFutureYearOfBirth() {
        when(dateOfBirth.getYear()).thenReturn(LocalDate.now().getYear() + 1);

        var signUpRequest = signUpRequest()
                .password("aB1#afasas")
                .name(patientName)
                .dateOfBirth(dateOfBirth)
                .username("usernameWithAlphabetsAnd1@ncg")
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");

        assertThat(requestValidation.isValid()).isFalse();
    }

    @Test
    void returnInValidSignUpRequestWithEmptyName() {
        when(patientName.getFName()).thenReturn(null);
        when(patientName.getMName()).thenReturn(null);
        when(patientName.getLName()).thenReturn(null);

        var signUpRequest = signUpRequest()
                .password("aB1#afasas")
                .name(patientName)
                .dateOfBirth(dateOfBirth)
                .username("usernameWithAlphabetsAnd1@ncg")
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
                .name(patientName)
                .dateOfBirth(dateOfBirth)
                .username("usernameWithAlphabetsAnd1@ncg")
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
                .name(patientName)
                .dateOfBirth(dateOfBirth)
                .username(name)
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
                .name(patientName)
                .dateOfBirth(dateOfBirth)
                .username(name)
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");

        assertThat(requestValidation.isValid()).isFalse();
    }

    @Test
    void returnInValidSignUpRequestWithUserDoesNotEndWithProvider() {
        var signUpRequest = signUpRequest()
                .password("aB1#afasas")
                .name(patientName)
                .dateOfBirth(dateOfBirth)
                .username("userDoesNotEndWith")
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
                .name(patientName)
                .dateOfBirth(dateOfBirth)
                .username(username)
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
                .name(patientName)
                .dateOfBirth(dateOfBirth)
                .username("username@ncg")
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
                .name(patientName)
                .dateOfBirth(dateOfBirth)
                .username("username@ncg")
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");

        assertThat(requestValidation.isValid()).isFalse();
        assertThat(requestValidation.getError())
                .isSubsetOf("password can't be empty");
    }

    @Test
    void returnInValidSignUpRequestWithYearOfBirthGreaterThan120() {
        when(dateOfBirth.getDate()).thenReturn(LocalDate.now().getDayOfMonth());
        when(dateOfBirth.getMonth()).thenReturn(LocalDate.now().getMonthValue());
        when(dateOfBirth.getYear()).thenReturn(LocalDate.now().getYear() - 121);

        var signUpRequest = signUpRequest()
                .password("aB1 #afasas")
                .name(patientName)
                .dateOfBirth(dateOfBirth)
                .username("usernameWithAlphabetsAnd1@ncg")
                .build();

        var requestValidation = SignUpRequestValidator.validate(signUpRequest, "@ncg");

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