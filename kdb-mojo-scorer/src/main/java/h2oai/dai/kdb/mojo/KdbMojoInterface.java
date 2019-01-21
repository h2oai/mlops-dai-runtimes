package h2oai.dai.kdb.mojo;

import kdb.client.KdbClient;
import deps.javakdb.c;
import deps.javakdb.c.Flip;
import h2oai.dai.kdb.mojo.MojoKdbTransform;
import java.io.IOException;
import java.util.HashMap;
import ai.h2o.mojos.runtime.MojoPipeline;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class KdbMojoInterface {

    private static final Logger log = LoggerFactory.getLogger(KdbMojoInterface.class);

    public static c Subscribe(String kdbHost, Integer kdbPort, String kdbAuthFilePath) throws IOException, c.KException {
        HashMap <String, String> kdbAuth = null;
        if (kdbAuthFilePath.equals("")) {
            log.info("kdbAuthFilePath was an empty string, no credentials were gathered");
        } else {
            kdbAuth = KdbClient.getKdbCredentials(kdbAuthFilePath);
        }
        return KdbClient.createKdbClient(kdbHost, kdbPort, kdbAuth);
    }

    public static MojoFrame Retrieve(Object kdbResponse, MojoPipeline model) throws IOException {
        MojoFrame iframe = null;
        if (kdbResponse != null) {
            Object[] data = (Object[]) kdbResponse;
            Flip kdbFlipTable = (c.Flip) data[2];
            iframe =  MojoKdbTransform.createMojoFrameFromKdbFlip(model, kdbFlipTable);
        } else {
            log.info("DEBUG: Nothing to do as object received from KDB was null");
        }
        return iframe;
    }

    public static void Publish (c subscribedKdbClient, Object kdbResponse, String qPubTable, MojoFrame oframe) throws IOException, c.KException {
        if (kdbResponse != null) {
            Object[] data = (Object[]) kdbResponse;
            Flip kdbFlipTable = (c.Flip) data[2];
            Object[] pubObject = MojoKdbTransform.generateMojoPredictionPublishObject(qPubTable, oframe, kdbFlipTable);
            subscribedKdbClient.k(pubObject);
        } else {
            log.info("DEBUG: Nothing to do as object received from KDB was null");
        }
    }
}