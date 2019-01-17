package kdb.client;

import deps.javakdb.c;
import deps.javakdb.c.KException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.util.HashMap;
import java.util.Scanner;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


public class KdbClient {

    private Scanner createAuthFileScanner(String authFileInputPath) throws IOException {
        File authFile = new File(authFileInputPath);
        if (!authFile.isFile()) {
            throw new FileNotFoundException(String.format("Could not locate input auth file: %s", authFileInputPath));
        } else {
            FileInputStream fileInputStreamObj = new FileInputStream(authFileInputPath);
            return new Scanner(fileInputStreamObj);
        }
    }

    public HashMap<String, String> getKdbCredentials(String authFileInputPath) throws IOException {
        HashMap<String, String> jsonObject = null;
        authFileInputPath = authFileInputPath.trim();
        try (Scanner scanner = createAuthFileScanner(authFileInputPath)) {
            while (scanner.hasNext()) {
                String kdbAuthString = scanner.nextLine();
                Gson gson = new Gson();
                jsonObject = gson.fromJson(kdbAuthString,
                        new TypeToken<HashMap<String, Object>>() {}.getType());
            }
        }
        return jsonObject;
    }

    public static c createKdbClient(String kdbHost, Integer kdbPort, HashMap<String, String> kdbAuth) throws IOException, c.KException {
        String username = "";
        String password = "";
        if (kdbAuth != null) {
            username = kdbAuth.get("username");
            password = kdbAuth.get("password");
        }
        if (username.equals("") || password.equals("")) {
            return new c(kdbHost, kdbPort);
        } else {
            return new c(kdbHost, kdbPort, username + ":" + password);
        }
    }
}
