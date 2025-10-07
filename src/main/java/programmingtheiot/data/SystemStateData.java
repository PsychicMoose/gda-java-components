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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import programmingtheiot.common.ConfigConst;

/**
 * Convenience wrapper to store system state data, including location
 * information, action command, state data and a list of the following
 * data items:
 * <p>SystemPerformanceData
 * <p>SensorData
 * 
 * NOTE: This is an OPTIONAL implementation as noted in the requirements.
 */
public class SystemStateData extends BaseIotData implements Serializable
{
	// static
	private static final long serialVersionUID = 1L;
	
	// private var's
	private int command = ConfigConst.DEFAULT_COMMAND;
	private List<SensorData> sensorDataList = new ArrayList<>();
	private List<SystemPerformanceData> sysPerfDataList = new ArrayList<>();
    
	// constructors
	
	/**
	 * Default constructor.
	 */
	public SystemStateData()
	{
		super();
	}
	
	
	// public methods
	
	/**
	 * Adds a SensorData instance to the internal list.
	 * @param data The SensorData to add
	 * @return True if successfully added, false otherwise
	 */
	public boolean addSensorData(SensorData data)
	{
		if (data != null) {
			return this.sensorDataList.add(data);
		}
		return false;
	}
	
	/**
	 * Adds a SystemPerformanceData instance to the internal list.
	 * @param data The SystemPerformanceData to add
	 * @return True if successfully added, false otherwise
	 */
	public boolean addSystemPerformanceData(SystemPerformanceData data)
	{
		if (data != null) {
			return this.sysPerfDataList.add(data);
		}
		return false;
	}
	
	/**
	 * Gets the command value.
	 * @return The command as an integer
	 */
	public int getCommand()
	{
		return this.command;
	}
	
	/**
	 * Gets the list of SensorData instances.
	 * @return The list of SensorData (may be empty but not null)
	 */
	public List<SensorData> getSensorDataList()
	{
		return this.sensorDataList;
	}
	
	/**
	 * Gets the list of SystemPerformanceData instances.
	 * @return The list of SystemPerformanceData (may be empty but not null)
	 */
	public List<SystemPerformanceData> getSystemPerformanceDataList()
	{
		return this.sysPerfDataList;
	}
	
	/**
	 * Sets the command value.
	 * @param actionCmd The command to set
	 */
	public void setCommand(int actionCmd)
	{
		this.command = actionCmd;
	}
	
	/**
	 * Returns a string representation of this instance. This will invoke the base class
	 * {@link #toString()} method, then append the output from this call.
	 * 
	 * @return String The string representing this instance, returned in CSV 'key=value' format.
	 */
	public String toString()
	{
		StringBuilder sb = new StringBuilder(super.toString());
		
		sb.append(',');
		sb.append(ConfigConst.COMMAND_PROP).append('=').append(this.getCommand()).append(',');
		sb.append(ConfigConst.SENSOR_DATA_LIST_PROP).append('=').append(this.getSensorDataList()).append(',');
		sb.append(ConfigConst.SYSTEM_PERF_DATA_LIST_PROP).append('=').append(this.getSystemPerformanceDataList());
		
		return sb.toString();
	}
	
	
	// protected methods
	
	/**
	 * Handles updating data from another BaseIotData instance.
	 * If the data is a SystemStateData instance, copies its specific properties.
	 * 
	 * @param data The BaseIotData instance to update from
	 */
	protected void handleUpdateData(BaseIotData data)
	{
		if (data instanceof SystemStateData) {
			SystemStateData ssData = (SystemStateData) data;
			
			// Set the command
			this.setCommand(ssData.getCommand());
			
			// Clear and copy sensor data list
			this.sensorDataList.clear();
			List<SensorData> sensorList = ssData.getSensorDataList();
			if (sensorList != null) {
				this.sensorDataList.addAll(sensorList);
			}
			
			// Clear and copy system performance data list
			this.sysPerfDataList.clear();
			List<SystemPerformanceData> perfList = ssData.getSystemPerformanceDataList();
			if (perfList != null) {
				this.sysPerfDataList.addAll(perfList);
			}
		}
	}
	
}