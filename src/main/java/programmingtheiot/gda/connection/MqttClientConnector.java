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
import java.security.cert.Certificate;
import java.util.Collection;
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
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;

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
	

	private IConnectionListener connListener = null;
	private boolean useCloudGatewayConfig = false;
	// constructors
	
	/**
	 * Default.
	 * 
		*/
	public MqttClientConnector()
	{
		this(false);
	}

	public MqttClientConnector(boolean useCloudGatewayConfig)
	{
		this(useCloudGatewayConfig ? ConfigConst.CLOUD_GATEWAY_SERVICE : null);
	}

	public MqttClientConnector(String cloudGatewayConfigSectionName)
	{
		super();
		
		if (cloudGatewayConfigSectionName != null && cloudGatewayConfigSectionName.trim().length() > 0) {
			this.useCloudGatewayConfig = true;
			initClientParameters(cloudGatewayConfigSectionName);
		} else {
			this.useCloudGatewayConfig = false;
			initClientParameters(ConfigConst.MQTT_GATEWAY_SERVICE);
		}
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
		
		
	public boolean setConnectionListener(IConnectionListener listener)
	{
		if (listener != null) {
			_Logger.info("Setting connection listener.");
			this.connListener = listener;
			return true;
		} else {
			_Logger.warning("No connection listener specified. Ignoring.");
		}
		
		return false;
	}



	protected boolean publishMessage(String topicName, byte[] payload, int qos)
	{
		if (topicName == null) {
			_Logger.warning("Resource is null. Unable to publish message: " + this.brokerAddr);
			
			return false;
		}
		
		if (payload == null || payload.length == 0) {
			_Logger.warning("Message is null or empty. Unable to publish message: " + this.brokerAddr);
			
			return false;
		}
		
		if (qos < 0 || qos > 2) {
			_Logger.warning("Invalid QoS. Using default. QoS requested: " + qos);
			
			// TODO: retrieve default QoS from config file
			qos = ConfigConst.DEFAULT_QOS;
		}
		
		try {
			MqttMessage mqttMsg = new MqttMessage();
			mqttMsg.setQos(qos);
			mqttMsg.setPayload(payload);
			
			this.mqttClient.publish(topicName, mqttMsg);
			
			return true;
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to publish message to topic: " + topicName, e);
		}
		
		return false;
	}


	protected boolean subscribeToTopic(String topicName, int qos)
{
	return subscribeToTopic(topicName, qos, null);
}

protected boolean subscribeToTopic(String topicName, int qos, IMqttMessageListener listener)
{
	// NOTE: This is the preferred method for subscribing to a given topic,
	// as it allows the use of an IMqttMessageListener to be defined and
	// registered as the handler for incoming messages pertaining to the
	// given topic 'topicName'.

	if (topicName == null) {
		_Logger.warning("Resource is null. Unable to subscribe to topic: " + this.brokerAddr);
		
		return false;
	}
	
	if (qos < 0 || qos > 2) {
		_Logger.warning("Invalid QoS. Using default. QoS requested: " + qos);
		
		// TODO: retrieve default QoS from config file
		qos = ConfigConst.DEFAULT_QOS;
	}
	
	try {
		if (listener != null) {
			this.mqttClient.subscribe(topicName, qos, listener);
			
			_Logger.info("Successfully subscribed to topic with listener: " + topicName);
		} else {
			this.mqttClient.subscribe(topicName, qos);
			
			_Logger.info("Successfully subscribed to topic: " + topicName);
		}
		
		return true;
	} catch (Exception e) {
		_Logger.log(Level.SEVERE, "Failed to subscribe to topic: " + topicName, e);
	}
	
	return false;
}


protected boolean unsubscribeFromTopic(String topicName)
{
	if (topicName == null) {
		_Logger.warning("Resource is null. Unable to unsubscribe from topic: " + this.brokerAddr);
		
		return false;
	}
	
	try {
		this.mqttClient.unsubscribe(topicName);
		
		_Logger.info("Successfully unsubscribed from topic: " + topicName);
		
		return true;
	} catch (Exception e) {
		_Logger.log(Level.SEVERE, "Failed to unsubscribe from topic: " + topicName, e);
	}
	
	return false;
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
    if (topicName == null) {
        _Logger.warning("Resource is null. Unable to publish message: " + this.brokerAddr);
        return false;
    }
    
    if (msg == null || msg.length() == 0) {
        _Logger.warning("Message is null or empty. Unable to publish message: " + this.brokerAddr);
        return false;
    }
    
    // Just call the protected method!
    return publishMessage(topicName.getResourceName(), msg.getBytes(), qos);
}



@Override
public boolean subscribeToTopic(ResourceNameEnum topicName, int qos)
{
	if (topicName == null) {
		_Logger.warning("Resource is null. Unable to subscribe to topic: " + this.brokerAddr);
		
		return false;
	}
	
	return subscribeToTopic(topicName.getResourceName(), qos);
}

@Override
public boolean unsubscribeFromTopic(ResourceNameEnum topicName)
{
	if (topicName == null) {
		_Logger.warning("Resource is null. Unable to unsubscribe from topic: " + this.brokerAddr);
		
		return false;
	}
	
	return unsubscribeFromTopic(topicName.getResourceName());
}

	/*@Override
	public boolean setConnectionListener(IConnectionListener listener)
	{
		// Note: IConnectionListener may not be used in this exercise
		// Just return true if not null
		if (listener != null) {
			return true;
		}
		return false;
	}*/
	
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
		
		// Option 1
		if (! this.useCloudGatewayConfig) {
			this.subscribeToTopic(ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE, qos);
			this.subscribeToTopic(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, qos);
			this.subscribeToTopic(ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE, qos);
			
			// IMPORTANT NOTE: You'll have to parse each message type in the callback method
			// `public void messageArrived(String topic, MqttMessage msg) throws Exception`
		}
		
		// This call enables the MqttClientConnector to notify another listener
		// about the connection now being complete. This will be important for
		// the CloudClientConnector implementation, is it needs to know when
		// this client is finally connected with the cloud-hosted MQTT broker.
		if (this.connListener != null) {
			this.connListener.onConnect();
		}
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
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		this.host = configUtil.getProperty(
			configSectionName, ConfigConst.HOST_KEY, ConfigConst.DEFAULT_HOST);
		
		this.brokerKeepAlive = configUtil.getInteger(
			configSectionName, ConfigConst.KEEP_ALIVE_KEY, ConfigConst.DEFAULT_KEEP_ALIVE);
		
		this.useAsyncClient = configUtil.getBoolean(
			configSectionName, ConfigConst.USE_ASYNC_CLIENT_KEY);
		
		this.clientID = MqttClient.generateClientId();
		
		// Initialize these FIRST before trying to use them
		this.persistence = new MemoryPersistence();
		this.connOpts = new MqttConnectOptions();
		this.connOpts.setKeepAliveInterval(this.brokerKeepAlive);
		this.connOpts.setCleanSession(false);
		this.connOpts.setAutomaticReconnect(true);
		
		// Check for encryption
		boolean enableCrypt = configUtil.getBoolean(
			configSectionName, ConfigConst.ENABLE_CRYPT_KEY);
		
		if (enableCrypt) {
			initSecureConnectionParameters(configSectionName);
		} else {
			this.port = configUtil.getInteger(
				configSectionName, ConfigConst.PORT_KEY, ConfigConst.DEFAULT_MQTT_PORT);
			this.protocol = ConfigConst.DEFAULT_MQTT_PROTOCOL;
		}
		
		// Check for credentials
		String credFile = configUtil.getProperty(
			configSectionName, ConfigConst.CRED_FILE_KEY);
		if (credFile != null && !credFile.isEmpty()) {
			initCredentialConnectionParameters(configSectionName);
		}
		
		// Build broker address with correct protocol and port
		this.brokerAddr = this.protocol + "://" + this.host + ":" + this.port;
		_Logger.info("MQTT broker address: " + this.brokerAddr);
	}
	
	/**
	 * Called by {@link #initClientParameters(String)} to load credentials.
	 * 
	 * @param configSectionName The name of the configuration section to use for
	 * the MQTT client configuration parameters.
	 */
