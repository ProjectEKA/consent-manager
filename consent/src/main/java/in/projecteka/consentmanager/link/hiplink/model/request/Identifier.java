package in.projecteka.consentmanager.link.hiplink.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Identifier {
    IdentifierType type;
    String value;
}
