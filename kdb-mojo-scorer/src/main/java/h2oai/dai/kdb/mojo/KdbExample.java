package h2oai.dai.kdb.mojo;

import ai.h2o.mojos.runtime.MojoPipeline;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import h2oai.dai.kdb.mojo.KdbMojoInterface;
import deps.javakdb.c;

public class KdbExample {

    public static void main(final String[] args) {
        String mojoFilePath = "";
        String kdbHost = "";
        int kdbPort = 5001; // default kdb server port
        String kdbAuthFilePath = "";
        String qExpression = "";
        String qPubTable = "";

        for (int i = 0; i < args.length; i += 2) {
            switch (args[i]) {
                case "-f":
                    mojoFilePath = args[i + 1];
                    System.out.println("mojoFilePath: " + mojoFilePath);
                    break;
                case "-h":
                    kdbHost = args[i + 1];
                    System.out.println("kdbHost: " + kdbHost);
                    break;
                case "-p":
                    kdbPort = Integer.parseInt(args[i + 1]);
                    System.out.println("kdbPort: " + kdbPort);
                    break;
                case "-auth":
                    kdbAuthFilePath = args[i + 1];
                    System.out.println("kdbAuth: " + kdbAuthFilePath);
                    break;
                case "-sub":
                    qExpression = args[i + 1]; // something like ".u.sub[`walmarttick;`]"
                    System.out.println("qExpression: " + qExpression);
                    break;
                case "-pub":
                    qPubTable = args[i + 1];
                    System.out.println("qPubTable: " + qPubTable);
                    break;
                default:
                    break;
            }
        }
        try {
            MojoPipeline model = MojoKdbTransform.loadMojo(mojoFilePath);
            c subscribedKdbClient = KdbMojoInterface.Subscribe(kdbHost, kdbPort, kdbAuthFilePath, qExpression);
            while(true) {
                MojoFrame iframe = KdbMojoInterface.Retrieve(subscribedKdbClient, model);
                MojoFrame oframe = model.transform(iframe);
                KdbMojoInterface.Publish(subscribedKdbClient, qPubTable, oframe);
            }
        } catch(Exception e) {
            System.out.println(e);
        }

    }
}