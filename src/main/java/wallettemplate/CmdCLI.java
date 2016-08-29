package wallettemplate;

import com.google.common.base.Enums;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;

/**
 * Created by advman on 2016-08-24.
 */


public class CmdCLI {
    private String[] args = null;
    final static Logger log = LoggerFactory.getLogger(CmdCLI.class);
    private Options options = new Options();
    private static CommandLine cmd = null;
    private String netArg = null;
    private String dataArg = null;
    private File f = null;

    public CmdCLI(String[] args) {
        this.args = args;
        // build boolean options
        options.addOption("h", "help", false, "print this message.");

        // build argument options for the choice of network
        OptionBuilder.withArgName("Network Type");
        OptionBuilder.hasArg();
        OptionBuilder.withLongOpt("network");
        OptionBuilder.withDescription("required arg; use environment name eg. main, test or regtest.");
        Option network = OptionBuilder.create("net");
        options.addOption(network);

        // build the argument options for the data directory
        OptionBuilder.withArgName("Data Directory");
        OptionBuilder.hasArg();
        OptionBuilder.withLongOpt("datadir");
        OptionBuilder.withDescription("required arg; state the data directory to be used.");
        Option datadir = OptionBuilder.create("d");
        options.addOption(datadir);
    }

    public void parse() {
        // create the parser
        CommandLineParser parser = new BasicParser();

        try {
            cmd = parser.parse(options, args);

            // help option selected
            if (cmd.hasOption("h")) {
                help();
            }

            // Network arg required
            if (cmd.getOptionValue("network") == null) {
                log.info("Network value required");
                help();
            }
            // Data directory arg required
            if (cmd.getOptionValue("datadir") == null) {
                log.info("Data directory value required");
                help();
            }

            // Validate network arg
            netArg = getNetArg("network");
            if (!Enums.getIfPresent(NetworkEnum.class, netArg).isPresent()) {
                String[] netNames = getNetNames(NetworkEnum.class);
                log.info("Network value " + netArg + " invalid.  Select one of the following " + Arrays.toString(netNames));
                help();
            }

            // Validate data directory arg
            dataArg = getDataArg("datadir");
            f = new File(dataArg);
            if (!f.exists() || !f.isDirectory()) {
                log.info("Data directory value " + dataArg + " invalid. The directory must exist.");
                help();
            }
            log.info("Value of the environment argument " + cmd.getOptionValue("network"));
            log.info("Value of the data directory argument " + cmd.getOptionValue("datadir"));

        } catch (ParseException e) {
            log.error("Failed to parse comand line properties: " + e.getMessage());
            help();
        }
    }

    // Convert the Network Enums to string array values
    public static String[] getNetNames(Class<? extends Enum<?>> e) {
        return Arrays.stream(e.getEnumConstants()).map(Enum::name).toArray(String[]::new);
    }

    // Get command line argument values for the data directory
    public static String getDataArg(String dataArg) {
        return cmd.getOptionValue(dataArg);
    }
    // Get command line argument values for the network
    public static String getNetArg(String netArg) {
        return cmd.getOptionValue(netArg).toUpperCase();
    }

    private void help() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("CmdCLI", options);
        System.exit(0);
    }
}
