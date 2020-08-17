package in.projecteka.library.clients.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Notification<T> {
    private String id;
    private Communication communication;
    private T content;
    private Action action;
}