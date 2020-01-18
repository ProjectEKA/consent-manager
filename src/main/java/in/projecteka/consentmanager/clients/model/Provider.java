package in.projecteka.consentmanager.clients.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.projecteka.consentmanager.link.discovery.model.Address;
import in.projecteka.consentmanager.link.discovery.model.Identifier;
import in.projecteka.consentmanager.link.discovery.model.Telecom;
import in.projecteka.consentmanager.link.discovery.model.Type;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Setter
public class Provider {
    private String resourceType;
    @JsonAlias("identifier")
    private List<Identifier> identifiers;
    @JsonAlias("type")
    private List<Type> types;
    @JsonAlias("telecom")
    private List<Telecom> telecoms;
    @JsonAlias("address")
    private List<Address> addresses;
    private String name;
}



