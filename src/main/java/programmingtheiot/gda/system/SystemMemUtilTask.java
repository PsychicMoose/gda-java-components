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
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;

/**
 * SystemMemUtilTask monitors memory utilization for the system.
 * Uses Java Management Extensions (JMX) to retrieve memory metrics.
 */
public class SystemMemUtilTask extends BaseSystemUtilTask
{
	// private var's
	private static final Logger _Logger = Logger.getLogger(SystemMemUtilTask.class.getName());
	
	private MemoryMXBean memMgr = null;
	private OperatingSystemMXBean osMgr = null;
	private com.sun.management.OperatingSystemMXBean sunOsMgr = null;
	
	
	// constructors
	
	/**
	 * Default constructor.
	 * Initializes the memory management beans for memory monitoring.
	 */
	public SystemMemUtilTask()
	{
		super("MemUtil", 0);
		
		this.memMgr = ManagementFactory.getMemoryMXBean();
		this.osMgr = ManagementFactory.getOperatingSystemMXBean();
		
		// Try to cast to Sun/Oracle specific implementation for system-wide memory metrics
		if (this.osMgr instanceof com.sun.management.OperatingSystemMXBean) {
			this.sunOsMgr = (com.sun.management.OperatingSystemMXBean) this.osMgr;
			_Logger.info("SystemMemUtilTask using enhanced Sun/Oracle JVM metrics");
		} else {
			_Logger.info("SystemMemUtilTask using standard JVM metrics");
		}
	}
	
	
	// protected methods
	
	/**
	 * Template method implementation to retrieve memory utilization.
	 * 
	 * @return float The memory utilization percentage (0.0 to 100.0)
	 */
	@Override
	protected float getSystemUtil()
	{
		float memUtil = 0.0f;
		
		try {
			// First try to get system-wide memory usage
			if (this.sunOsMgr != null) {
				memUtil = getSystemMemoryUtilization();
			}
			
			// If system-wide metrics are not available or zero, fall back to JVM memory
			if (memUtil <= 0.0f) {
				memUtil = getJvmMemoryUtilization();
			}
			
			// Log detailed memory information for debugging
			logDetailedMemoryInfo();
			
		} catch (Exception e) {
			_Logger.severe("Error getting memory utilization: " + e.getMessage());
		}
		
		// Ensure the value is within valid range
		if (memUtil < 0.0f) {
			memUtil = 0.0f;
		} else if (memUtil > 100.0f) {
			memUtil = 100.0f;
		}
		
		return memUtil;
	}
	
	/**
	 * Gets system-wide memory utilization using Sun/Oracle specific APIs.
	 * 
	 * @return float The system memory utilization percentage
	 */
	private float getSystemMemoryUtilization()
	{
		float memUtil = 0.0f;
		
		if (this.sunOsMgr != null) {
			long totalPhysicalMemory = this.sunOsMgr.getTotalPhysicalMemorySize();
			long freePhysicalMemory = this.sunOsMgr.getFreePhysicalMemorySize();
			
			if (totalPhysicalMemory > 0) {
				long usedMemory = totalPhysicalMemory - freePhysicalMemory;
				memUtil = ((float) usedMemory / (float) totalPhysicalMemory) * 100.0f;
				
				_Logger.fine("System memory utilization: " + 
					"Total=" + formatBytes(totalPhysicalMemory) + 
					", Free=" + formatBytes(freePhysicalMemory) + 
					", Used=" + formatBytes(usedMemory) + 
					" (" + String.format("%.2f", memUtil) + "%)");
			}
		}
		
		return memUtil;
	}
	
