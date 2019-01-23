package ai.h2o.mojos.deploy.kdb;

import ai.h2o.mojos.runtime.MojoPipeline;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import ai.h2o.mojos.deploy.kdb.KdbMojoInterface;
import ai.h2o.mojos.deploy.common.kdb.KdbClientFactory;
import ai.h2o.mojos.deploy.common.kdb.KdbCredentials;
import kx.c;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.apache.commons.cli.*;

import java.util.Arrays;

public class KdbExample {

    private static final Logger log = LoggerFactory.getLogger(KdbExample.class);

    public static void main(final String[] args) {
        CommandLine commandLine = generateCommandLine(args);
        log.info("Received commandline arguments:");
        String mojoFilePath = commandLine.getOptionValue('f');
        log.info("Path to Mojo File: {}", mojoFilePath);
        String kdbHost = commandLine.getOptionValue("h");
        log.info("KDB Server Hostname/IP Address: {}", kdbHost);
        Integer kdbPort = Integer.parseInt(commandLine.getOptionValue("p"));
        log.info("KDB Port Number: {}", kdbPort);
        String kdbAuthFilePath = commandLine.getOptionValue("auth", "");
        log.info("Path to KDB Credentials JSON File: {}", kdbAuthFilePath);
        String qExpression = commandLine.getOptionValue("q");
        log.info("KDB Q Subscription Expression: {}", qExpression);
        String qPubTable = commandLine.getOptionValue("t");
        log.info("KDB Table to publish to: {}", qPubTable);
        String dropColumns = commandLine.getOptionValue("d", "");
        log.info("Columns to Drop from KDB Tickerplant Response: {}", dropColumns);
        try {
            int counter = 0;
            c kdbClient;
            MojoPipeline model = MojoPipeline.loadFrom(mojoFilePath);
            if (!kdbAuthFilePath.equals("")) {
                kdbClient = KdbClientFactory.createKdbClient(kdbHost, kdbPort, kdbAuthFilePath);
            } else {
                kdbClient = KdbClientFactory.createKdbClient(kdbHost, kdbPort);
            }
            c kdbSubscribedClient = KdbMojoInterface.Subscribe(kdbClient, qExpression);
            while (true) {
                Object kdbResponse = KdbMojoInterface.Retrieve(kdbSubscribedClient);
                if ((counter % 10) == 0) {
                    log.info("Processed {} responses from KDB", counter);
                }
                MojoFrame iframe = KdbMojoInterface.Parse(kdbResponse, model, dropColumns);
                MojoFrame oframe = model.transform(iframe);
                KdbMojoInterface.Publish(kdbSubscribedClient, kdbResponse, qPubTable, oframe);
                counter++;
            }
        } catch (c.KException e) {
            log.error("Exception from KDB Client", e);
        } catch(Exception e) {
            log.error("Exception during process", e);
        }
    }

    private static CommandLine generateCommandLine(final String[] cliArguments) {
        final CommandLineParser cmdLineParser = new DefaultParser();
        CommandLine commandLine = null;
        Options options = new Options();
        options.addRequiredOption("f", "mojofilepath", true, "location on local filesystem of pipline.mojo file");
        options.addRequiredOption("h", "kdbhost", true, "hostname or ip address of kdb server");
        options.addRequiredOption("p", "kdbport", true, "port number on which kdb server is exposed");
        options.addOption("auth", "kdbauth", true, "location on local filesystem of kdbcredentials.json file");
        options.addRequiredOption("q", "qexpression", true, "q expression used to subscribe to KDB Tickerplant");
        options.addRequiredOption("t", "pubtable", true, "table in KDB server to publish results to");
        options.addOption("d", "dropcolumns", true, "comma separated list of columns to drop from KDB Tickerplant response");
        try {
            commandLine = cmdLineParser.parse(options, cliArguments);
        } catch (ParseException parseException) {
            log.error("Unable to parse command-line arguments {} due to: {}", Arrays.toString(cliArguments), parseException);
        }
        return commandLine;
    }
}