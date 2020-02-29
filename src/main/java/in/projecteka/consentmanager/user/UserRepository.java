package in.projecteka.consentmanager.user;

import in.projecteka.consentmanager.clients.properties.UserServiceProperties;
import in.projecteka.consentmanager.user.model.User;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;
import org.apache.log4j.Logger;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static in.projecteka.consentmanager.clients.ClientError.dbOperationFailed;

public class UserRepository {
    private final static String INSERT_PATIENT = "Insert into patient(id, " +
            "first_name, last_name, gender, date_of_birth, phone_number)" +
            " values($1, $2, $3, $4, $5, $6);";

    private final static Logger logger = Logger.getLogger(UserRepository.class);

    private WebClient builder;
    private PgPool dbClient;

    public UserRepository(WebClient.Builder builder,
                          UserServiceProperties properties,
                          PgPool dbClient) {
        this.builder = builder.baseUrl(properties.getUrl()).build();
        this.dbClient = dbClient;
    }

    public Mono<User> userWith(String userName) {
        return builder.get().uri("/users.json")
                .retrieve()
                .bodyToFlux(User.class)
                .filter(user -> user.getIdentifier().equals(userName))
                .single()
                .switchIfEmpty(Mono.error(new Exception("Something went wrong")));
    }

    public Mono<Void> save(User user) {
        return Mono.create(monoSink -> dbClient.preparedQuery(INSERT_PATIENT,
                Tuple.of(user.getIdentifier(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getGender().toString(),
                        user.getDateOfBirth(),
                        user.getPhone()),
                handler -> {
                    if (handler.failed()) {
                        logger.error(handler.cause());
                        monoSink.error(dbOperationFailed());
                        return;
                    }
                    monoSink.success();
                }));
    }
}
