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

package programmingtheiot.gda.app;

import java.util.logging.Level;
import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IActuatorDataListener;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;

import programmingtheiot.data.ActuatorData;
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
		
		// Get the DataUtil instance for JSON conversion
		this.dataUtil = DataUtil.getInstance();
		
		initManager();
	}
	
	/**
	 * Parameterized constructor for testing or specific configurations.
	 * 
	 * @param enableMqttClient Enable MQTT client connection
	 * @param enableCoapServer Enable CoAP server
	 * @param enableCloudClient Enable cloud client connection
	 * @param enableSmtpClient Enable SMTP client connection
	 * @param enablePersistenceClient Enable persistence client
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
		
		initManager();
	}
	
	
	// public methods
	
	/**
	 * Handles actuator command responses from connected devices.
	 * 
	 * @param resourceName The resource name associated with the actuator
	 * @param data The ActuatorData response
	 * @return true if handled successfully, false otherwise
	 */
	@Override
	public boolean handleActuatorCommandResponse(ResourceNameEnum resourceName, ActuatorData data)
	{
		if (data != null) {
			_Logger.info("Handling actuator response: " + data.getName());
			
			// Optional: analyze the incoming data
			this.handleIncomingDataAnalysis(resourceName, data);
			
			if (data.hasError()) {
				_Logger.warning("Error flag set for ActuatorData instance: " + data.getName());
			}
			
			return true;
		} else {
			_Logger.warning("Received null ActuatorData response");
			return false;
		}
	}

	/**
	 * Handles actuator command requests to be sent to connected devices.
	 * 
	 * @param resourceName The resource name associated with the actuator
	 * @param data The ActuatorData request
	 * @return true if handled successfully, false otherwise
	 */
	@Override
	public boolean handleActuatorCommandRequest(ResourceNameEnum resourceName, ActuatorData data)
	{
		if (data != null) {
			_Logger.info("Handling actuator request: " + data.getName());
			
			// Forward to actuator data listener if set
			if (this.actuatorDataListener != null) {
				this.actuatorDataListener.onActuatorDataUpdate(data);
			}
			
			// TODO: In Part 03, send to CDA via MQTT or CoAP
			
			return true;
		} else {
			_Logger.warning("Received null ActuatorData request");
			return false;
		}
	}

	/**
	 * Handles generic incoming messages (usually JSON).
	 * 
	 * @param resourceName The resource name associated with the message
	 * @param msg The message string (typically JSON)
	 * @return true if handled successfully, false otherwise
	 */
	@Override
	public boolean handleIncomingMessage(ResourceNameEnum resourceName, String msg)
	{
		if (msg != null) {
			_Logger.info("Handling incoming generic message for resource: " + resourceName);
			_Logger.fine("Message content: " + msg);
			
			// The message is likely JSON representing ActuatorData or SystemStateData
			// TODO: Parse and process the JSON message appropriately
			
			return true;
		} else {
			_Logger.warning("Received null message");
			return false;
		}
	}

	/**
	 * Handles sensor data messages from connected devices.
	 * 
	 * @param resourceName The resource name associated with the sensor
	 * @param data The SensorData message
	 * @return true if handled successfully, false otherwise
	 */
	@Override
	public boolean handleSensorMessage(ResourceNameEnum resourceName, SensorData data)
	{
		if (data != null) {
			_Logger.info("Handling sensor message: " + data.getName() + ", value: " + data.getValue());
			
			if (data.hasError()) {
				_Logger.warning("Error flag set for SensorData instance: " + data.getName());
			}
			
			// Convert to JSON for upstream transmission
			String jsonData = this.dataUtil.sensorDataToJson(data);
			
			if (jsonData != null) {
				_Logger.fine("Converted sensor data to JSON: " + jsonData);
				// TODO: Send to cloud or other upstream services
				this.handleUpstreamTransmission(resourceName, jsonData, 0);
			}
			
			return true;
		} else {
			_Logger.warning("Received null SensorData");
			return false;
		}
	}

	/**
	 * Handles system performance messages from the SystemPerformanceManager.
	 * 
	 * @param resourceName The resource name (typically GDA_SYSTEM_PERF_MSG_RESOURCE)
	 * @param data The SystemPerformanceData message
	 * @return true if handled successfully, false otherwise
	 */
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
				// TODO: Send to cloud or other upstream services
				this.handleUpstreamTransmission(resourceName, jsonData, 0);
			}
			
			return true;
		} else {
			_Logger.warning("Received null SystemPerformanceData");
			return false;
		}
	}
	
	/**
	 * Sets the actuator data listener for handling actuator updates.
	 * 
	 * @param name The name/identifier for the listener
	 * @param listener The IActuatorDataListener implementation
	 */
	public void setActuatorDataListener(String name, IActuatorDataListener listener)
	{
		if (listener != null) {
			this.actuatorDataListener = listener;
			_Logger.info("Actuator data listener set: " + name);
		}
	}
	
	/**
	 * Starts the DeviceDataManager and all enabled connections.
	 */
	public void startManager()
	{
		_Logger.info("Starting DeviceDataManager...");
		
		// Start system performance manager if enabled
		if (this.sysPerfMgr != null) {
			boolean started = this.sysPerfMgr.startManager();
			_Logger.info("SystemPerformanceManager started: " + started);
		}
		
		// Start MQTT client if enabled
		if (this.enableMqttClient && this.mqttClient != null) {
			// TODO: Implement in Lab Module 7
			// boolean connected = this.mqttClient.connectClient();
			// _Logger.info("MQTT client connected: " + connected);
		}
		
		// Start CoAP server if enabled
		if (this.enableCoapServer && this.coapServer != null) {
			// TODO: Implement in Lab Module 8
			// boolean started = this.coapServer.startServer();
			// _Logger.info("CoAP server started: " + started);
		}
		
		// Start cloud client if enabled
		if (this.enableCloudClient && this.cloudClient != null) {
			// TODO: Implement in Lab Module 10
			// boolean connected = this.cloudClient.connectClient();
			// _Logger.info("Cloud client connected: " + connected);
		}
		
		// Start persistence client if enabled
		if (this.enablePersistenceClient && this.persistenceClient != null) {
			// TODO: Implement as optional exercise in Lab Module 5
			// this.persistenceClient.connectClient();
		}
		
		// Start SMTP client if enabled (stateless, so no connection needed)
		if (this.enableSmtpClient && this.smtpClient != null) {
			_Logger.info("SMTP client ready (stateless)");
		}
		
		_Logger.info("DeviceDataManager started successfully");
	}
	
	/**
	 * Stops the DeviceDataManager and all active connections.
	 */
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
			// TODO: Implement in Lab Module 7
			// boolean disconnected = this.mqttClient.disconnectClient();
			// _Logger.info("MQTT client disconnected: " + disconnected);
		}
		
		// Stop CoAP server
		if (this.coapServer != null) {
			// TODO: Implement in Lab Module 8
			// boolean stopped = this.coapServer.stopServer();
			// _Logger.info("CoAP server stopped: " + stopped);
		}
		
		// Disconnect cloud client
		if (this.cloudClient != null) {
			// TODO: Implement in Lab Module 10
			// boolean disconnected = this.cloudClient.disconnectClient();
			// _Logger.info("Cloud client disconnected: " + disconnected);
		}
		
		// Disconnect persistence client
		if (this.persistenceClient != null) {
			// TODO: Implement as optional exercise in Lab Module 5
			// this.persistenceClient.disconnectClient();
		}
		
		_Logger.info("DeviceDataManager stopped successfully");
	}

	
	// private methods
	
	/**
	 * Initializes the manager and creates instances of enabled connections.
	 * This will NOT start them, but only create the instances.
	 */
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
			_Logger.info("MQTT client enabled - will initialize in Lab Module 7");
			// TODO: Implement in Lab Module 7
			// this.mqttClient = new MqttClientConnector();
		}
		
		// Initialize CoAP server if enabled
		if (this.enableCoapServer) {
			_Logger.info("CoAP server enabled - will initialize in Lab Module 8");
			// TODO: Implement in Lab Module 8
			// this.coapServer = new CoapServerGateway();
		}
		
		// Initialize cloud client if enabled
		if (this.enableCloudClient) {
			_Logger.info("Cloud client enabled - will initialize in Lab Module 10");
			// TODO: Implement in Lab Module 10
			// this.cloudClient = new CloudClientConnector();
		}
		
		// Initialize persistence client if enabled
		if (this.enablePersistenceClient) {
			_Logger.info("Persistence client enabled - optional exercise in Lab Module 5");
			// TODO: Implement as optional exercise in Lab Module 5
			// this.persistenceClient = new RedisPersistenceAdapter();
		}
		
		// Initialize SMTP client if enabled
		if (this.enableSmtpClient) {
			_Logger.info("SMTP client enabled - will initialize when needed");
			// TODO: Implement when needed
			// this.smtpClient = new SmtpClientConnector();
		}
		
		_Logger.info("DeviceDataManager initialization complete");
	}
	
	/**
	 * Handles analysis of incoming actuator data.
	 * Will eventually publish back to CDA using MQTT or CoAP.
	 * 
	 * @param resourceName The resource name associated with the data
	 * @param data The ActuatorData to analyze
	 */
	private void handleIncomingDataAnalysis(ResourceNameEnum resourceName, ActuatorData data)
	{
		_Logger.fine("Analyzing incoming actuator data: " + data.getName());
		
		// TODO: In Part 03, implement logic to publish back to CDA
		// This might include:
		// - Validation of actuator commands
		// - Checking against business rules
		// - Forwarding to appropriate actuator controllers
	}
	
	/**
	 * Handles analysis of incoming system state data.
	 * This is for GDA internal command processing.
	 * 
	 * @param resourceName The resource name associated with the data
	 * @param data The SystemStateData to analyze
	 */
	private void handleIncomingDataAnalysis(ResourceNameEnum resourceName, SystemStateData data)
	{
		_Logger.fine("Analyzing incoming system state data");
		
		// TODO: Implement internal command handling
		// This might include:
		// - System configuration changes
		// - Mode changes (e.g., switching between local and cloud control)
		// - Diagnostic commands
	}
	
	/**
	 * Handles upstream transmission of data to cloud or other services.
	 * 
	 * @param resourceName The resource name associated with the data
	 * @param jsonData The JSON data to transmit
	 * @param qos Quality of Service level for transmission
	 * @return true if transmission successful, false otherwise
	 */
	private boolean handleUpstreamTransmission(ResourceNameEnum resourceName, String jsonData, int qos)
	{
		_Logger.fine("Processing upstream transmission for resource: " + resourceName);
		
		// TODO: In Part 03, implement actual transmission logic
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