package in.projecteka.consentmanager.common;

public enum Role {
    GATEWAY;

    public static Role valueOfIgnoreCase(String mayBeRole) {
         return  mayBeRole.equalsIgnoreCase(GATEWAY.name()) ? GATEWAY : null;
    }
}
