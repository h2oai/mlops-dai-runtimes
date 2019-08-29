package ai.h2o.mojos.deploy.common.kdb;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.MalformedJsonException;
import kx.c;
import kx.c.KException;

import java.io.FileReader;
import java.io.IOException;


public class KdbClientFactory {

    public static KdbCredentials getKdbCredentialsFromJsonFile(String authFileInputPath) throws IOException {
        JsonElement jsonElement = new JsonParser().parse(new FileReader(authFileInputPath));
        Gson gson = new Gson();
        validateJsonObject(jsonElement);
        return gson.fromJson(jsonElement, KdbCredentials.class);
    }

    /**
     * Method to generate an initialized Java KDB Client attached to the specified Host and Port on which a KDB Server is running
     *
     * @param kdbHost String, The IP Address or Hostname of the KDB Server
     * @param kdbPort Integer, The port number of the KDB Server on which it is exposed
     * @return {@link kx.c} Initialized KDB Client as defined by https://github.com/KxSystems/javakdb/blob/master/src/kx/c.java
     * @throws IOException IOException Input/Output exception
     * @throws KException c.KException Exception thrown if there is an issue while trying to initialize the KDB Client
     */
    public static c createKdbClientNoAuth(String kdbHost, Integer kdbPort) throws IOException, KException {
        return new c(kdbHost, kdbPort);
    }

    /**
     * Method to generate an initialized Java KDB Client attached to the specified Host and Port on which a KDB Server is running,
     * using {@link KdbCredentials} class to pass credentials to the KDB Server for authentication
     *
     * @param kdbHost String, The IP Address or Hostname of the KDB Server
     * @param kdbPort Integer, The port number of the KDB Server on which it is exposed
     * @param kdbAuth {@link KdbCredentials} Previously initialized class of KdbCredentials containing KDB Server Credentials
     * @return {@link kx.c} Initialized KDB Client as defined by https://github.com/KxSystems/javakdb/blob/master/src/kx/c.java
     * @throws IOException IOException Input/Output exception
     * @throws KException c.KException Exception thrown if there is an issue while trying to initialize the KDB Client
     */
    public static c createKdbClient(String kdbHost, Integer kdbPort, KdbCredentials kdbAuth) throws IOException, KException {
        String username = kdbAuth.getUsername();
        String password = kdbAuth.getPassword();
        if (username.equals("") || password.equals("")) {
            throw new IllegalArgumentException("Either Username or Password provided was an empty string. \n" +
                    "Check that the credentials provided are correct");
        }
        return new c(kdbHost, kdbPort, username + ":" + password);
    }

    /**
     * Method to generate an initialized Java KDB Client attached to the specified Host and Port on which a KDB Server is running,
     * using JSON file on local system to provide KDB Server credentials
     *
     * @param kdbHost String, The IP Address or Hostname of the KDB Server
     * @param kdbPort Integer, The port number of the KDB Server on which it is exposed
     * @param authFileInputPath String, Filepath to JSON file on local file system containing credentials,
     *                          EX. /local/path/to/credentials.json
     *                          JSON Structure: {"username":"myusername", "password":"mypassword"}
     * @return {@link kx.c} Initialized KDB Client as defined by https://github.com/KxSystems/javakdb/blob/master/src/kx/c.java
     * @throws IOException IOException Input/Output exception
     * @throws KException c.KException Exception thrown if there is an issue while trying to initialize the KDB Client
     */
    public static c createKdbClient(String kdbHost, Integer kdbPort, String authFileInputPath) throws IOException, KException {
        KdbCredentials kdbAuth = getKdbCredentialsFromJsonFile(authFileInputPath);
        return createKdbClient(kdbHost, kdbPort, kdbAuth);
    }

    private static void validateJsonObject(JsonElement jsonElement) throws MalformedJsonException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (!jsonObject.has("username") || !jsonObject.has("password")) {
            throw new MalformedJsonException(String.format("Json provided does not have correct fields: %s, instead of 'username' and 'password'", jsonObject.keySet().toString()));
        }
    }
}