package in.projecteka.consentmanager.consent.model;

import in.projecteka.consentmanager.consent.model.request.ConsentRequest;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class ConsentRequestValidator implements Validator {
    @Override
    public boolean supports(Class<?> clazz) {
        return ConsentRequest.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        if (!errors.hasErrors()) {
            ConsentRequest request = (ConsentRequest) target;
            //compare permission dates - toDate must be greater than equal to fromDate
            if (request.getConsent().getPermission().comparePermissionDates() > 0) {
                errors.reject("invalid.permission.fromDate", "Permission date is invalid.");
            }
        }
    }
}
