package in.projecteka.library.common.heartbeat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class CacheMethodProperty {
    private final String methodName;
}