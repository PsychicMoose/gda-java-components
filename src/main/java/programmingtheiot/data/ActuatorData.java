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
 * ActuatorData implementation for handling actuator commands and responses.
 * This class contains command, value, state data, and response flag information.
 */
public class ActuatorData extends BaseIotData implements Serializable
{
	// static
	private static final long serialVersionUID = 1L;
	
	// private var's
	private int     command      = ConfigConst.DEFAULT_COMMAND;
	private float   value        = ConfigConst.DEFAULT_VAL;
	private boolean isResponse   = false;
	private String  stateData    = "";
    
    
	// constructors
	
	/**
	 * Default constructor.
	 */
	public ActuatorData()
	{
		super();
	}
	
	
	// public methods
	
	/**
	 * Gets the command value.
	 * @return The command as an integer
	 */
	public int getCommand()
	{
		return this.command;
	}
	
	/**
	 * Gets the state data string.
	 * @return The state data
	 */
	public String getStateData()
	{
		return this.stateData;
	}
	
	/**
	 * Gets the actuator value.
	 * @return The value as a float
	 */
	public float getValue()
	{
		return this.value;
	}
	
	/**
	 * Checks if this is a response message.
	 * @return True if this is a response, false otherwise
	 */
	public boolean isResponseFlagEnabled()
	{
		return this.isResponse;
	}
	
	/**
	 * Sets this actuator data as a response and updates timestamp.
	 */
	public void setAsResponse()
	{
		updateTimeStamp();
		this.isResponse = true;
	}
	
	/**
	 * Sets the command value and updates timestamp.
	 * @param command The command to set
	 */
	public void setCommand(int command)
	{
		updateTimeStamp();
		this.command = command;
	}
	
	/**
	 * Sets the state data and updates timestamp.
	 * @param stateData The state data to set (null values are ignored)
	 */
	public void setStateData(String stateData)
	{
		updateTimeStamp();
		if (stateData != null) {
			this.stateData = stateData;
		}
	}
	
	/**
	 * Sets the actuator value and updates timestamp.
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
		sb.append(ConfigConst.COMMAND_PROP).append('=').append(this.getCommand()).append(',');
		sb.append(ConfigConst.IS_RESPONSE_PROP).append('=').append(this.isResponseFlagEnabled()).append(',');
		sb.append(ConfigConst.VALUE_PROP).append('=').append(this.getValue());
		
		return sb.toString();
	}
	
	
	// protected methods
	
	/**
	 * Handles updating data from another BaseIotData instance.
	 * If the data is an ActuatorData instance, copies its specific properties.
	 * 
	 * @param data The BaseIotData instance to update from
	 */
	protected void handleUpdateData(BaseIotData data)
	{
		if (data instanceof ActuatorData) {
			ActuatorData aData = (ActuatorData) data;
			this.setCommand(aData.getCommand());
			this.setValue(aData.getValue());
			this.setStateData(aData.getStateData());
			
			if (aData.isResponseFlagEnabled()) {
				this.isResponse = true;
			}
		}
	}
	
}