package in.projecteka.consentmanager.common;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminRequest<T> {
    String consentManagerId;
    T data;
}
