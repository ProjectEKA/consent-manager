package in.projecteka.library.common.heartbeat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.boot.context.properties.ConstructorBinding;

@Getter
@AllArgsConstructor
@ConstructorBinding
@Builder
public class CacheMethodProperty {
    private final String methodName;
}