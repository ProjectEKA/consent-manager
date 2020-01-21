package in.projecteka.consentmanager.clients.model;

import in.projecteka.consentmanager.link.discovery.model.Coding;
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
public class Type {
    private List<Coding> coding;
}
