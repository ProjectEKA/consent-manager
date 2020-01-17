package in.projecteka.consentmanager.consent.model.request;

import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

public class ConsentRequestValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return ConsentRequest.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {

    }
}
