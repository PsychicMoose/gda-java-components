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

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;

/**
 * SystemCpuUtilTask monitors CPU utilization for the system.
 * Uses Java Management Extensions (JMX) to retrieve CPU metrics.
 */
public class SystemCpuUtilTask extends BaseSystemUtilTask
{
	// private var's
	private static final Logger _Logger = Logger.getLogger(SystemCpuUtilTask.class.getName());
	
	private OperatingSystemMXBean osMgr = null;
	private com.sun.management.OperatingSystemMXBean sunOsMgr = null;
	
	
	// constructors
	
	/**
	 * Default constructor.
	 * Initializes the operating system management bean for CPU monitoring.
	 */
	public SystemCpuUtilTask()
	{
		super("CpuUtil", 0);
		
		this.osMgr = ManagementFactory.getOperatingSystemMXBean();
		
		// Try to cast to Sun/Oracle specific implementation for more detailed metrics
		if (this.osMgr instanceof com.sun.management.OperatingSystemMXBean) {
			this.sunOsMgr = (com.sun.management.OperatingSystemMXBean) this.osMgr;
			_Logger.info("SystemCpuUtilTask using enhanced Sun/Oracle JVM metrics");
		} else {
			_Logger.info("SystemCpuUtilTask using standard JVM metrics");
		}
	}
	
	
	// protected methods
	
	/**
	 * Template method implementation to retrieve CPU utilization.
	 * 
	 * @return float The CPU utilization percentage (0.0 to 100.0)
	 */
	@Override
	protected float getSystemUtil()
	{
		float cpuUtil = 0.0f;
		
		try {
			if (this.sunOsMgr != null) {
				// Use Sun/Oracle specific method for system-wide CPU usage
				double cpuLoad = this.sunOsMgr.getSystemCpuLoad();
				
				// getSystemCpuLoad() returns a value between 0.0 and 1.0
				// or a negative value if not available
				if (cpuLoad >= 0.0) {
					cpuUtil = (float) (cpuLoad * 100.0);
					_Logger.fine("System CPU utilization: " + String.format("%.2f", cpuUtil) + "%");
				} else {
					// Fallback to process CPU usage
					cpuUtil = getProcessCpuUtil();
				}
			} else {
				// Use standard method - system load average
				cpuUtil = getSystemLoadAverage();
			}
			
			// Additional logging for debugging
			logDetailedCpuInfo();
			
		} catch (Exception e) {
			_Logger.severe("Error getting CPU utilization: " + e.getMessage());
		}
		
		// Ensure the value is within valid range
		if (cpuUtil < 0.0f) {
			cpuUtil = 0.0f;
		} else if (cpuUtil > 100.0f) {
			cpuUtil = 100.0f;
		}
		
		return cpuUtil;
	}
	
	/**
	 * Gets the CPU utilization for the current JVM process.
	 * 
	 * @return float The process CPU utilization percentage
	 */
	private float getProcessCpuUtil()
	{
		float cpuUtil = 0.0f;
		
		if (this.sunOsMgr != null) {
			double processCpuLoad = this.sunOsMgr.getProcessCpuLoad();
			
			if (processCpuLoad >= 0.0) {
				cpuUtil = (float) (processCpuLoad * 100.0);
				_Logger.fine("Process CPU utilization: " + String.format("%.2f", cpuUtil) + "%");
			}
		}
		
		return cpuUtil;
	}
	
	/**
	 * Gets system load average as a CPU utilization approximation.
	 * This is a fallback method when specific CPU metrics are not available.
	 * 
	 * @return float The estimated CPU utilization based on load average
	 */
	private float getSystemLoadAverage()
	{
		float cpuUtil = 0.0f;
		
		// Get 1-minute load average
		double loadAvg = this.osMgr.getSystemLoadAverage();
		
		if (loadAvg >= 0.0) {
			// Get number of available processors
			int processors = this.osMgr.getAvailableProcessors();
			
			// Calculate utilization as percentage
			// Load average / number of processors * 100
			cpuUtil = (float) ((loadAvg / processors) * 100.0);
			
			_Logger.fine("System load average: " + loadAvg + 
				", Processors: " + processors + 
				", Estimated CPU utilization: " + String.format("%.2f", cpuUtil) + "%");
		}
		
		return cpuUtil;
	}
	
	/**
	 * Logs detailed CPU information for debugging purposes.
	 */
	private void logDetailedCpuInfo()
	{
		if (_Logger.isLoggable(java.util.logging.Level.FINEST)) {
			StringBuilder sb = new StringBuilder("CPU Details: ");
			sb.append("Available processors=").append(this.osMgr.getAvailableProcessors());
			sb.append(", System load average=").append(this.osMgr.getSystemLoadAverage());
			
			if (this.sunOsMgr != null) {
				sb.append(", System CPU load=").append(this.sunOsMgr.getSystemCpuLoad());
				sb.append(", Process CPU load=").append(this.sunOsMgr.getProcessCpuLoad());
				sb.append(", Process CPU time=").append(this.sunOsMgr.getProcessCpuTime());
			}
			
			_Logger.finest(sb.toString());
		}
	}
	
	/**
	 * Gets the number of available processors.
	 * 
	 * @return The number of available processors
	 */
	public int getAvailableProcessors()
	{
		return this.osMgr.getAvailableProcessors();
	}
}