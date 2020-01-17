package in.projecteka.consentmanager.consent.model.request;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class ConsentRequestValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return ConsentRequest.class.equals(clazz);
    }

    /**
     * This is a custom validator. Seems like WebFlux does not work with JSR303.
     * Needs more investigation. Otherwise, we will have to do programmatic validation by
     * invoking the validator
     * @param target
     * @param errors
     */
    @Override
    public void validate(Object target, Errors errors) {
    }
}
