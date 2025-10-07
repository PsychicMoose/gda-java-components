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

package programmingtheiot.data;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * Utility class for converting between data objects and JSON representation.
 * Uses Google Gson library for serialization and deserialization.
 */
public class DataUtil
{
	// static
	
	private static final Logger _Logger = 
		Logger.getLogger(DataUtil.class.getName());
	
	private static final DataUtil _Instance = new DataUtil();

	/**
	 * Returns the Singleton instance of this class.
	 * 
	 * @return DataUtil
	 */
	public static final DataUtil getInstance()
	{
		return _Instance;
	}
	
	
	// private var's
	private Gson gson;
	
	
	// constructors
	
	/**
	 * Default (private).
	 * Initializes Gson with pretty printing enabled.
	 */
	private DataUtil()
	{
		super();
		
		// Initialize Gson with pretty printing for better readability
		this.gson = new GsonBuilder()
			.setPrettyPrinting()
			.enableComplexMapKeySerialization()
			.create();
	}
	
	
	// public methods
	
	/**
	 * Converts ActuatorData to JSON string.
	 * 
	 * @param actuatorData The ActuatorData instance to convert
	 * @return JSON string representation, or null if input is null
	 */
	public String actuatorDataToJson(ActuatorData actuatorData)
	{
		if (actuatorData == null) {
			_Logger.warning("ActuatorData is null. Cannot convert to JSON.");
			return null;
		}
		
		try {
			String jsonData = this.gson.toJson(actuatorData);
			return jsonData;
		} catch (Exception e) {
			_Logger.severe("Failed to convert ActuatorData to JSON: " + e.getMessage());
			return null;
		}
	}
	
	/**
	 * Converts SensorData to JSON string.
	 * 
	 * @param sensorData The SensorData instance to convert
	 * @return JSON string representation, or null if input is null
	 */
	public String sensorDataToJson(SensorData sensorData)
	{
		if (sensorData == null) {
			_Logger.warning("SensorData is null. Cannot convert to JSON.");
			return null;
		}
		
		try {
			String jsonData = this.gson.toJson(sensorData);
			return jsonData;
		} catch (Exception e) {
			_Logger.severe("Failed to convert SensorData to JSON: " + e.getMessage());
			return null;
		}
	}
	
	/**
	 * Converts SystemPerformanceData to JSON string.
	 * 
	 * @param sysPerfData The SystemPerformanceData instance to convert
	 * @return JSON string representation, or null if input is null
	 */
	public String systemPerformanceDataToJson(SystemPerformanceData sysPerfData)
	{
		if (sysPerfData == null) {
			_Logger.warning("SystemPerformanceData is null. Cannot convert to JSON.");
			return null;
		}
		
		try {
			String jsonData = this.gson.toJson(sysPerfData);
			return jsonData;
		} catch (Exception e) {
			_Logger.severe("Failed to convert SystemPerformanceData to JSON: " + e.getMessage());
			return null;
		}
	}
	
	/**
	 * Converts SystemStateData to JSON string.
	 * 
	 * @param sysStateData The SystemStateData instance to convert
	 * @return JSON string representation, or null if input is null
	 */
	public String systemStateDataToJson(SystemStateData sysStateData)
	{
		if (sysStateData == null) {
			_Logger.warning("SystemStateData is null. Cannot convert to JSON.");
			return null;
		}
		
		try {
			String jsonData = this.gson.toJson(sysStateData);
			return jsonData;
		} catch (Exception e) {
			_Logger.severe("Failed to convert SystemStateData to JSON: " + e.getMessage());
			return null;
		}
	}
	
