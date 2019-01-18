package h2oai.dai.kdb.mojo;

import kdb.client.KdbClient;
import deps.javakdb.c;
import deps.javakdb.c.Flip;
import h2oai.dai.kdb.mojo.MojoKdbTransform;
import java.io.IOException;
import java.util.HashMap;
import ai.h2o.mojos.runtime.MojoPipeline;
import ai.h2o.mojos.runtime.frame.MojoFrame;


public class KdbMojoInterface {

    public static c Subscribe(String kdbHost, Integer kdbPort, String kdbAuthFilePath, String qSubExpression) throws IOException, c.KException {
        HashMap <String, String> kdbAuth = KdbClient.getKdbCredentials(kdbAuthFilePath);
        c kdbJavaClient = KdbClient.createKdbClient(kdbHost, kdbPort, kdbAuth);
        kdbJavaClient.k(qSubExpression);
        return kdbJavaClient;
    }

    public static MojoFrame Retrieve(c subscribedKdbClient, MojoPipeline model) throws IOException, c.KException {
        Object r = subscribedKdbClient.k();
        MojoFrame iframe = null;
        if (r != null) {
            Object[] data = (Object[]) r;
            Flip kdbFlipTable = (c.Flip) data[2];
            iframe =  MojoKdbTransform.createMojoFrameFromKdbFlip(model, kdbFlipTable);
        } else {
            // have logger make note that r was null
        }
        return iframe;
    }

    public static void Publish (c subscribedKdbClient, String qPubTable, MojoFrame oframe) throws IOException, c.KException {
        Object r = subscribedKdbClient.k();
        if (r != null) {
            Object[] data = (Object[]) r;
            Flip kdbFlipTable = (c.Flip) data[2];
            Object[] pubObject = MojoKdbTransform.generateMojoPredictionPublishObject(qPubTable, oframe, kdbFlipTable);
            subscribedKdbClient.k(pubObject);
        } else {
            // have logger make a note that r was null
        }
    }
}