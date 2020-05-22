package in.projecteka.consentmanager.user.model;

import io.vertx.core.json.JsonArray;
import lombok.Builder;
import lombok.Getter;

@Builder(toBuilder = true)
@Getter
public class RecoverCmIdRow {
    String cmId;
    Integer yearOfBirth;
    JsonArray unverifiedIdentifiers;
}
