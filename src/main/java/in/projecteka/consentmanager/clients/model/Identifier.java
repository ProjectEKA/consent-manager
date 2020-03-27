package in.projecteka.consentmanager.clients.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Identifier {
    public enum IdentifierType {
        OFFICIAL
    }

    private String system;
    private String value;
    private String use;

    public boolean isOfficial() {
        return use != null && use.equalsIgnoreCase(IdentifierType.OFFICIAL.toString());
    }
}
