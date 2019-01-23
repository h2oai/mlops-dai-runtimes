package ai.h2o.mojos.deploy.common.kdb;

import com.google.gson.stream.MalformedJsonException;
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

    @Test
    void validateMalformedKeyUsernameJsonCredentials() {
        try {
            KdbCredentials kdbAuth = KdbClientFactory.getKdbCredentialsFromJsonFile("src/test/resources/testincorrectkeyusername.json");
        } catch (Exception e) {
            assertThat(e instanceof MalformedJsonException);
        }
    }

    @Test
    void validateMalformedKeyPasswordJsonCredentials() {
        try {
            KdbCredentials kdbAuth = KdbClientFactory.getKdbCredentialsFromJsonFile("src/test/resources/testincorrectkeypassword.json");
        } catch (Exception e) {
            assertThat(e instanceof MalformedJsonException);
        }
    }

    @Test
    void validateTooManyKeys() throws IOException {
        KdbCredentials kdbAuth = KdbClientFactory.getKdbCredentialsFromJsonFile("src/test/resources/testtoomanykeys.json");
        assertThat(kdbAuth.getUsername().equals("testjsonusername"));
        assertThat(kdbAuth.getPassword().equals("testjsonpassword"));
    }

    @Test
    void validateMissingKey() {
        try {
            KdbCredentials kdbAuth = KdbClientFactory.getKdbCredentialsFromJsonFile("src/test/resources/testmissingkey.json");
        } catch (Exception e) {
            assertThat(e instanceof MalformedJsonException);
        }
    }
}