private void initSecureConnectionParameters(String configSectionName)
{
    ConfigUtil configUtil = ConfigUtil.getInstance();
    
    this.port = configUtil.getInteger(
        configSectionName, ConfigConst.SECURE_PORT_KEY, 8883);
    this.protocol = "ssl";
    
    String certFile = configUtil.getProperty(
        configSectionName, ConfigConst.CERT_FILE_KEY);
    
    try {
        if (this.useCloudGatewayConfig && certFile != null) {
            // Load the Ubidots certificate properly
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            FileInputStream fis = new FileInputStream(certFile);
            
            // Load all certificates from the chain
            Collection<? extends Certificate> certs = cf.generateCertificates(fis);
            fis.close();
            
            // Create KeyStore with all certificates
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            
            int i = 0;
            for (Certificate cert : certs) {
                keyStore.setCertificateEntry("ubidots-" + i++, cert);
            }
            
            // Create TrustManager
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);
            
            // Create SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
            
            this.connOpts.setSocketFactory(sslContext.getSocketFactory());
            _Logger.info("TLS enabled with Ubidots certificate chain");
        } else {
            // Your existing local broker code
            SSLSocketFactory sslSocketFactory = createSocketFactory(certFile);
            this.connOpts.setSocketFactory(sslSocketFactory);
            _Logger.info("TLS enabled for MQTT connection on port " + this.port);
        }
    } catch (Exception e) {
        _Logger.log(Level.SEVERE, "Failed to setup TLS", e);
        throw new RuntimeException("TLS setup failed", e);
    }
}

		
	private void initCredentialConnectionParameters(String configSectionName)
	{
		ConfigUtil configUtil = ConfigUtil.getInstance();
		String credFile = configUtil.getProperty(configSectionName, ConfigConst.CRED_FILE_KEY);
		
		if (credFile != null && !credFile.isEmpty()) {
			try {
				_Logger.info("Loading credentials from: " + credFile);
				
				// Read the token from your file
				java.util.Properties props = new java.util.Properties();
				props.load(new FileInputStream(credFile));
				String apiToken = props.getProperty("apitoken");
				
				if (apiToken != null) {
					// For Ubidots: token goes as username, password can be empty
					this.connOpts.setUserName(apiToken.trim());
					this.connOpts.setPassword("".toCharArray());
					_Logger.info("Credentials loaded successfully");
				}
			} catch (Exception e) {
				_Logger.log(Level.WARNING, "Failed to load credentials from: " + credFile, e);
			}
		}
	}
		
	
}