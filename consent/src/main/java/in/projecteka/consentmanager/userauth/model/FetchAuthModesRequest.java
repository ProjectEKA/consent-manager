package in.projecteka.consentmanager.userauth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@Data
public class FetchAuthModesRequest {
    private String requestId;
    private LocalDateTime timestamp;

    @NonNull
    private Query query;

    @Builder
    @AllArgsConstructor
    @Data
    public static class Query {
        @NonNull
        String id;
        @NonNull
        AuthPurpose purpose;
        @NonNull
        Requester requester;
    }
}
