/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 * 
 * You may find it more helpful to your design to adjust the
 * functionality, constants and interfaces (if there are any)
 * provided within in order to meet the needs of your specific
 * Programming the Internet of Things project.
 */

package programmingtheiot.gda.connection;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;
/**
 * Shell representation of class for student implementation.
 * 
 */
public class MqttClientConnector implements IPubSubClient, MqttCallbackExtended
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(MqttClientConnector.class.getName());
	
	// params
	
	private boolean useAsyncClient = true;
	
	private MqttAsyncClient mqttClient = null; 	private MqttConnectOptions   connOpts = null;
	private MemoryPersistence    persistence = null;
	private IDataMessageListener dataMsgListener = null;
	
	private String  clientID = null;
	private String  brokerAddr = null;
	private String  host = ConfigConst.DEFAULT_HOST;
	private String  protocol = ConfigConst.DEFAULT_MQTT_PROTOCOL;
	private int     port = ConfigConst.DEFAULT_MQTT_PORT;
	private int     brokerKeepAlive = ConfigConst.DEFAULT_KEEP_ALIVE;
	
	// constructors
	
	/**
	 * Default.
	 * 
	 */
	public MqttClientConnector()
	{
		super();
		
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		this.host = configUtil.getProperty(
			ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.HOST_KEY, ConfigConst.DEFAULT_HOST);
		
		this.brokerKeepAlive = configUtil.getInteger(
			ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.KEEP_ALIVE_KEY, ConfigConst.DEFAULT_KEEP_ALIVE);
		
		this.useAsyncClient = configUtil.getBoolean(
			ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.USE_ASYNC_CLIENT_KEY);
		
		this.clientID = MqttClient.generateClientId();
		
		// Initialize these FIRST before trying to use them
		this.persistence = new MemoryPersistence();
		this.connOpts = new MqttConnectOptions();
		this.connOpts.setKeepAliveInterval(this.brokerKeepAlive);
		this.connOpts.setCleanSession(false);
		this.connOpts.setAutomaticReconnect(true);
		
		// NOW check for TLS and configure it
		boolean enableCrypt = configUtil.getBoolean(
			ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.ENABLE_CRYPT_KEY);
		
		if (enableCrypt) {
			this.port = configUtil.getInteger(
				ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.SECURE_PORT_KEY, 8883);
			this.protocol = "ssl";
			
			String certFile = configUtil.getProperty(
				ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.CERT_FILE_KEY);
			
			try {
				SSLSocketFactory sslSocketFactory = createSocketFactory(certFile);
				this.connOpts.setSocketFactory(sslSocketFactory);  // Now connOpts exists!
				_Logger.info("TLS enabled for MQTT connection on port " + this.port);
			} catch (Exception e) {
				_Logger.log(Level.SEVERE, "Failed to setup TLS, falling back to non-TLS", e);
				this.port = configUtil.getInteger(
					ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.PORT_KEY, ConfigConst.DEFAULT_MQTT_PORT);
				this.protocol = ConfigConst.DEFAULT_MQTT_PROTOCOL;
			}
		} else {
			this.port = configUtil.getInteger(
				ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.PORT_KEY, ConfigConst.DEFAULT_MQTT_PORT);
			this.protocol = ConfigConst.DEFAULT_MQTT_PROTOCOL;
		}
		
		// Build broker address with correct protocol and port
		this.brokerAddr = this.protocol + "://" + this.host + ":" + this.port;
		_Logger.info("MQTT broker address: " + this.brokerAddr);
	}

	private SSLSocketFactory createSocketFactory(String caCertFile) throws Exception {
		// Load CA certificate
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		FileInputStream fis = new FileInputStream(caCertFile);
		X509Certificate ca = (X509Certificate) cf.generateCertificate(fis);
		fis.close();
		
		// Create KeyStore with the CA
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(null, null);
		keyStore.setCertificateEntry("ca", ca);
		
		// Create TrustManager
		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(keyStore);
		
		// Create SSLContext
		SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, tmf.getTrustManagers(), null);
		
		return sslContext.getSocketFactory();
	}
		
		
		// public methods
		
		@Override
	public boolean connectClient()
	{
		try {
			if (this.mqttClient == null) {
				// Use MqttAsyncClient instead of MqttClient
				this.mqttClient = new MqttAsyncClient(this.brokerAddr, this.clientID, this.persistence);
				this.mqttClient.setCallback(this);
			}
			
			if (!this.mqttClient.isConnected()) {
				_Logger.info("MQTT client connecting to broker: " + this.brokerAddr);
				IMqttToken token = this.mqttClient.connect(this.connOpts);
				token.waitForCompletion(5000);  // Wait up to 5 seconds for connection
				return true;
			} else {
				_Logger.warning("MQTT client already connected to broker: " + this.brokerAddr);
			}
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to connect MQTT client to broker.", e);
		}
		
		return false;
	}

	@Override
	public boolean disconnectClient()
	{
		try {
			if (this.mqttClient != null) {
				if (this.mqttClient.isConnected()) {
					_Logger.info("Disconnecting MQTT client from broker: " + this.brokerAddr);
					this.mqttClient.disconnect();
					return true;
				} else {
					_Logger.warning("MQTT client not connected to broker: " + this.brokerAddr);
				}
			}
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to disconnect MQTT client from broker: " + this.brokerAddr, e);
		}
		
		return false;
	}

	public boolean isConnected()
	{
		if (this.mqttClient != null) {
			return this.mqttClient.isConnected();
		}
		return false;
	}
	
	@Override
	public boolean publishMessage(ResourceNameEnum topicName, String msg, int qos)
	{
		if (topicName == null || msg == null) {
			_Logger.warning("Invalid parameters for publishMessage()");
			return false;
		}
		
		if (this.mqttClient == null || !this.mqttClient.isConnected()) {
			_Logger.warning("MQTT client not connected. Cannot publish message.");
			return false;
		}
		
		try {
			String topic = topicName.getResourceName();
			MqttMessage mqttMsg = new MqttMessage(msg.getBytes());
			mqttMsg.setQos(qos);
			
			_Logger.info("Publishing message to topic: " + topic + " with QoS: " + qos);
			this.mqttClient.publish(topic, mqttMsg);
			return true;
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to publish message.", e);
		}
		
		return false;
	}

	@Override
	public boolean subscribeToTopic(ResourceNameEnum topicName, int qos)
	{
		if (topicName == null) {
			_Logger.warning("Invalid topic name for subscribeToTopic()");
			return false;
		}
		
		if (this.mqttClient == null || !this.mqttClient.isConnected()) {
			_Logger.warning("MQTT client not connected. Cannot subscribe to topic.");
			return false;
		}
		
		try {
			String topic = topicName.getResourceName();
			_Logger.info("Subscribing to topic: " + topic + " with QoS: " + qos);
			this.mqttClient.subscribe(topic, qos);
			return true;
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to subscribe to topic.", e);
		}
		
		return false;
	}

	@Override
	public boolean unsubscribeFromTopic(ResourceNameEnum topicName)
	{
		if (topicName == null) {
			_Logger.warning("Invalid topic name for unsubscribeFromTopic()");
			return false;
		}
		
		if (this.mqttClient == null || !this.mqttClient.isConnected()) {
			_Logger.warning("MQTT client not connected. Cannot unsubscribe from topic.");
			return false;
		}
		
		try {
			String topic = topicName.getResourceName();
			_Logger.info("Unsubscribing from topic: " + topic);
			this.mqttClient.unsubscribe(topic);
			return true;
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to unsubscribe from topic.", e);
		}
		
		return false;
	}

	@Override
	public boolean setConnectionListener(IConnectionListener listener)
	{
		// Note: IConnectionListener may not be used in this exercise
		// Just return true if not null
		if (listener != null) {
			return true;
		}
		return false;
	}
	
	@Override
	public boolean setDataMessageListener(IDataMessageListener listener)
	{
		if (listener != null) {
			this.dataMsgListener = listener;
			return true;
		}
		
		return false;
	}
	
	// callbacks
	
	@Override
	public void connectComplete(boolean reconnect, String serverURI)
	{
		_Logger.info("MQTT connection successful (is reconnect = " + reconnect + "). Broker: " + serverURI);
		
		int qos = 1;
		
		// Subscribe to the CDA topics we need to receive
		this.subscribeToTopic(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE, qos);
		this.subscribeToTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, qos);
		this.subscribeToTopic(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, qos);
	}

	@Override
	public void connectionLost(Throwable t)
	{
		_Logger.log(Level.WARNING, "Lost connection to MQTT broker: " + this.brokerAddr, t);
	}
	
	@Override
	public void deliveryComplete(IMqttDeliveryToken token)
	{
		// TODO: Logging level may need to be adjusted to see output in log file / console
		_Logger.fine("Delivered MQTT message with ID: " + token.getMessageId());
	}
	
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception
	{
		_Logger.info("MQTT message arrived on topic: " + topic + "'");
		
		if (this.dataMsgListener != null) {
			String payload = new String(message.getPayload());
			
			// Determine which resource type based on topic
			ResourceNameEnum resource = ResourceNameEnum.getEnumFromValue(topic);
			
			if (resource != null) {
				try {
					// Handle different message types based on the resource
					if (resource == ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE) {
						ActuatorData actuatorData = DataUtil.getInstance().jsonToActuatorData(payload);
						this.dataMsgListener.handleActuatorCommandResponse(resource, actuatorData);
						
					} else if (resource == ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE) {
						SensorData sensorData = DataUtil.getInstance().jsonToSensorData(payload);
						this.dataMsgListener.handleSensorMessage(resource, sensorData);
						
					} else if (resource == ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE) {
						SystemPerformanceData sysPerfData = DataUtil.getInstance().jsonToSystemPerformanceData(payload);
						this.dataMsgListener.handleSystemPerformanceMessage(resource, sysPerfData);
						
					} else {
						// For any other topics, use the generic handler
						this.dataMsgListener.handleIncomingMessage(resource, payload);
					}
				} catch (Exception e) {
					_Logger.warning("Failed to process message: " + e.getMessage());
				}
			}
		}
	}

	

	

	// private methods
	
	/**
	 * Called by the constructor to set the MQTT client parameters to be used for the connection.
	 * 
	 * @param configSectionName The name of the configuration section to use for
	 * the MQTT client configuration parameters.
	 */
	private void initClientParameters(String configSectionName)
	{
		// TODO: implement this in future exercises
	}
	
	/**
	 * Called by {@link #initClientParameters(String)} to load credentials.
	 * 
	 * @param configSectionName The name of the configuration section to use for
	 * the MQTT client configuration parameters.
	 */
	private void initCredentialConnectionParameters(String configSectionName)
	{
		// TODO: implement this in future exercises for secure connections
	}
	
	/**
	 * Called by {@link #initClientParameters(String)} to enable encryption.
	 * 
	 * @param configSectionName The name of the configuration section to use for
	 * the MQTT client configuration parameters.
	 */
	private void initSecureConnectionParameters(String configSectionName)
	{
		// TODO: implement this in future exercises for TLS/SSL connections
	}

	
	
	
}