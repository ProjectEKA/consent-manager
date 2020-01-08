package in.org.projecteka.hdaf.link.discovery.model;

import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Identifier {
    public enum IdentifierType {
        OFFICIAL;
        @Override
        public String toString() {
            return super.toString();
        }
    }
    private String system;
    private String type;
    private String use;

    public boolean isOfficial() {
        return use != null && use.equalsIgnoreCase(IdentifierType.OFFICIAL.toString());
    }
}
