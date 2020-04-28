package in.projecteka.consentmanager.clients.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@NoArgsConstructor
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Type {
    private List<Coding> coding;
}
