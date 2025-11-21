/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 */

package programmingtheiot.gda.app;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IActuatorDataListener;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;

import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.BaseIotData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;
import programmingtheiot.data.SystemStateData;

import programmingtheiot.gda.connection.CloudClientConnector;
import programmingtheiot.gda.connection.CoapServerGateway;
import programmingtheiot.gda.connection.IPersistenceClient;
import programmingtheiot.gda.connection.IPubSubClient;
import programmingtheiot.gda.connection.IRequestResponseClient;
import programmingtheiot.gda.connection.MqttClientConnector;
import programmingtheiot.gda.connection.RedisPersistenceAdapter;
import programmingtheiot.gda.connection.SmtpClientConnector;

import programmingtheiot.gda.system.SystemPerformanceManager;

/**
 * DeviceDataManager coordinates all data processing within the GDA application.
 * It manages connections to various services and handles incoming/outgoing data messages.
 */
public class DeviceDataManager implements IDataMessageListener
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(DeviceDataManager.class.getName());
	
	// private var's
	
	private boolean enableMqttClient = true;
	private boolean enableCoapServer = false;
	private boolean enableCloudClient = false;
	private boolean enableSmtpClient = false;
	private boolean enablePersistenceClient = false;
	private boolean enableSystemPerf = false;
	
	private IActuatorDataListener actuatorDataListener = null;
	private IPubSubClient mqttClient = null;
	private IPubSubClient cloudClient = null;
	private IPersistenceClient persistenceClient = null;
	private IRequestResponseClient smtpClient = null;
	private CoapServerGateway coapServer = null;
	private SystemPerformanceManager sysPerfMgr = null;
	
	private DataUtil dataUtil = null;
	
	// Humidity threshold crossing properties
	private ActuatorData   latestHumidifierActuatorData = null;
	private ActuatorData   latestHumidifierActuatorResponse = null;
	private SensorData     latestHumiditySensorData = null;
	private OffsetDateTime latestHumiditySensorTimeStamp = null;
	
	private boolean handleHumidityChangeOnDevice = false;
	private int     lastKnownHumidifierCommand   = ConfigConst.OFF_COMMAND;
	
	// Threshold configuration values
	private long    humidityMaxTimePastThreshold = 300; // seconds
	private float   nominalHumiditySetting   = 40.0f;
	private float   triggerHumidifierFloor   = 30.0f;
	private float   triggerHumidifierCeiling = 50.0f;
	
	// constructors
	
	/**
	 * Default constructor.
	 * Reads configuration settings and initializes connections.
	 */
	public DeviceDataManager()
	{
		super();
		
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		this.enableMqttClient =
			configUtil.getBoolean(
				ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_MQTT_CLIENT_KEY);
		
		this.enableCoapServer =
			configUtil.getBoolean(
				ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_COAP_SERVER_KEY);
		
		this.enableCloudClient =
			configUtil.getBoolean(
				ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_CLOUD_CLIENT_KEY);
		
		this.enableSmtpClient =
			configUtil.getBoolean(
				ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_SMTP_CLIENT_KEY);
		
		this.enablePersistenceClient =
			configUtil.getBoolean(
				ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_PERSISTENCE_CLIENT_KEY);
		
		// Parse humidity threshold crossing configuration
		this.handleHumidityChangeOnDevice =
			configUtil.getBoolean(
				ConfigConst.GATEWAY_DEVICE, "handleHumidityChangeOnDevice");
		
		this.humidityMaxTimePastThreshold =
			configUtil.getInteger(
				ConfigConst.GATEWAY_DEVICE, "humidityMaxTimePastThreshold");
		
		this.nominalHumiditySetting =
			configUtil.getFloat(
				ConfigConst.GATEWAY_DEVICE, "nominalHumiditySetting");
		
		this.triggerHumidifierFloor =
			configUtil.getFloat(
				ConfigConst.GATEWAY_DEVICE, "triggerHumidifierFloor");
		
		this.triggerHumidifierCeiling =
			configUtil.getFloat(
				ConfigConst.GATEWAY_DEVICE, "triggerHumidifierCeiling");
		
		// Basic validation for timing
		if (this.humidityMaxTimePastThreshold < 10 || this.humidityMaxTimePastThreshold > 7200) {
			this.humidityMaxTimePastThreshold = 300;
		}
		
		// Get the DataUtil instance for JSON conversion
		this.dataUtil = DataUtil.getInstance();
		
		initManager();
	}
	
	/**
	 * Parameterized constructor for testing or specific configurations.
	 */
	public DeviceDataManager(
		boolean enableMqttClient,
		boolean enableCoapServer,
		boolean enableCloudClient,
		boolean enableSmtpClient,
		boolean enablePersistenceClient)
	{
		super();
		
		this.enableMqttClient = enableMqttClient;
		this.enableCoapServer = enableCoapServer;
		this.enableCloudClient = enableCloudClient;
		this.enableSmtpClient = enableSmtpClient;
		this.enablePersistenceClient = enablePersistenceClient;
		
		this.dataUtil = DataUtil.getInstance();
		
		// Load humidity configuration even for parameterized constructor
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		this.handleHumidityChangeOnDevice =
			configUtil.getBoolean(
				ConfigConst.GATEWAY_DEVICE, "handleHumidityChangeOnDevice");
		
		this.humidityMaxTimePastThreshold =
			configUtil.getInteger(
				ConfigConst.GATEWAY_DEVICE, "humidityMaxTimePastThreshold");
		
		this.nominalHumiditySetting =
			configUtil.getFloat(
				ConfigConst.GATEWAY_DEVICE, "nominalHumiditySetting");
		
		this.triggerHumidifierFloor =
			configUtil.getFloat(
				ConfigConst.GATEWAY_DEVICE, "triggerHumidifierFloor");
		
		this.triggerHumidifierCeiling =
			configUtil.getFloat(
				ConfigConst.GATEWAY_DEVICE, "triggerHumidifierCeiling");
		
		if (this.humidityMaxTimePastThreshold < 10 || this.humidityMaxTimePastThreshold > 7200) {
			this.humidityMaxTimePastThreshold = 300;
		}
		
		initManager();
	}
	
	// public methods
	
	@Override
	public boolean handleActuatorCommandResponse(ResourceNameEnum resourceName, ActuatorData data)
	{
		if (data != null) {
			_Logger.info("Handling actuator response: " + data.getName());
			
			// Store response if it's for the humidifier
			if (data.getTypeID() == ConfigConst.HUMIDIFIER_ACTUATOR_TYPE) {
				this.latestHumidifierActuatorResponse = data;
				_Logger.info("Stored humidifier actuator response with command: " + data.getCommand());
			}
			
			// Optional: analyze the incoming data
			this.handleIncomingDataAnalysis(resourceName, data);
			
			if (data.hasError()) {
				_Logger.warning("Error flag set for ActuatorData instance: " + data.getName());
			}
			
			// Store in persistence if enabled
			if (this.enablePersistenceClient && this.persistenceClient != null) {
				// TODO: Implement persistence storage
				// this.persistenceClient.storeData(resourceName.getResourceName(), ConfigConst.DEFAULT_QOS, data);
			}
			
			return true;
		} else {
			_Logger.warning("Received null ActuatorData response");
			return false;
		}
	}

	@Override
	public boolean handleActuatorCommandRequest(ResourceNameEnum resourceName, ActuatorData data)
	{
		if (data != null) {
			_Logger.info("Handling actuator request: " + data.getName());
			
			// Forward to actuator data listener if set (for CoAP)
			if (this.actuatorDataListener != null) {
				this.actuatorDataListener.onActuatorDataUpdate(data);
			}
			
			// Send via MQTT if enabled
			if (this.enableMqttClient && this.mqttClient != null) {
				String jsonData = this.dataUtil.actuatorDataToJson(data);
				
				if (this.mqttClient.publishMessage(resourceName, jsonData, ConfigConst.DEFAULT_QOS)) {
					_Logger.info("Published ActuatorData command to CDA via MQTT: " + data.getCommand());
				} else {
					_Logger.warning("Failed to publish ActuatorData command to CDA: " + data.getCommand());
				}
			}
			
			return true;
		} else {
			_Logger.warning("Received null ActuatorData request");
			return false;
		}
	}

	@Override
	public boolean handleIncomingMessage(ResourceNameEnum resourceName, String msg)
	{
		if (msg != null) {
			_Logger.info("Handling incoming generic message for resource: " + resourceName);
			_Logger.fine("Message content: " + msg);
			
			// Determine the content type and convert accordingly
			try {
				// Try to determine what type of data this is based on the resource name
				if (resourceName == ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE) {
					SensorData sensorData = this.dataUtil.jsonToSensorData(msg);
					return handleSensorMessage(resourceName, sensorData);
				} else if (resourceName == ResourceNameEnum.CDA_ACTUATOR_RESPONSE_RESOURCE) {
					ActuatorData actuatorData = this.dataUtil.jsonToActuatorData(msg);
					return handleActuatorCommandResponse(resourceName, actuatorData);
				} else if (resourceName == ResourceNameEnum.CDA_SYSTEM_PERF_MSG_RESOURCE) {
					SystemPerformanceData perfData = this.dataUtil.jsonToSystemPerformanceData(msg);
					return handleSystemPerformanceMessage(resourceName, perfData);
				}
			} catch (Exception e) {
				_Logger.warning("Failed to parse incoming message: " + e.getMessage());
			}
			
			return true;
		} else {
			_Logger.warning("Received null message");
			return false;
		}
	}

	@Override
	public boolean handleSensorMessage(ResourceNameEnum resourceName, SensorData data)
	{
		if (data != null) {
			_Logger.fine("Handling sensor message: " + data.getName());
			
			if (data.hasError()) {
				_Logger.warning("Error flag set for SensorData instance.");
			}
			
			String jsonData = DataUtil.getInstance().sensorDataToJson(data);
			
			_Logger.fine("JSON [SensorData] -> " + jsonData);
			
			// TODO: retrieve this from config file
			int qos = ConfigConst.DEFAULT_QOS;
			
			// Store in persistence if enabled
			if (this.enablePersistenceClient && this.persistenceClient != null) {
				// TODO: Implement persistence storage
				// this.persistenceClient.storeData(resourceName.getResourceName(), qos, data);
			}
			
			// Analyze the incoming sensor data for threshold crossings
			this.handleIncomingDataAnalysis(resourceName, data);
			
			// Handle upstream transmission
			this.handleUpstreamTransmission(resourceName, jsonData, qos);
			
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean handleSystemPerformanceMessage(ResourceNameEnum resourceName, SystemPerformanceData data)
	{
		if (data != null) {
			_Logger.info("Handling system performance message: " + data.getName());
			_Logger.info(String.format("System Performance - CPU: %.2f%%, Memory: %.2f%%, Disk: %.2f%%",
				data.getCpuUtilization(), 
				data.getMemoryUtilization(),
				data.getDiskUtilization()));
			
			if (data.hasError()) {
				_Logger.warning("Error flag set for SystemPerformanceData instance");
			}
			
			// Convert to JSON for upstream transmission
			String jsonData = this.dataUtil.systemPerformanceDataToJson(data);
			
			if (jsonData != null) {
				_Logger.fine("Converted system performance data to JSON: " + jsonData);
				
				// Store in persistence if enabled
				if (this.enablePersistenceClient && this.persistenceClient != null) {
					// TODO: Implement persistence storage
					// this.persistenceClient.storeData(resourceName.getResourceName(), ConfigConst.DEFAULT_QOS, data);
				}
				
				// Handle upstream transmission
				this.handleUpstreamTransmission(resourceName, jsonData, ConfigConst.DEFAULT_QOS);
			}
			
			return true;
		} else {
			_Logger.warning("Received null SystemPerformanceData");
			return false;
		}
	}
	
	public void setActuatorDataListener(String name, IActuatorDataListener listener)
	{
		if (listener != null) {
			this.actuatorDataListener = listener;
			_Logger.info("Actuator data listener set: " + name);
		}
	}
	
	public void startManager()
	{
		_Logger.info("Starting DeviceDataManager...");
		
		// Start system performance manager if enabled
		if (this.sysPerfMgr != null) {
			boolean started = this.sysPerfMgr.startManager();
			_Logger.info("SystemPerformanceManager started: " + started);
		}
		
		// Start MQTT client if enabled
		if (this.mqttClient != null) {
			if (this.mqttClient.connectClient()) {
				_Logger.info("Successfully connected MQTT client to broker.");
				// Subscriptions handled in MqttClientConnector.connectComplete()
			} else {
				_Logger.severe("Failed to connect MQTT client to broker.");
			}
		}
		
		// Start CoAP server if enabled
		if (this.enableCoapServer && this.coapServer != null) {
			// TODO: Implement in Lab Module 8
		}
		
		// Start cloud client if enabled
		if (this.enableCloudClient && this.cloudClient != null) {
			// TODO: Implement in Lab Module 10
		}
		
		// Start persistence client if enabled
		if (this.enablePersistenceClient && this.persistenceClient != null) {
			// TODO: Implement as optional exercise in Lab Module 5
		}
		
		// Start SMTP client if enabled (stateless, so no connection needed)
		if (this.enableSmtpClient && this.smtpClient != null) {
			_Logger.info("SMTP client ready (stateless)");
		}
		
		_Logger.info("DeviceDataManager started successfully");
	}
	
	public void stopManager()
	{
		_Logger.info("Stopping DeviceDataManager...");
		
		// Stop system performance manager
		if (this.sysPerfMgr != null) {
			boolean stopped = this.sysPerfMgr.stopManager();
			_Logger.info("SystemPerformanceManager stopped: " + stopped);
		}
		
		// Disconnect MQTT client
		if (this.mqttClient != null) {
			if (this.mqttClient.disconnectClient()) {
				_Logger.info("Successfully disconnected MQTT client from broker.");
			} else {
				_Logger.severe("Failed to disconnect MQTT client from broker.");
			}
		}
		
		// Stop CoAP server
		if (this.coapServer != null) {
			// TODO: Implement in Lab Module 8
		}
		
		// Disconnect cloud client
		if (this.cloudClient != null) {
			// TODO: Implement in Lab Module 10
		}
		
		// Disconnect persistence client
		if (this.persistenceClient != null) {
			// TODO: Implement as optional exercise in Lab Module 5
		}
		
		_Logger.info("DeviceDataManager stopped successfully");
	}

	// private methods
	
	private void initManager()
	{
		_Logger.info("Initializing DeviceDataManager...");
		
		ConfigUtil configUtil = ConfigUtil.getInstance();
		
		// Check if system performance monitoring is enabled
		this.enableSystemPerf =
			configUtil.getBoolean(ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_SYSTEM_PERF_KEY);
		
		// Initialize system performance manager if enabled
		if (this.enableSystemPerf) {
			_Logger.info("Initializing SystemPerformanceManager...");
			this.sysPerfMgr = new SystemPerformanceManager();
			this.sysPerfMgr.setDataMessageListener(this);
		}
		
		// Initialize MQTT client if enabled
		if (this.enableMqttClient) {
			_Logger.info("Initializing MQTT client...");
			this.mqttClient = new MqttClientConnector();
			this.mqttClient.setDataMessageListener(this);
		}
		
		// Initialize CoAP server if enabled
		if (this.enableCoapServer) {
			_Logger.info("CoAP server enabled - will initialize in Lab Module 8");
			// TODO: Implement in Lab Module 8
		}
		
		// Initialize cloud client if enabled
		if (this.enableCloudClient) {
			_Logger.info("Cloud client enabled - will initialize in Lab Module 10");
			// TODO: Implement in Lab Module 10
		}
		
		// Initialize persistence client if enabled
		if (this.enablePersistenceClient) {
			_Logger.info("Persistence client enabled - optional exercise in Lab Module 5");
			// TODO: Implement as optional exercise in Lab Module 5
		}
		
		// Initialize SMTP client if enabled
		if (this.enableSmtpClient) {
			_Logger.info("SMTP client enabled - will initialize when needed");
			// TODO: Implement when needed
		}
		
		_Logger.info("DeviceDataManager initialization complete");
	}
	
	/**
	 * Handles analysis of incoming actuator data.
	 */
	private void handleIncomingDataAnalysis(ResourceNameEnum resourceName, ActuatorData data)
	{
		_Logger.fine("Analyzing incoming actuator data: " + data.getName());
		// For now, this is empty. Can be expanded for actuator response validation
	}
	
	/**
	 * Handles analysis of incoming system state data.
	 */
	private void handleIncomingDataAnalysis(ResourceNameEnum resourceName, SystemStateData data)
	{
		_Logger.info("Analyzing incoming system state data");
		// For now, this is empty. Can be expanded for system command handling
	}
	
	/**
	 * Handles analysis of incoming sensor data for threshold crossings.
	 */
	private void handleIncomingDataAnalysis(ResourceNameEnum resource, SensorData data)
	{
		_Logger.fine("Analyzing incoming sensor data");
		
		// Check either resource or SensorData for type
		if (data.getTypeID() == ConfigConst.HUMIDITY_SENSOR_TYPE) {
			if (this.handleHumidityChangeOnDevice) {
				handleHumiditySensorAnalysis(resource, data);
			}
		}
	}
	
	/**
	 * Analyzes humidity sensor data for threshold crossings and triggers actuator events.
	 */
	private void handleHumiditySensorAnalysis(ResourceNameEnum resource, SensorData data)
	{
		_Logger.fine("Analyzing humidity data from CDA: " + data.getLocationID() + ". Value: " + data.getValue());
		
		boolean isLow  = data.getValue() < this.triggerHumidifierFloor;
		boolean isHigh = data.getValue() > this.triggerHumidifierCeiling;
		
		if (isLow || isHigh) {
			_Logger.fine("Humidity data from CDA exceeds nominal range.");
			
			if (this.latestHumiditySensorData == null) {
				// Set properties then exit - nothing more to do until the next sample
				this.latestHumiditySensorData = data;
				this.latestHumiditySensorTimeStamp = getDateTimeFromData(data);
				
				_Logger.fine(
					"Starting humidity nominal exception timer. Waiting for seconds: " +
					this.humidityMaxTimePastThreshold);
				
				return;
			} else {
				OffsetDateTime curHumiditySensorTimeStamp = getDateTimeFromData(data);
				
				long diffSeconds =
					ChronoUnit.SECONDS.between(
						this.latestHumiditySensorTimeStamp, curHumiditySensorTimeStamp);
				
				_Logger.fine("Checking Humidity value exception time delta: " + diffSeconds);
				
				if (diffSeconds >= this.humidityMaxTimePastThreshold) {
					ActuatorData ad = new ActuatorData();
					ad.setName(ConfigConst.HUMIDIFIER_ACTUATOR_NAME);
					ad.setLocationID(data.getLocationID());
					ad.setTypeID(ConfigConst.HUMIDIFIER_ACTUATOR_TYPE);
					ad.setValue(this.nominalHumiditySetting);
					
					if (isLow) {
						ad.setCommand(ConfigConst.ON_COMMAND);
					} else if (isHigh) {
						ad.setCommand(ConfigConst.OFF_COMMAND);
					}
					
					_Logger.info(
						"Humidity exceptional value reached. Sending actuation event to CDA: " +
						ad);
					
					this.lastKnownHumidifierCommand = ad.getCommand();
					sendActuatorCommandtoCda(ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, ad);
					
					// Set ActuatorData and reset SensorData (and timestamp)
					this.latestHumidifierActuatorData = ad;
					this.latestHumiditySensorData = null;
					this.latestHumiditySensorTimeStamp = null;
				}
			}
		} else if (this.lastKnownHumidifierCommand == ConfigConst.ON_COMMAND) {
			// Check if we need to turn off the humidifier
			if (this.latestHumidifierActuatorData != null) {
				// Check the value - if the humidifier is on, but not yet at nominal, keep it on
				if (data.getValue() >= this.nominalHumiditySetting) {
					this.latestHumidifierActuatorData.setCommand(ConfigConst.OFF_COMMAND);
					
					_Logger.info(
						"Humidity nominal value reached. Sending OFF actuation event to CDA: " +
						this.latestHumidifierActuatorData);
					
					sendActuatorCommandtoCda(
						ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, this.latestHumidifierActuatorData);
					
					// Reset ActuatorData and SensorData (and timestamp)
					this.lastKnownHumidifierCommand = this.latestHumidifierActuatorData.getCommand();
					this.latestHumidifierActuatorData = null;
					this.latestHumiditySensorData = null;
					this.latestHumiditySensorTimeStamp = null;
				} else {
					_Logger.fine("Humidifier is still on. Not yet at nominal levels (OK).");
				}
			} else {
				// Shouldn't happen, unless some other logic nullifies the class-scoped ActuatorData instance
				_Logger.warning(
					"ERROR: ActuatorData for humidifier is null (shouldn't be). Can't send command.");
			}
		}
	}
	
	/**
	 * Sends actuator command to the CDA via MQTT or CoAP.
	 */
	private void sendActuatorCommandtoCda(ResourceNameEnum resource, ActuatorData data)
	{
		// Handle CoAP server case (when GDA is server and CDA is observing client)
		if (this.actuatorDataListener != null) {
			// Check for null reference to avoid NPE
			try {
				this.actuatorDataListener.onActuatorDataUpdate(data);
			} catch (NullPointerException e) {
				_Logger.fine("ActuatorDataListener not fully initialized yet (OK for testing)");
			}
		}
		
		// Handle MQTT case
		if (this.enableMqttClient && this.mqttClient != null) {
			String jsonData = DataUtil.getInstance().actuatorDataToJson(data);
			
			if (this.mqttClient.publishMessage(resource, jsonData, ConfigConst.DEFAULT_QOS)) {
				_Logger.info(
					"Published ActuatorData command from GDA to CDA: " + data.getCommand());
			} else {
				_Logger.warning(
					"Failed to publish ActuatorData command from GDA to CDA: " + data.getCommand());
			}
		}
	}
	
	/**
	 * Extracts OffsetDateTime from BaseIotData timestamp.
	 */
	private OffsetDateTime getDateTimeFromData(BaseIotData data)
	{
		OffsetDateTime odt = null;
		
		try {
			odt = OffsetDateTime.parse(data.getTimeStamp());
		} catch (Exception e) {
			_Logger.warning(
				"Failed to extract ISO 8601 timestamp from IoT data. Using local current time.");
			
			// This won't be accurate, but should be reasonably close
			odt = OffsetDateTime.now();
		}
		
		return odt;
	}
	
	/**
	 * Handles upstream transmission of data to cloud or other services.
	 */
	private boolean handleUpstreamTransmission(ResourceNameEnum resourceName, String jsonData, int qos)
	{
		_Logger.info("TODO: Send JSON data to cloud service: " + resourceName);
		
		// TODO: In Part 04, implement actual transmission logic
		// This will eventually:
		// - Send data to cloud service via cloudClient
		// - Store data in persistence layer if enabled
		// - Send alerts via SMTP if thresholds are exceeded
		
		// For now, just log the data
		if (jsonData != null && jsonData.length() > 0) {
			_Logger.fine("Data ready for upstream transmission: " + 
				jsonData.substring(0, Math.min(jsonData.length(), 100)) + "...");
			return true;
		}
		
		return false;
	}
}