package com.mapr.elasticsearch.service;

import com.mapr.elasticsearch.service.service.MaprMusicElasticSearchService;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptionParser {

    private static final Logger log = LoggerFactory.getLogger(OptionParser.class);
    private String[] args;
    private Options options = new Options();
    private MaprMusicElasticSearchService elasticSearchService;

    public OptionParser(String[] args) {

        this.args = args;
        this.elasticSearchService = new MaprMusicElasticSearchService();

        options.addOption("r", "reinit", false, "When specified indices will be reinitialized.");
        options.addOption("h", "help", false, "Prints usage information.");
        options.addOption(null, "host", true, "Specifies ElasticSearch host name.");
        options.addOption("p", "port", true, "Specifies ElasticSearch transport port number.");
    }

    public void parseOpts() {

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
            if (cmd.hasOption("h")) {
                help();
            }

            if (cmd.hasOption("host")) {
                String host = cmd.getOptionValue("host");
                elasticSearchService.setHost(host);
            }

            if (cmd.hasOption("p")) {
                Integer portNumber = null;
                try {
                    portNumber = Integer.parseInt(cmd.getOptionValue("p"));
                } catch (NumberFormatException e) {
                    log.error("Failed to parse option port number");
                    help();
                }

                if (portNumber != null && portNumber <= 0) {
                    log.error("Port number must be greater than zero");
                    help();
                }

                if (portNumber != null) {
                    elasticSearchService.setPort(portNumber);
                }
            }

            if (cmd.hasOption("r")) {
                elasticSearchService.reinit();
            }

            elasticSearchService.start();
        } catch (ParseException e) {
            log.error("Failed to parse command line properties. {}", e.getMessage());
            help();
        }

    }

    private void help() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("elasticsearch-service-1.0-SNAPSHOT.jar", options);
        System.exit(0);
    }
}
