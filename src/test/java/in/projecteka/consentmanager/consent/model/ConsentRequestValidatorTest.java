package in.projecteka.consentmanager.consent.model;

import in.projecteka.consentmanager.consent.model.request.ConsentRequest;
import in.projecteka.consentmanager.consent.model.request.RequestedDetail;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsentRequestValidatorTest {

    @Test
    public void shouldResultInErrorIfToDateEarlierThanFromDate() {
        LocalDateTime fromDate = LocalDateTime.now();
        Calendar toDate = Calendar.getInstance();
        toDate.add(Calendar.DATE, -1);
        LocalDateTime ldt = LocalDateTime.ofInstant(toDate.getTime().toInstant(), ZoneId.systemDefault());
        AccessPeriod dateRange = AccessPeriod.builder().fromDate(fromDate).toDate(ldt).build();
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