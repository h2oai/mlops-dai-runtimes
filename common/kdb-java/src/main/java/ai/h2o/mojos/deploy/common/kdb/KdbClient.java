package ai.h2o.mojos.deploy.common.kdb;

import kx.c;
import kx.c.KException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.util.HashMap;
import java.util.Scanner;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


public class KdbClient {

    private class KdbCredentials {
        private String username;
        private String password;

        private KdbCredentials(String username, String password) {
            this.username = username;
            this.password = password;
        }

        private String getUsername() {
            return username;
        }
        private String getPassword() {
            return password;
        }
    }

    private static Scanner createAuthFileScanner(String authFileInputPath) throws IOException {
        File authFile = new File(authFileInputPath);
        if (!authFile.isFile()) {
            throw new FileNotFoundException(String.format("Could not locate input auth file: %s", authFileInputPath));
        } else {
            FileInputStream fileInputStreamObj = new FileInputStream(authFileInputPath);
            return new Scanner(fileInputStreamObj);
        }
    }

    /**
     * A simple file parser that will ingest a JSON file from the local file system containing 2 keys: username and password
     * @param authFileInputPath ex. /local/file/path/to/file.json
     * @return credentialsObject Initialized KdbCredentials class with attributes for username and password set
     * @throws IOException Will throw IOException if file does not exist locally
     */
    public static KdbCredentials getKdbCredentialsFromJsonFile(String authFileInputPath) throws IOException {
        KdbCredentials credentialsObject = null;
        authFileInputPath = authFileInputPath.trim();
        try (Scanner scanner = createAuthFileScanner(authFileInputPath)) {
            while (scanner.hasNext()) {
                String kdbAuthString = scanner.nextLine();
                Gson gson = new Gson();
                credentialsObject = gson.fromJson(kdbAuthString, KdbCredentials.class);
            }
        }
        return credentialsObject;
    }

    /**
     * Method to initialize java KDB Client
     * @param kdbHost String, ip address or hostname on which KDB process is exposed on
     * @param kdbPort Integer, port number on which the KDB process is exposed on
     * @param kdbAuth Initialized class of KdbCredentials with username and password defined
     * @return c kdbClient --> initialized KDB Client as defined by https://github.com/KxSystems/javakdb/blob/master/src/kx/c.java
     * @throws IOException Input/Output exception
     * @throws c.KException Exception thrown if there is an issue while trying to initialize the KDB Client
     * @throws IllegalArgumentException Exception thrown if either username or password is passed, but received empty string for opposite argument. EX. username="mojouser", password=""
     */
    public static c createKdbClient(String kdbHost, Integer kdbPort, KdbCredentials kdbAuth) throws IOException, c.KException {
        String username = "";
        String password = "";
        if (kdbAuth != null) {
            username = kdbAuth.getUsername();
            password = kdbAuth.getPassword();
        }
        if (username.equals("") && password.equals("")) {
            return new c(kdbHost, kdbPort);
        } else if (username.equals("") && !password.equals("")) {
            throw new IllegalArgumentException("Illegal Argument, Received empty string for username but got password");
        } else if (!username.equals("") && password.equals("")) {
            throw new IllegalArgumentException("Illegal Argument, Received username, but empty string for password");
        } else {
            return new c(kdbHost, kdbPort, username + ":" + password);
        }
    }
}
