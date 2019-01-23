package ai.h2o.mojos.deploy.kdb;

import kx.c;
import kx.c.Flip;
import ai.h2o.mojos.deploy.kdb.MojoKdbTransform;
import java.io.IOException;
import ai.h2o.mojos.runtime.MojoPipeline;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import ai.h2o.mojos.runtime.lic.LicenseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class KdbMojoInterface {

    private static final Logger log = LoggerFactory.getLogger(KdbMojoInterface.class);

    /**
     * Method to instantiate KDB Client and subscribe to KDB Tickerplant.
     *
     * @param initializedKdbClient {@link kx.c} an initialzed instance of KDB Client as defined by https://github.com/KxSystems/javakdb/blob/master/src/kx/c.java
     * @param qExpression String q Expression passed to KDB Client to subscribe to Tickerplant. ex. ".u.sub[\`walmarttick;\`]"
     * @return c Object for Class c from KDB Java Client, https://github.com/KxSystems/javakdb/blob/master/src/kx/c.java
     * @throws IOException Throws IOException if kdbAuthFilePath is not found on local system
     * @throws c.KException Throws c.KException if there is an error instantiating the KDB Client
     */
    public static c Subscribe(c initializedKdbClient, String qExpression) throws IOException, c.KException {
        initializedKdbClient.k(qExpression);
        log.info("Subscribed to KDB Tickerplant with expression: {}", qExpression);
        return initializedKdbClient;
    }

    /**
     * Small wrapper around KDB Client method client.k() that returns the next object from KDB Tickerplant. Mostly for convenience.
     *
     * @param subscribedKdbClient c Object for Class c from KDB Java Client, https://github.com/KxSystems/javakdb/blob/master/src/kx/c.java
     * @return Object[] kdbResponse, contains the next batch of data received from KDB Tickerplant as a 1-D Object array
     * @throws IOException IOException thrown by Client
     * @throws c.KException KException thrown by KDB Client
     */
    public static Object[] Retrieve(c subscribedKdbClient) throws IOException, c.KException {
        Object[] kdbResponse = (Object[]) subscribedKdbClient.k();
        kdbResponse = validateKdbResponse(kdbResponse);
        return kdbResponse;
    }

    /**
     * Method to parse data obtained from KDB Tickerplant and convert to MojoFrame for inference
     *
     * @param kdbResponse Object[], response from KDB Tickerplant (new data) typically obtained from: Object kdbResponse = subscribedKdbClient.k();
     * @param model MojoPipeline, loaded model pipeline as obtained from loadMojo
     * @param dropCols String, comma separated list of columns that user wishes to drop prior to creating the MojoFrame. Ex. time,sym,optionalOtherDroppedColumn
     * @return MojoFrame, contains new data from KDB converted into MojoFrame for inference
     * @throws IOException Throws IOException
     */
    public static MojoFrame Parse(Object[] kdbResponse, MojoPipeline model, String dropCols) throws IOException {
        Flip kdbFlipTable = (c.Flip) kdbResponse[2];
        return MojoKdbTransform.createMojoFrameFromKdbFlip(model, kdbFlipTable, dropCols);
    }

    /**
     * Method to publish new predictions to KDB Tickerplant so that they can be consumed by other KDB processes
     *
     * @param subscribedKdbClient c Object for Class c from KDB Java Client, https://github.com/KxSystems/javakdb/blob/master/src/kx/c.java
     * @param kdbResponse Object[] array of original data received from KDB Tickerplant
     * @param qPubTable String, name of table to which the KDB Client will publish the new predictions.
     * @param oframe MojoFrame, MojoFrame containing the predictions made by the mojo model artifact
     * @throws IOException IOException thrown by method
     * @throws c.KException KException thrown by method
     */
    public static void Publish (c subscribedKdbClient, Object[] kdbResponse, String qPubTable, MojoFrame oframe) throws IOException, c.KException {
        Flip kdbFlipTable = (c.Flip) kdbResponse[2];
        Object[] pubObject = MojoKdbTransform.generateMojoPredictionPublishObject(qPubTable, oframe, kdbFlipTable);
        subscribedKdbClient.k(pubObject);
    }

    private static Object[] validateKdbResponse(Object[] kdbResponse) throws RuntimeException {
        if (kdbResponse == null) {
            throw new RuntimeException("There was no data received from KDB Tickerplant");
        } else if (kdbResponse.length != 3) {
            throw new ArrayIndexOutOfBoundsException(String.format("Data array recieved from KDB Tickerplant did not have proper length: %d instead of 3", kdbResponse.length));
        } else {
            return kdbResponse;
        }
    }
}