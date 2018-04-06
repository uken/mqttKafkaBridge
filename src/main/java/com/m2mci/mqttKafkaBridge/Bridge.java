package com.m2mci.mqttKafkaBridge;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Bridge implements MqttCallback {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private MqttAsyncClient mqtt;
	private KafkaProducer<String, byte[]> kafkaProducer;
	private static final int MQTT_CLIENT_ID_MAX_LENGTH = 23;
	
	private void connect(String serverURI, String clientId, String bootstrapServers, int kafkaBatchSize, int kafkaBufferSize, String dataDir) throws MqttException {
		logger.info("Connecting to mqtt with clientId: " + clientId);
		mqtt = new MqttAsyncClient(serverURI, clientId, new MqttDefaultFilePersistence(dataDir));
		mqtt.setCallback(this);
		IMqttToken token = mqtt.connect();
		Properties props = new Properties();
		props.put("bootstrap.servers", bootstrapServers);
		props.put("acks", "1");
		props.put("retries", 0);
		props.put("batch.size", kafkaBatchSize);
		props.put("linger.ms", 1);
		props.put("buffer.memory", kafkaBufferSize);
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
		kafkaProducer = new KafkaProducer<String, byte[]>(props);
		token.waitForCompletion();
		logger.info("Connected to MQTT and Kafka");
	}

	private void reconnect() throws MqttException {
		IMqttToken token = mqtt.connect();
		token.waitForCompletion();
	}

	private void subscribe(String[] mqttTopicFilters) throws MqttException {
		int[] qos = new int[mqttTopicFilters.length];
		for (int i = 0; i < qos.length; ++i) {
			qos[i] = 0;
		}
		logger.info("Subscribing to topics:");
		for ( String t : mqttTopicFilters) {
			logger.info(t);
		}
		mqtt.subscribe(mqttTopicFilters, qos);
	}

	public void connectionLost(Throwable cause) {
		logger.warn("Lost connection to MQTT server", cause);
		while (true) {
			try {
				logger.info("Attempting to reconnect to MQTT server");
				reconnect();
				logger.info("Reconnected to MQTT server, resuming");
				return;
			} catch (MqttException e) {
				logger.warn("Reconnect failed, retrying in 10 seconds", e);
			}
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
			}
		}
	}

	public void deliveryComplete(IMqttDeliveryToken token) {
		// TODO Auto-generated method stub
		
	}

	public void messageArrived(String topic, MqttMessage message) throws Exception {
		byte[] payload = message.getPayload();
		ProducerRecord<String, byte[]> data = new ProducerRecord<String, byte[]>(topic, payload);
		kafkaProducer.send(data);
	}

	private static String getClientId() {
		return ("bridge-" + java.util.UUID.randomUUID().toString()).substring(0, MQTT_CLIENT_ID_MAX_LENGTH);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {

		Properties properties = new Properties();
		properties.load(new FileInputStream(args[0]));

		String mqttServer = properties.getProperty("mqtt");
		String mqttDataDir = properties.getProperty("mqtt.datadir");
		String kafkaServer = properties.getProperty("kafka");
		int kafkaBatchSize = Integer.parseInt(properties.getProperty("kafka.batch.size"));
		int kafkaBufferSize = Integer.parseInt(properties.getProperty("kafka.buffer.size"));
		int numOfBridges = Integer.parseInt(properties.getProperty("numOfBridges"));
		String[] topics = properties.getProperty("topics").split(",");

		try {
			for (int i = 0; i < numOfBridges; i++) {
				Bridge bridge = new Bridge();
				bridge.connect(mqttServer, getClientId(), kafkaServer, kafkaBatchSize, kafkaBufferSize, mqttDataDir);
				bridge.subscribe(topics);
			}
		} catch (MqttException e) {
			e.printStackTrace(System.err);
		}
	}
}
