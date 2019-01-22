package h2oai.dai.kdb.mojo;

import ai.h2o.mojos.deploy.common.kdb.KdbClient;
import ai.h2o.mojos.deploy.common.kdb.KdbClient.KdbCredentials;
import kx.c;
import kx.c.Flip;
import h2oai.dai.kdb.mojo.MojoKdbTransform;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import ai.h2o.mojos.runtime.MojoPipeline;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import ai.h2o.mojos.runtime.lic.LicenseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class KdbMojoInterface {

    private static final Logger log = LoggerFactory.getLogger(KdbMojoInterface.class);

    /**
     * Small wrapper method that allows user to load the mojo artifact from local file system
     * @param pathToMojoFile String ex. /local/path/to/pipeline.mojo file
     * @return MojoPipeline Object of class MojoPipeline, the loaded model artifact that can be used to transform new data
     * @throws IOException Throws IOException if pipeline.mojo file cannot be found at local path
     * @throws LicenseException Throws LicenseException if DRIVERLESS_AI_LICENSE_FILE is not defined or path does not point to license.sig
     */
    public static MojoPipeline loadMojo(String pathToMojoFile) throws IOException, LicenseException {
        log.info("Loading mojo scoring pipeline from: " + pathToMojoFile);
        MojoPipeline model = MojoPipeline.loadFrom(pathToMojoFile);
        log.info("Loaded mojo scoring pipeline successfully");
        return model;
    }

    /**
     * Method to instantiate KDB Client and subscribe to KDB Tickerplant.
     * @param kdbHost String IP Address or hostname where KDB Tickerplant is running
     * @param kdbPort Int Port Number at which the KDB Tickerplant is exposed
     * @param kdbAuthFilePath String ex. /path/to/credentials.json which contains two keys: username, password in format {"username": "foo", "password": "bar"}
     * @param qExpression String q Expression passed to KDB Client to subscribe to Tickerplant. ex. ".u.sub[\`walmarttick;\`]"
     * @return c Object for Class c from KDB Java Client, https://github.com/KxSystems/javakdb/blob/master/src/kx/c.java
     * @throws IOException Throws IOException if kdbAuthFilePath is not found on local system
     * @throws c.KException Throws c.KException if there is an error instantiating the KDB Client
     */
    public static c Subscribe(String kdbHost, Integer kdbPort, String kdbAuthFilePath, String qExpression) throws IOException, c.KException {
        KdbCredentials kdbAuth = null;
        if (kdbAuthFilePath.equals("")) {
            log.info("kdbAuthFilePath was an empty string, no credentials were gathered");
        } else {
            kdbAuth = KdbClient.getKdbCredentialsFromJsonFile(kdbAuthFilePath);
        }
        c subscribedKdbClient = KdbClient.createKdbClient(kdbHost, kdbPort, kdbAuth);
        log.info("Connect to KDB Tickerplant at host: " + kdbHost + " and port: " + kdbPort);
        subscribedKdbClient.k(qExpression);
        log.info("Subscribed to KDB Tickerplant with expression: " + qExpression);
        return subscribedKdbClient;
    }

    /**
     * Small wrapper around KDB Client method client.k() that returns the next object from KDB Tickerplant. Mostly for convenience.
     * @param subscribedKdbClient c Object for Class c from KDB Java Client, https://github.com/KxSystems/javakdb/blob/master/src/kx/c.java
     * @return Object kdbResponse, contains the next batch of data received from KDB Tickerplant as a 1-D Object array
     * @throws IOException IOException thrown by Client
     * @throws c.KException KException thrown by KDB Client
     */
    public static Object Retrieve(c subscribedKdbClient) throws IOException, c.KException {
        return subscribedKdbClient.k();
    }

    /**
     * Method to parse data obtained from KDB Tickerplant and convert to MojoFrame for inference
     * @param kdbResponse Object, response from KDB Tickerplant (new data) typically obtained from: Object kdbResponse = subscribedKdbClient.k();
     * @param model MojoPipeline, loaded model pipeline as obtained from loadMojo
     * @param dropCols String, comma separated list of columns that user wishes to drop prior to creating the MojoFrame. Ex. time,sym,optionalOtherDroppedColumn
     * @return MojoFrame, contains new data from KDB converted into MojoFrame for inference
     * @throws IOException Throws IOException
     */
    public static MojoFrame Parse(Object kdbResponse, MojoPipeline model, String dropCols) throws IOException {
        MojoFrame iframe = null;
        if (kdbResponse != null) {
            Object[] data = (Object[]) kdbResponse;
            Flip kdbFlipTable = (c.Flip) data[2];
            iframe =  MojoKdbTransform.createMojoFrameFromKdbFlip(model, kdbFlipTable, dropCols);
        } else {
            log.warn("DEBUG: Nothing to do as object received from KDB was null");
        }
        return iframe;
    }

    /**
     * Method to publish new predictions to KDB Tickerplant so that they can be consumed by other KDB processes
     * @param subscribedKdbClient c Object for Class c from KDB Java Client, https://github.com/KxSystems/javakdb/blob/master/src/kx/c.java
     * @param kdbResponse Object array of original data received from KDB Tickerplant
     * @param qPubTable String, name of table to which the KDB Client will publish the new predictions.
     * @param oframe MojoFrame, MojoFrame containing the predictions made by the mojo model artifact
     * @throws IOException IOException thrown by method
     * @throws c.KException KException thrown by method
     */
    public static void Publish (c subscribedKdbClient, Object kdbResponse, String qPubTable, MojoFrame oframe) throws IOException, c.KException {
        if (kdbResponse != null) {
            Object[] data = (Object[]) kdbResponse;
            Flip kdbFlipTable = (c.Flip) data[2];
            Object[] pubObject = MojoKdbTransform.generateMojoPredictionPublishObject(qPubTable, oframe, kdbFlipTable);
            subscribedKdbClient.k(pubObject);
        } else {
            log.warn("DEBUG: Nothing to do as object received from KDB was null");
        }
    }
}