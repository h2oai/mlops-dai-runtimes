package ai.h2o.mojos.deploy.common.kdb;


/**
 * Simple java class to house credentials for KDB
 */
public class KdbCredentials {

    private final String username;
    private final String password;

    public KdbCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }
    public String getUsername() {
            return username;
    }
    public String getPassword() {
        return password;
    }
}
