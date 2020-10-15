package in.projecteka.consentmanager.userauth.model;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum AuthPurpose {
    LINK, KYC_AND_LINK, KYC, @JsonEnumDefaultValue INVALID_PURPOSE
}
