package programmingtheiot.gda.connection;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;

public class CloudClientConnector implements ICloudClient, IConnectionListener
{
    private static final Logger _Logger =
        Logger.getLogger(CloudClientConnector.class.getName());
    
    private MqttClientConnector mqttClient = null;
    private IDataMessageListener dataMsgListener = null;
    private String topicPrefix = null;
    private int qos = ConfigConst.DEFAULT_QOS;
    private String deviceID = null;
    
    // Inner class for handling LED actuation messages from cloud
    private class LedEnablementMessageListener implements IMqttMessageListener
    {
        private IDataMessageListener dataMsgListener = null;
        private ResourceNameEnum resource = ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE;
        private int typeID = ConfigConst.LED_ACTUATOR_TYPE;
        private String itemName = ConfigConst.LED_ACTUATOR_NAME;
        
        LedEnablementMessageListener(IDataMessageListener dataMsgListener)
        {
            this.dataMsgListener = dataMsgListener;
        }
        
        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception
        {
            try {
                String jsonData = new String(message.getPayload());
                _Logger.info("Received message from cloud on topic: " + topic);
                _Logger.fine("Message content: " + jsonData);
                
                // For Ubidots, the message might just be a simple value
                // We need to create an ActuatorData from it
                ActuatorData actuatorData = new ActuatorData();
                actuatorData.setLocationID(ConfigConst.CONSTRAINED_DEVICE);
                actuatorData.setTypeID(this.typeID);
                actuatorData.setName(this.itemName);
                
                // Try to parse the value - Ubidots might send just a number
                try {
                    // Try parsing as a simple number first
                    float value = Float.parseFloat(jsonData.trim());
                    int val = (int) value;
                    
                    switch (val) {
                        case 1:  // Ubidots typically uses 1 for ON
                            actuatorData.setCommand(ConfigConst.ON_COMMAND);
                            actuatorData.setStateData("LED switching ON");
                            _Logger.info("Received LED enablement message [ON].");
                            break;
                        case 0:  // Ubidots typically uses 0 for OFF
                            actuatorData.setCommand(ConfigConst.OFF_COMMAND);
                            actuatorData.setStateData("LED switching OFF");
                            _Logger.info("Received LED enablement message [OFF].");
                            break;
                        default:
                            _Logger.warning("Unknown LED command value: " + val);
                            return;
                    }
                    
                    actuatorData.setValue(value);
                    
                } catch (NumberFormatException nfe) {
                    // If not a simple number, try parsing as JSON ActuatorData
                    try {
                        actuatorData = DataUtil.getInstance().jsonToActuatorData(jsonData);
                        actuatorData.setLocationID(ConfigConst.CONSTRAINED_DEVICE);
                        actuatorData.setTypeID(this.typeID);
                        actuatorData.setName(this.itemName);
                        
                        int val = actuatorData.getCommand();
                        switch (val) {
                            case ConfigConst.ON_COMMAND:
                                _Logger.info("Received LED enablement message [ON].");
                                actuatorData.setStateData("LED switching ON");
                                break;
                            case ConfigConst.OFF_COMMAND:
                                _Logger.info("Received LED enablement message [OFF].");
                                actuatorData.setStateData("LED switching OFF");
                                break;
                            default:
                                _Logger.warning("Unknown LED command: " + val);
                                return;
                        }
                    } catch (Exception e) {
                        _Logger.warning("Failed to parse cloud message: " + e.getMessage());
                        return;
                    }
                }
                
                // Send to DeviceDataManager for processing
                if (this.dataMsgListener != null) {
                    // Convert to JSON for DeviceDataManager
                    String actuatorJson = DataUtil.getInstance().actuatorDataToJson(actuatorData);
                    this.dataMsgListener.handleIncomingMessage(
                        ResourceNameEnum.CDA_ACTUATOR_CMD_RESOURCE, actuatorJson);
                }
                
            } catch (Exception e) {
                _Logger.log(Level.WARNING, "Failed to process LED actuation message", e);
            }
        }
    }
    
