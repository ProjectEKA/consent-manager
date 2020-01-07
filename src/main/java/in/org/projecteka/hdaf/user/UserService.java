package in.org.projecteka.hdaf.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.org.projecteka.hdaf.user.model.User;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class UserService {
    public Mono<User> getUser(String userId) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            Path path = Paths.get("src/main/java/in/org/projecteka/hdaf/user/users.json");
            byte[] jsonData = Files.readAllBytes(path);
            User[] users = mapper.readValue(jsonData, User[].class);

            return Arrays.stream(users)
                    .filter(user -> user.getIdentifier().equals(userId))
                    .findFirst()
                    .map(Mono::just)
                    .orElse(Mono.empty());

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
