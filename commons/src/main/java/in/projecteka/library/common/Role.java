package in.projecteka.library.common;

import java.util.Optional;

public enum Role {
    GATEWAY;

    public static Optional<Role> valueOfIgnoreCase(String mayBeRole) {
         return  mayBeRole == null
                 ? Optional.empty()
                 : mayBeRole.equalsIgnoreCase(GATEWAY.name()) ? Optional.of(GATEWAY) : Optional.empty();
    }
}
