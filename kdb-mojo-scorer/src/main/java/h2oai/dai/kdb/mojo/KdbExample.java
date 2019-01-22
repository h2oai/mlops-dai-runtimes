package h2oai.dai.kdb.mojo;

import ai.h2o.mojos.runtime.MojoPipeline;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import h2oai.dai.kdb.mojo.KdbMojoInterface;
import kx.c;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class KdbExample {

    private static final Logger log = LoggerFactory.getLogger(KdbExample.class);

    public static void main(final String[] args) {
        String mojoFilePath = "";
        String kdbHost = "";
        int kdbPort = 5001; // default kdb server port
        String kdbAuthFilePath = "";
        String qExpression = "";
        String qPubTable = "";
        String dropColumns = "";

        for (int i = 0; i < args.length; i += 2) {
            switch (args[i]) {
                case "-f":
                    mojoFilePath = args[i + 1];
                    log.info("mojoFilePath: " + mojoFilePath);
                    break;
                case "-h":
                    kdbHost = args[i + 1];
                    log.info("kdbHost: " + kdbHost);
                    break;
                case "-p":
                    kdbPort = Integer.parseInt(args[i + 1]);
                    log.info("kdbPort: " + kdbPort);
                    break;
                case "-auth":
                    kdbAuthFilePath = args[i + 1];
                    log.info("kdbAuth: " + kdbAuthFilePath);
                    break;
                case "-sub":
                    qExpression = args[i + 1]; // something like ".u.sub[`walmarttick;`]"
                    log.info("qExpression: " + qExpression);
                    break;
                case "-pub":
                    qPubTable = args[i + 1];
                    log.info("qPubTable: " + qPubTable);
                    break;
                case "-drop":
                    dropColumns = args[i + 1];
                    log.info("dropColumns: " + dropColumns);
                default:
                    break;
            }
        }
        try {
            int counter = 0;
            int errorCounter = 0;
            MojoPipeline model = KdbMojoInterface.loadMojo(mojoFilePath);
            c subscribedKdbClient = KdbMojoInterface.Subscribe(kdbHost, kdbPort, kdbAuthFilePath, qExpression);
            try {
                while (true) {
                    Object kdbResponse = KdbMojoInterface.Retrieve(subscribedKdbClient);
                    if ((counter % 10) == 0) {
                        log.info("Processed " + counter + " responses from KDB");
                    }
                    MojoFrame iframe = KdbMojoInterface.Parse(kdbResponse, model, dropColumns);
                    MojoFrame oframe = model.transform(iframe);
                    KdbMojoInterface.Publish(subscribedKdbClient, kdbResponse, qPubTable, oframe);
                    counter++;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                log.warn("ArrayIndexOutOfBoundsException", e);
                incrementErrorCounter(errorCounter);
            } catch (NullPointerException e) {
                log.warn("NullPointerException", e);
                incrementErrorCounter(errorCounter);
            }
        } catch (c.KException e) {
            log.error("Exception from KDB Client", e);
        } catch(Exception e) {
            log.error("Exception during process", e);
        }
    }

    private static void incrementErrorCounter(int errorCounter) throws Exception {
        errorCounter++;
        if (errorCounter > 10) {
            throw new Exception("Too many error, Exiting as number of exceptions has reached above threshold of 10.");
        }
    }
}