	/**
	 * Converts JSON string to ActuatorData object.
	 * 
	 * @param jsonData The JSON string to parse
	 * @return ActuatorData instance, or null if parsing fails
	 */
	public ActuatorData jsonToActuatorData(String jsonData)
	{
		if (jsonData == null || jsonData.trim().isEmpty()) {
			_Logger.warning("JSON data is null or empty. Cannot convert to ActuatorData.");
			return null;
		}
		
		try {
			ActuatorData actuatorData = this.gson.fromJson(jsonData, ActuatorData.class);
			return actuatorData;
		} catch (JsonSyntaxException e) {
			_Logger.severe("Failed to parse JSON to ActuatorData: " + e.getMessage());
			return null;
		}
	}
	
	/**
	 * Converts JSON string to SensorData object.
	 * 
	 * @param jsonData The JSON string to parse
	 * @return SensorData instance, or null if parsing fails
	 */
	public SensorData jsonToSensorData(String jsonData)
	{
		if (jsonData == null || jsonData.trim().isEmpty()) {
			_Logger.warning("JSON data is null or empty. Cannot convert to SensorData.");
			return null;
		}
		
		try {
			SensorData sensorData = this.gson.fromJson(jsonData, SensorData.class);
			return sensorData;
		} catch (JsonSyntaxException e) {
			_Logger.severe("Failed to parse JSON to SensorData: " + e.getMessage());
			return null;
		}
	}
	
	/**
	 * Converts JSON string to SystemPerformanceData object.
	 * 
	 * @param jsonData The JSON string to parse
	 * @return SystemPerformanceData instance, or null if parsing fails
	 */
	public SystemPerformanceData jsonToSystemPerformanceData(String jsonData)
	{
		if (jsonData == null || jsonData.trim().isEmpty()) {
			_Logger.warning("JSON data is null or empty. Cannot convert to SystemPerformanceData.");
			return null;
		}
		
		try {
			SystemPerformanceData sysPerfData = this.gson.fromJson(jsonData, SystemPerformanceData.class);
			return sysPerfData;
		} catch (JsonSyntaxException e) {
			_Logger.severe("Failed to parse JSON to SystemPerformanceData: " + e.getMessage());
			return null;
		}
	}
	
	/**
	 * Converts JSON string to SystemStateData object.
	 * 
	 * @param jsonData The JSON string to parse
	 * @return SystemStateData instance, or null if parsing fails
	 */
	public SystemStateData jsonToSystemStateData(String jsonData)
	{
		if (jsonData == null || jsonData.trim().isEmpty()) {
			_Logger.warning("JSON data is null or empty. Cannot convert to SystemStateData.");
			return null;
		}
		
		try {
			SystemStateData sysStateData = this.gson.fromJson(jsonData, SystemStateData.class);
			return sysStateData;
		} catch (JsonSyntaxException e) {
			_Logger.severe("Failed to parse JSON to SystemStateData: " + e.getMessage());
			return null;
		}
	}
	
	/**
	 * Helper method to write JSON data to a file.
	 * 
	 * @param jsonData The JSON string to write
	 * @param filename The filename to write to
	 * @return True if successful, false otherwise
	 */
	public boolean writeJsonToFile(String jsonData, String filename)
	{
		if (jsonData == null || filename == null) {
			_Logger.warning("JSON data or filename is null. Cannot write to file.");
			return false;
		}
		
		try {
			Path path = FileSystems.getDefault().getPath(filename);
			Files.write(path, jsonData.getBytes());
			return true;
		} catch (Exception e) {
			_Logger.severe("Failed to write JSON to file: " + e.getMessage());
			return false;
		}
	}
	
	/**
	 * Helper method to read JSON data from a file.
	 * 
	 * @param filename The filename to read from
	 * @return The JSON string content, or null if reading fails
	 */
	public String readJsonFromFile(String filename)
	{
		if (filename == null) {
			_Logger.warning("Filename is null. Cannot read from file.");
			return null;
		}
		
		try {
			Path path = FileSystems.getDefault().getPath(filename);
			byte[] jsonBytes = Files.readAllBytes(path);
			return new String(jsonBytes);
		} catch (Exception e) {
			_Logger.severe("Failed to read JSON from file: " + e.getMessage());
			return null;
		}
	}
}