package in.projecteka.consentmanager.userauth.model;

import in.projecteka.library.clients.model.GatewayResponse;
import in.projecteka.library.clients.model.RespError;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@AllArgsConstructor
@Data
public class FetchAuthModesResponse {
    private String requestId;
    private LocalDateTime timestamp;
    private AuthResponse auth;
    private RespError error;
    @NotNull
    private GatewayResponse resp;

    @Builder
    @AllArgsConstructor
    @Data
    public static class AuthResponse{
        private AuthPurpose purpose;
        private List<AuthMode> modes;
    }
}
