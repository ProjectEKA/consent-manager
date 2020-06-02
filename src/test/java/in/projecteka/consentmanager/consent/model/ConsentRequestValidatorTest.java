package in.projecteka.consentmanager.consent.model;

import in.projecteka.consentmanager.consent.model.request.RequestedDetail;
import in.projecteka.consentmanager.consent.model.request.ConsentRequest;
import in.projecteka.consentmanager.dataflow.model.Consent;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsentRequestValidatorTest {

    @Test
    public void shouldResultInErrorIfToDateEarlierThanFromDate() {
        Date fromDate = new Date();
        Calendar toDate = Calendar.getInstance();
        toDate.add(Calendar.DATE, -1);
        AccessPeriod dateRange = AccessPeriod.builder().fromDate(fromDate).toDate(toDate.getTime()).build();
        ConsentPermission permission = ConsentPermission.builder().dateRange(dateRange).build();
        RequestedDetail requestedDetail = RequestedDetail.builder().permission(permission).build();
        ConsentRequest request = ConsentRequest.builder()
                .consent(requestedDetail)
                .build();
        ConsentRequestValidator consentRequestValidator = new ConsentRequestValidator();
        Errors errors = new BeanPropertyBindingResult(request, "request");
        consentRequestValidator.validate(request, errors);
        assertEquals(1, errors.getErrorCount());
    }


}