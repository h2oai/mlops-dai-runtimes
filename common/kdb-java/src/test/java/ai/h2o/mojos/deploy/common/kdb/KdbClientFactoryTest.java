package ai.h2o.mojos.deploy.common.kdb;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import static com.google.common.truth.Truth.assertThat;

class KdbClientFactoryTest {

    @Test
    void validateJsonCredentialsParsing() throws IOException {
        KdbCredentials kdbAuth = KdbClientFactory.getKdbCredentialsFromJsonFile("src/test/resources/testcredentials.json");
        assertThat(kdbAuth.getUsername().equals("testjsonusername"));
        assertThat(kdbAuth.getPassword().equals("testjsonpassword"));
    }
}