	/**
	 * Gets JVM memory utilization as a fallback when system metrics are not available.
	 * 
	 * @return float The JVM memory utilization percentage
	 */
	private float getJvmMemoryUtilization()
	{
		float memUtil = 0.0f;
		
		// Get heap memory usage
		MemoryUsage heapUsage = this.memMgr.getHeapMemoryUsage();
		
		// Get non-heap memory usage (includes metaspace, code cache, etc.)
		MemoryUsage nonHeapUsage = this.memMgr.getNonHeapMemoryUsage();
		
		// Calculate total memory usage
		long totalUsed = heapUsage.getUsed() + nonHeapUsage.getUsed();
		long totalMax = heapUsage.getMax() + nonHeapUsage.getMax();
		
		// If max is not defined (-1), use committed memory
		if (heapUsage.getMax() == -1) {
			totalMax = heapUsage.getCommitted() + nonHeapUsage.getCommitted();
		}
		
		if (totalMax > 0) {
			memUtil = ((float) totalUsed / (float) totalMax) * 100.0f;
			
			_Logger.fine("JVM memory utilization: " +
				"Heap Used=" + formatBytes(heapUsage.getUsed()) + 
				", Non-Heap Used=" + formatBytes(nonHeapUsage.getUsed()) +
				", Total Max=" + formatBytes(totalMax) + 
				" (" + String.format("%.2f", memUtil) + "%)");
		}
		
		return memUtil;
	}
	
	/**
	 * Helper method to format bytes into human-readable format.
	 * 
	 * @param bytes The number of bytes to format
	 * @return String representation of bytes (e.g., "1.5 GB")
	 */
	private String formatBytes(long bytes)
	{
		if (bytes < 0) {
			return "N/A";
		}
		
		if (bytes < 1024) {
			return bytes + " B";
		}
		
		int exp = (int) (Math.log(bytes) / Math.log(1024));
		String unit = "KMGTPE".charAt(exp - 1) + "B";
		
		return String.format("%.1f %s", bytes / Math.pow(1024, exp), unit);
	}
	
	/**
	 * Logs detailed memory information for debugging purposes.
	 */
	private void logDetailedMemoryInfo()
	{
		if (_Logger.isLoggable(java.util.logging.Level.FINEST)) {
			StringBuilder sb = new StringBuilder("Memory Details:\n");
			
			// Heap memory details
			MemoryUsage heap = this.memMgr.getHeapMemoryUsage();
			sb.append("  Heap: init=").append(formatBytes(heap.getInit()));
			sb.append(", used=").append(formatBytes(heap.getUsed()));
			sb.append(", committed=").append(formatBytes(heap.getCommitted()));
			sb.append(", max=").append(formatBytes(heap.getMax())).append("\n");
			
			// Non-heap memory details
			MemoryUsage nonHeap = this.memMgr.getNonHeapMemoryUsage();
			sb.append("  Non-Heap: init=").append(formatBytes(nonHeap.getInit()));
			sb.append(", used=").append(formatBytes(nonHeap.getUsed()));
			sb.append(", committed=").append(formatBytes(nonHeap.getCommitted()));
			sb.append(", max=").append(formatBytes(nonHeap.getMax()));
			
			// System memory details if available
			if (this.sunOsMgr != null) {
				sb.append("\n  System: total=").append(formatBytes(this.sunOsMgr.getTotalPhysicalMemorySize()));
				sb.append(", free=").append(formatBytes(this.sunOsMgr.getFreePhysicalMemorySize()));
				
				long totalSwap = this.sunOsMgr.getTotalSwapSpaceSize();
				long freeSwap = this.sunOsMgr.getFreeSwapSpaceSize();
				if (totalSwap > 0) {
					sb.append("\n  Swap: total=").append(formatBytes(totalSwap));
					sb.append(", free=").append(formatBytes(freeSwap));
				}
			}
			
			_Logger.finest(sb.toString());
		}
	}
	
	/**
	 * Gets the heap memory utilization percentage.
	 * 
	 * @return The heap memory utilization as a percentage
	 */
	public float getHeapUtilization()
	{
		MemoryUsage heapUsage = this.memMgr.getHeapMemoryUsage();
		
		long used = heapUsage.getUsed();
		long max = heapUsage.getMax();
		
		if (max == -1) {
			max = heapUsage.getCommitted();
		}
		
		if (max > 0) {
			return ((float) used / (float) max) * 100.0f;
		}
		
		return 0.0f;
	}
}