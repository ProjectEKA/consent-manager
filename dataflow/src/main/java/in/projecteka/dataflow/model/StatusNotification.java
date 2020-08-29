package in.projecteka.dataflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Builder
public class StatusNotification {
    private SessionStatus sessionStatus;
    private String hipId;
    private List<StatusResponse> statusResponses;
}
