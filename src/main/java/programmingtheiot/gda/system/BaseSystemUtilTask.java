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

package programmingtheiot.gda.system;

import java.util.logging.Logger;

/**
 * Base class for system utilization monitoring tasks.
 * Provides common functionality for CPU, memory, and disk utilization tasks.
 */
public abstract class BaseSystemUtilTask
{
	// private var's
	private static final Logger _Logger = Logger.getLogger(BaseSystemUtilTask.class.getName());
	
	private String name = null;
	private int typeID = 0;
	private float latestValue = 0.0f;
	
	
	// constructors
	
	/**
	 * Constructor with name and type ID.
	 * 
	 * @param name The name of this utilization task
	 * @param typeID The type identifier for this task
	 */
	public BaseSystemUtilTask(String name, int typeID)
	{
		this.name = name;
		this.typeID = typeID;
	}
	
	
	// public methods
	
	/**
	 * Gets the name of this utilization task.
	 * 
	 * @return The name of the task
	 */
	public String getName()
	{
		return this.name;
	}
	
	/**
	 * Gets the type ID of this utilization task.
	 * 
	 * @return The type ID
	 */
	public int getTypeID()
	{
		return this.typeID;
	}
	
	/**
	 * Retrieves the current telemetry value by calling the template method.
	 * This method is thread-safe and caches the latest value.
	 * 
	 * @return The current system utilization percentage (0.0 to 100.0)
	 */
	public synchronized float getTelemetryValue()
	{
		try {
			this.latestValue = getSystemUtil();
			
			// Validate the value is within expected range
			if (this.latestValue < 0.0f) {
				_Logger.warning("System utilization value is negative: " + this.latestValue + ". Setting to 0.0");
				this.latestValue = 0.0f;
			} else if (this.latestValue > 100.0f) {
				_Logger.warning("System utilization value exceeds 100%: " + this.latestValue + ". Setting to 100.0");
				this.latestValue = 100.0f;
			}
			
		} catch (Exception e) {
			_Logger.severe("Error getting telemetry value for " + this.name + ": " + e.getMessage());
			// Return the last known good value if there's an error
		}
		
		return this.latestValue;
	}
	
	/**
	 * Gets the latest cached telemetry value without refreshing.
	 * 
	 * @return The last retrieved system utilization percentage
	 */
	public float getLatestValue()
	{
		return this.latestValue;
	}
	
	
	// protected methods
	
	/**
	 * Template method to be implemented by subclasses for retrieving
	 * specific system utilization metrics.
	 * 
	 * @return The system utilization percentage (should be 0.0 to 100.0)
	 */
	protected abstract float getSystemUtil();
}