package com.m2mci.mqttKafkaBridge;

import java.io.OutputStream;
import java.io.PrintStream;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class CommandLineParser {
	private static final String ALL_MQTT_TOPICS = "#";
	private static final String DEFAULT_BOOTSTRAP_KAFKA = "localhost:9092";
	private static final String DEFAULT_MQTT_SERVER_URI = "tcp://localhost:1883";

	private static final int DEFAULT_KAFKA_BATCH_SIZE = 32000;
	private static final int DEFAULT_KAFKA_BUFFER_SIZE = 128000000;

	@Option(name="--id", usage="MQTT Client ID, if not specified, will set a random client id with prefix bridge-")
	private String clientId = null;

	@Option(name="--mqtt", usage="MQTT Server URI")
	private String mqttServer = DEFAULT_MQTT_SERVER_URI;

	@Option(name="--kafka", usage="Bootstrap Kafka Server, e.g. 10.0.1.100:9092")
	private String kafkaServer = DEFAULT_BOOTSTRAP_KAFKA;
	
	@Option(name="--topics", usage="MQTT topic filters (comma-separated)")
	private String mqttTopicFilters = ALL_MQTT_TOPICS;

	@Option(name="--kafka-batch-size", usage="Kafka batch size, e.g. 1000")
	private int kafkaBatchSize = DEFAULT_KAFKA_BATCH_SIZE;

	@Option(name="--kafka-buffer-size", usage="Kafka batch buffer size in bytes, e.g. 64000000")
	private int kafkaBufferSize = DEFAULT_KAFKA_BUFFER_SIZE;
	
	@Option(name="--help", aliases="-h", usage="Show help")
	private boolean showHelp = false;
	
	private CmdLineParser parser = new CmdLineParser(this);
	
	public String getClientId() {
		return clientId;
	}

	public String getMqttServer() {
		return mqttServer;
	}

	public String getKafkaServer() {
		return kafkaServer;
	}

	public String[] getMqttTopicFilters() {
		return mqttTopicFilters.split(",");
	}

	public void parse(String[] args) throws CmdLineException {
		parser.parseArgument(args);
		if (showHelp) {
			printUsage(System.out);
			System.exit(0);
		}
	}

	public void printUsage(OutputStream out) {
		PrintStream stream = new PrintStream(out);
		stream.println("java " + Bridge.class.getName() + " [options...]");
		parser.printUsage(out);
	}

	public int getKafkaBatchSize() {
		return kafkaBatchSize;
	}

	public int getKafkaBufferSize() {
		return kafkaBufferSize;
	}
}
