package ai.h2o.mojos.deploy.common.kdb;

import com.google.gson.stream.MalformedJsonException;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KdbClientFactoryTest {

    @Test
    void validateJsonCredentialsParsing() throws IOException {
        KdbCredentials kdbAuth = KdbClientFactory.getKdbCredentialsFromJsonFile("src/test/resources/testcredentials.json");
        assertThat(kdbAuth.getUsername().equals("testjsonusername"));
        assertThat(kdbAuth.getPassword().equals("testjsonpassword"));
    }

    @Test
    void validateMalformedKeyUsernameJsonCredentials() throws IOException {
        MalformedJsonException exception =
                assertThrows(MalformedJsonException.class, () -> KdbClientFactory.getKdbCredentialsFromJsonFile("src/test/resources/testincorrectkeyusername.json"));
        assertThat(exception.getMessage()).contains("Json provided does not have correct fields: [uesrname, password], instead of 'username' and 'password'");
    }

    @Test
    void validateMalformedKeyPasswordJsonCredentials() {
        MalformedJsonException exception =
                assertThrows(MalformedJsonException.class, () -> KdbClientFactory.getKdbCredentialsFromJsonFile("src/test/resources/testincorrectkeypassword.json"));
        assertThat(exception.getMessage()).contains("Json provided does not have correct fields: [username, pasword], instead of 'username' and 'password'");
    }

    @Test
    void validateTooManyKeys() throws IOException {
        KdbCredentials kdbAuth = KdbClientFactory.getKdbCredentialsFromJsonFile("src/test/resources/testtoomanykeys.json");
        assertThat(kdbAuth.getUsername().equals("testjsonusername"));
        assertThat(kdbAuth.getPassword().equals("testjsonpassword"));
    }

    @Test
    void validateMissingKey() {
        MalformedJsonException exception =
                assertThrows(MalformedJsonException.class, () -> KdbClientFactory.getKdbCredentialsFromJsonFile("src/test/resources/testmissingkey.json"));
        assertThat(exception.getMessage()).contains("Json provided does not have correct fields: [password], instead of 'username' and 'password'");
    }
}
