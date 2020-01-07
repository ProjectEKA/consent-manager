package in.org.projecteka.hdaf.link.discovery.model;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Identifier {
    private String system;
    private String type;
    private String use;
}
