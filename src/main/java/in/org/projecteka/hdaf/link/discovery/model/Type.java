package in.org.projecteka.hdaf.link.discovery.model;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@Getter
@NoArgsConstructor
@Setter
@Builder
public class Type {
    private List<Coding> coding;
}
