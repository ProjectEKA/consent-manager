package in.projecteka.library.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DbOptions {
    private final String host;
    private final int port;
}