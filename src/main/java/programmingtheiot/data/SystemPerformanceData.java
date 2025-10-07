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

import programmingtheiot.common.ConfigConst;

/**
 * SystemPerformanceData implementation for handling system metrics.
 * This class contains CPU, memory, and disk utilization values.
 */
public class SystemPerformanceData extends BaseIotData implements Serializable
{
	// static
	private static final long serialVersionUID = 1L;
	
	// private var's
	private float cpuUtil = ConfigConst.DEFAULT_VAL;
	private float diskUtil = ConfigConst.DEFAULT_VAL;
	private float memUtil = ConfigConst.DEFAULT_VAL;
    
	// constructors
	
	/**
	 * Default constructor. Sets the name to SYS_PERF_DATA.
	 */
	public SystemPerformanceData()
	{
		super();
		setName(ConfigConst.SYS_PERF_DATA);
	}
	
	
	// public methods
	
	/**
	 * Gets the CPU utilization percentage.
	 * @return The CPU utilization as a float
	 */
	public float getCpuUtilization()
	{
		return this.cpuUtil;
	}
	
	/**
	 * Gets the disk utilization percentage.
	 * @return The disk utilization as a float
	 */
	public float getDiskUtilization()
	{
		return this.diskUtil;
	}
	
	/**
	 * Gets the memory utilization percentage.
	 * @return The memory utilization as a float
	 */
	public float getMemoryUtilization()
	{
		return this.memUtil;
	}
	
	/**
	 * Sets the CPU utilization and updates timestamp.
	 * @param val The CPU utilization percentage to set
	 */
	public void setCpuUtilization(float val)
	{
		updateTimeStamp();
		this.cpuUtil = val;
	}
	
	/**
	 * Sets the disk utilization and updates timestamp.
	 * @param val The disk utilization percentage to set
	 */
	public void setDiskUtilization(float val)
	{
		updateTimeStamp();
		this.diskUtil = val;
	}
	
	/**
	 * Sets the memory utilization and updates timestamp.
	 * @param val The memory utilization percentage to set
	 */
	public void setMemoryUtilization(float val)
	{
		updateTimeStamp();
		this.memUtil = val;
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
		sb.append(ConfigConst.CPU_UTIL_PROP).append('=').append(this.getCpuUtilization()).append(',');
		sb.append(ConfigConst.DISK_UTIL_PROP).append('=').append(this.getDiskUtilization()).append(',');
		sb.append(ConfigConst.MEM_UTIL_PROP).append('=').append(this.getMemoryUtilization());
		
		return sb.toString();
	}
	
	
	// protected methods
	
	/**
	 * Handles updating data from another BaseIotData instance.
	 * If the data is a SystemPerformanceData instance, copies its specific properties.
	 * 
	 * @param data The BaseIotData instance to update from
	 */
	protected void handleUpdateData(BaseIotData data)
	{
		if (data instanceof SystemPerformanceData) {
			SystemPerformanceData spData = (SystemPerformanceData) data;
			this.setCpuUtilization(spData.getCpuUtilization());
			this.setDiskUtilization(spData.getDiskUtilization());
			this.setMemoryUtilization(spData.getMemoryUtilization());
		}
	}
	
}