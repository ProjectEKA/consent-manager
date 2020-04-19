package in.projecteka.consentmanager.consent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class QueryRepresentation {
    private List<Query> queries;
    private List<HIPConsentArtefactRepresentation> hipConsentArtefactRepresentations;

    public QueryRepresentation add(QueryRepresentation other) {
        List<Query> queries =
                Stream.of(getQueries(), other.getQueries()).flatMap(Collection::stream).collect(Collectors.toList());
        List<HIPConsentArtefactRepresentation> hipConsentArtefactRepresentations =
                Stream.of(getHipConsentArtefactRepresentations(), other.getHipConsentArtefactRepresentations())
                        .flatMap(Collection::stream).collect(Collectors.toList());
        return new QueryRepresentation(queries, hipConsentArtefactRepresentations);
    }
}
