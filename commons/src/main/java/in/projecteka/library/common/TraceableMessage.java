package in.projecteka.library.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Builder
@Data
public class TraceableMessage {
    String correlationId;
    Object message;
}