    public CloudClientConnector()
    {
        super();
        
        ConfigUtil configUtil = ConfigUtil.getInstance();
        
        // Initialize MQTT client with cloud configuration
        this.mqttClient = new MqttClientConnector(ConfigConst.CLOUD_GATEWAY_SERVICE);
        
        // Set this as the connection listener
        this.mqttClient.setConnectionListener(this);
        
        // Get configuration values
        this.topicPrefix = configUtil.getProperty(
            ConfigConst.CLOUD_GATEWAY_SERVICE, ConfigConst.BASE_TOPIC_KEY);
        
        this.qos = configUtil.getInteger(
            ConfigConst.CLOUD_GATEWAY_SERVICE, ConfigConst.DEFAULT_QOS_KEY, ConfigConst.DEFAULT_QOS);
        
        // Get device ID from config or use default
        this.deviceID = configUtil.getProperty(
            ConfigConst.CLOUD_GATEWAY_SERVICE, ConfigConst.DEVICE_LOCATION_ID_KEY);
        
        if (this.deviceID == null || this.deviceID.isEmpty()) {
            this.deviceID = "gda-device";
        }
        
        _Logger.info("CloudClientConnector initialized with topic prefix: " + this.topicPrefix);
        _Logger.info("Device ID: " + this.deviceID);
    }
    
@Override
public boolean connectClient()
{
    if (this.mqttClient != null) {
        boolean started = this.mqttClient.connectClient();
        
        if (started) {
            // Wait for the async connection to actually complete
            int maxWaitSeconds = 10;
            int waited = 0;
            
            while (!this.mqttClient.isConnected() && waited < maxWaitSeconds) {
                try {
                    _Logger.info("Waiting for cloud connection... (" + waited + "s)");
                    Thread.sleep(1000);
                    waited++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            if (this.mqttClient.isConnected()) {
                _Logger.info("Cloud client connected successfully");
                return true;
            } else {
                _Logger.warning("Cloud client connection timed out after " + maxWaitSeconds + " seconds");
                return false;
            }
        }
    }
    return false;
}
    @Override
    public boolean disconnectClient()
    {
        if (this.mqttClient != null) {
            return this.mqttClient.disconnectClient();
        }
        return false;
    }

    @Override
    public boolean setDataMessageListener(IDataMessageListener listener)
    {
        if (listener != null) {
            this.dataMsgListener = listener;
            
            // Also set it on the MQTT client
            if (this.mqttClient != null) {
                this.mqttClient.setDataMessageListener(listener);
            }
            
            return true;
        }
        return false;
    }
@Override
public boolean sendEdgeDataToCloud(ResourceNameEnum resource, SensorData data)
{
    if (data != null && this.mqttClient != null) {
        // Build variable name based on sensor type
        String variableName = "";
        
        switch(data.getTypeID()) {
            case ConfigConst.TEMP_SENSOR_TYPE:
                variableName = "temperature";
                break;
            case ConfigConst.HUMIDITY_SENSOR_TYPE:
                variableName = "humidity";
                break;
            case ConfigConst.PRESSURE_SENSOR_TYPE:
                variableName = "pressure";
                break;
            default:
                variableName = data.getName().toLowerCase().replace(" ", "-");
        }
        
        // Build Ubidots JSON with proper variable name
        String ubidotsJson = "{\"" + variableName + "\":" + data.getValue() + "}";
        
        String cloudTopic = this.topicPrefix + this.deviceID;
        
        _Logger.info("Sending to Ubidots: " + cloudTopic + " -> " + ubidotsJson);
        
        return this.mqttClient.publishMessage(cloudTopic, ubidotsJson.getBytes(), this.qos);
    }
    return false;
}

@Override
public boolean sendEdgeDataToCloud(ResourceNameEnum resource, SystemPerformanceData data)
{
    if (data != null && this.mqttClient != null) {
        String cloudTopic = this.topicPrefix + this.deviceID;
        
        try {
            String cpuJson = "{\"cpu-utilization\":" + data.getCpuUtilization() + "}";
            String memJson = "{\"memory-utilization\":" + data.getMemoryUtilization() + "}";
            String diskJson = "{\"disk-utilization\":" + data.getDiskUtilization() + "}";
            
            _Logger.info("Sending CPU to Ubidots: " + cpuJson);
            this.mqttClient.publishMessage(cloudTopic, cpuJson.getBytes(), this.qos);
            
            Thread.sleep(100);
            
            _Logger.info("Sending Memory to Ubidots: " + memJson);
            this.mqttClient.publishMessage(cloudTopic, memJson.getBytes(), this.qos);
            
            Thread.sleep(100);
            
            _Logger.info("Sending Disk to Ubidots: " + diskJson);
            this.mqttClient.publishMessage(cloudTopic, diskJson.getBytes(), this.qos);
            
            return true;
        } catch (InterruptedException e) {
            _Logger.warning("Sleep interrupted: " + e.getMessage());
        }
    }
    return false;
}
    @Override
    public boolean subscribeToCloudEvents(ResourceNameEnum resource)
    {
        // This could be implemented if you need to subscribe to specific cloud events
        return false;
    }

    @Override
    public boolean unsubscribeFromCloudEvents(ResourceNameEnum resource)
    {
        return false;
    }
    
    // IConnectionListener methods - CRITICAL FOR LED SUBSCRIPTION
    @Override
    public void onConnect()
    {
        _Logger.info("Connected to cloud service - setting up LED subscription");
        
        // Create LED message listener
        LedEnablementMessageListener ledListener = 
            new LedEnablementMessageListener(this.dataMsgListener);
        
        // Build the LED topic name
        // For Ubidots, this would be something like: /v1.6/devices/{device-id}/led-actuator/lv
        // The "/lv" suffix gets the last value
        String ledTopic = this.topicPrefix + this.deviceID + "/led-actuator/lv";
        
        _Logger.info("Subscribing to LED topic: " + ledTopic);
        
        // Subscribe to LED actuator topic
        if (this.mqttClient != null) {
            boolean subscribed = this.mqttClient.subscribeToTopic(ledTopic, this.qos, ledListener);
            if (subscribed) {
                _Logger.info("Successfully subscribed to LED actuator topic");
            } else {
                _Logger.warning("Failed to subscribe to LED actuator topic");
            }
        }
    }
    
    @Override
    public void onDisconnect()
    {
        _Logger.info("Disconnected from cloud service");
    }
    
    // Private helper method
    private String buildTopicName(ResourceNameEnum resource, Object data)
    {
        // For Ubidots, build the appropriate topic
        // Format: /v1.6/devices/{device-id}
        
        if (this.topicPrefix != null && this.deviceID != null) {
            String topic = this.topicPrefix + this.deviceID;
            
            // You might want to add variable names based on the data type
            if (data instanceof SensorData) {
                SensorData sd = (SensorData) data;
                // Add the sensor name as a variable
                topic += "/" + sd.getName().toLowerCase().replace(" ", "-");
            } else if (data instanceof SystemPerformanceData) {
                SystemPerformanceData spd = (SystemPerformanceData) data;
                // Add the system data name
                topic += "/" + spd.getName().toLowerCase().replace(" ", "-");
            }
            
            _Logger.fine("Built topic: " + topic);
            return topic;
        }
        
        return resource.getResourceName();
    }
}