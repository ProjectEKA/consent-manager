package in.org.projecteka.hdaf.link.discovery.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

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



