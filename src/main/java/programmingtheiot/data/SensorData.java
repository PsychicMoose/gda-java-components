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
 * SensorData implementation for handling sensor readings.
 * This class contains the sensor value and related methods.
 */
public class SensorData extends BaseIotData implements Serializable
{
	// static
	private static final long serialVersionUID = 1L;
	
	// private var's
	private float value = ConfigConst.DEFAULT_VAL;
    
	// constructors
	
	/**
	 * Default constructor.
	 */
	public SensorData()
	{
		super();
	}
	
	/**
	 * Constructor with sensor type.
	 * @param sensorType The type ID for this sensor
	 */
	public SensorData(int sensorType)
	{
		super();
		setTypeID(sensorType);
	}
	
	
	// public methods
	
	/**
	 * Gets the sensor value.
	 * @return The value as a float
	 */
	public float getValue()
	{
		return this.value;
	}
	
	/**
	 * Sets the sensor value and updates timestamp.
	 * @param val The value to set
	 */
	public void setValue(float val)
	{
		updateTimeStamp();
		this.value = val;
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
		sb.append(ConfigConst.VALUE_PROP).append('=').append(this.getValue());
		
		return sb.toString();
	}
	
	
	// protected methods
	
	/**
	 * Handles updating data from another BaseIotData instance.
	 * If the data is a SensorData instance, copies its specific properties.
	 * 
	 * @param data The BaseIotData instance to update from
	 */
	protected void handleUpdateData(BaseIotData data)
	{
		if (data instanceof SensorData) {
			SensorData sData = (SensorData) data;
			this.setValue(sData.getValue());
		}
	}
	
}