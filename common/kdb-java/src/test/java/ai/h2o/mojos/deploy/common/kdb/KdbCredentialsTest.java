package ai.h2o.mojos.deploy.common.kdb;

import org.junit.jupiter.api.Test;
import static com.google.common.truth.Truth.assertThat;

class KdbCredentialsTest {
    private final KdbCredentials kdbAuth = new KdbCredentials("testuser", "testpassword");

    @Test
    void validateManualCredentials() {
        assertThat(kdbAuth.getUsername().equals("testuser"));
        assertThat(kdbAuth.getPassword().equals("testpassword"));
    }
}