package in.projecteka.consentmanager.consent.model;

import io.vertx.sqlclient.Tuple;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Query {
    private String queryString;
    private Tuple params;
}
