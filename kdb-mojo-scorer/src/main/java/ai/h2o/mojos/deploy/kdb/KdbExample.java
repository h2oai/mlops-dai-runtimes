package ai.h2o.mojos.deploy.kdb;

import ai.h2o.mojos.deploy.common.kdb.KdbClientFactory;
import ai.h2o.mojos.runtime.MojoPipeline;
import ai.h2o.mojos.runtime.frame.MojoFrame;
import kx.c;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class KdbExample {

    private static final Logger log = LoggerFactory.getLogger(KdbExample.class);

    public static void main(final String[] args) {
        try {
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

            runKdbMojoDeployment(mojoFilePath, kdbHost, kdbPort, kdbAuthFilePath, qExpression, qPubTable, dropColumns);
        } catch(ParseException e) {
            log.error(String.format("Unable to parse command-line arguments %s, try passing flag -help (With no other flags) for cli help information", Arrays.toString(args)), e);
        }
    }

    private static void runKdbMojoDeployment(String mojoFilePath, String kdbHost, Integer kdbPort, String kdbAuthFilePath,
                                             String qExpression, String qPubTable, String dropColumns) {
        try {
            int counter = 0;
            c kdbClient;
            log.info("Beginning to load mojo pipeline");
            MojoPipeline model = MojoPipeline.loadFrom(mojoFilePath);
            log.info("Successfully Loaded Mojo Pipeline.");
            if (!kdbAuthFilePath.equals("")) {
                kdbClient = KdbClientFactory.createKdbClient(kdbHost, kdbPort, kdbAuthFilePath);
            } else {
                kdbClient = KdbClientFactory.createKdbClientNoAuth(kdbHost, kdbPort);
            }
            c kdbSubscribedClient = KdbMojoInterface.Subscribe(kdbClient, qExpression);
            while (true) {
                Object[] kdbResponse = KdbMojoInterface.Retrieve(kdbSubscribedClient);
                if ((counter % 10) == 0) {
                    log.info("Processed {} responses from KDB", counter);
                }
                MojoFrame iframe = KdbMojoInterface.Parse(kdbResponse, model.getInputFrameBuilder(), dropColumns);
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

    private static CommandLine generateCommandLine(String[] cliArguments) throws ParseException {
        CommandLineParser cmdLineParser = new DefaultParser();
        CommandLineParser helpLineParser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        Options helpOptions = new Options();
        helpOptions.addOption("help", "Prints all commandline options for KDB Mojo Deployment Example");
        Options options = new Options();
        options.addRequiredOption("f", "mojofilepath", true, "location on local filesystem of pipline.mojo file");
        options.addRequiredOption("h", "kdbhost", true, "hostname or ip address of kdb server");
        options.addRequiredOption("p", "kdbport", true, "port number on which kdb server is exposed");
        options.addOption("auth", "kdbauth", true, "location on local filesystem of kdbcredentials.json file");
        options.addRequiredOption("q", "qexpression", true, "q expression used to subscribe to KDB Tickerplant");
        options.addRequiredOption("t", "pubtable", true, "table in KDB server to publish results to");
        options.addOption("d", "dropcolumns", true, "comma separated list of columns to drop from KDB Tickerplant response");
        try {
            if(cliArguments.length <= 1) {
                CommandLine commandHelpLine = helpLineParser.parse(helpOptions, cliArguments);
                if (commandHelpLine.hasOption("help") || cliArguments.length == 0) {
                    formatter.printHelp("KDB Mojo Deployment Example CLI Help: ", options);
                    System.exit(1);
                }
            }
        } catch (ParseException parseException) {
            log.error("There was an exception while parsing the commandline arguments. Printing commandline help for your convenience");
            formatter.printHelp("KDB Mojo Deployment Example CLI Help:", options);
            System.exit(1);
        }
        return cmdLineParser.parse(options, cliArguments);
    }
}