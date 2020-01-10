package in.org.projecteka.hdaf.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.org.projecteka.hdaf.user.model.User;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class UserRepository {

    public Mono<User> userWith(String userName) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            URL resource = ClassLoader.getSystemClassLoader().getResource("users.json");
            if (resource != null) {
                Path path = Paths.get(resource.getPath());
                byte[] jsonData = Files.readAllBytes(path);
                User[] users = mapper.readValue(jsonData, User[].class);

                return Arrays.stream(users)
                        .filter(user -> user.getIdentifier().equals(userName))
                        .findFirst()
                        .map(Mono::just)
                        .orElse(Mono.empty());
            }
            return Mono.empty();

        } catch (IOException e) {
            return Mono.error(new Exception("Something went wrong"));
        }
    }
}
