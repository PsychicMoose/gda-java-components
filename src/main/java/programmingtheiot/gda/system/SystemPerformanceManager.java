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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.IDataMessageListener;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.SystemPerformanceData;

/**
 * SystemPerformanceManager manages the collection and reporting of system performance metrics.
 * It periodically collects CPU, memory, and disk utilization data and notifies listeners.
 */
public class SystemPerformanceManager
{
	// private var's
	private static final Logger _Logger = Logger.getLogger(SystemPerformanceManager.class.getName());
	
	private int pollRate = ConfigConst.DEFAULT_POLL_CYCLES;
	private ScheduledExecutorService schedExecSvc = null;
	private ScheduledFuture<?> scheduledTask = null;
	
	private SystemCpuUtilTask sysCpuUtilTask = null;
	private SystemMemUtilTask sysMemUtilTask = null;
	private SystemDiskUtilTask sysDiskUtilTask = null;
	
	private String locationID = ConfigConst.NOT_SET;
	private IDataMessageListener dataMsgListener = null;
	
	private Runnable taskRunner = null;
	private boolean isStarted = false;

	
	// constructors
	
	/**
	 * Default constructor.
	 * Initializes the system performance tasks and configures the polling rate.
	 */
	public SystemPerformanceManager()
	{
		// Get the poll rate from configuration
		this.pollRate =
			ConfigUtil.getInstance().getInteger(
				ConfigConst.GATEWAY_DEVICE, ConfigConst.POLL_CYCLES_KEY, ConfigConst.DEFAULT_POLL_CYCLES);
		
		if (this.pollRate <= 0) {
			this.pollRate = ConfigConst.DEFAULT_POLL_CYCLES;
		}
		
		// Get the location ID from configuration
		this.locationID =
			ConfigUtil.getInstance().getProperty(
				ConfigConst.GATEWAY_DEVICE, ConfigConst.LOCATION_ID_PROP, ConfigConst.NOT_SET);
		
		// Initialize the scheduled executor service
		this.schedExecSvc = Executors.newScheduledThreadPool(1);
		
		// Initialize the system utilization tasks
		this.sysCpuUtilTask = new SystemCpuUtilTask();
		this.sysMemUtilTask = new SystemMemUtilTask();
		this.sysDiskUtilTask = new SystemDiskUtilTask();
		
		// Define the task runner
		this.taskRunner = () -> {
			this.handleTelemetry();
		};
		
		_Logger.info("SystemPerformanceManager initialized with poll rate: " + this.pollRate + " seconds");
	}
	
	
	// public methods
	
	/**
	 * Handles telemetry collection by gathering CPU, memory, and disk utilization data.
	 * Creates a SystemPerformanceData instance and notifies the data message listener if set.
	 */
	public void handleTelemetry()
	{
		// Collect telemetry values
		float cpuUtil = this.sysCpuUtilTask.getTelemetryValue();
		float memUtil = this.sysMemUtilTask.getTelemetryValue();
		float diskUtil = this.sysDiskUtilTask.getTelemetryValue();
		
		// Log the telemetry values (change to 'info' level for testing if needed)
		_Logger.fine("System Performance - CPU: " + cpuUtil + "%, Memory: " + memUtil + "%, Disk: " + diskUtil + "%");
		
		// Create and populate SystemPerformanceData
		SystemPerformanceData spd = new SystemPerformanceData();
		spd.setLocationID(this.locationID);
		spd.setCpuUtilization(cpuUtil);
		spd.setMemoryUtilization(memUtil);
		spd.setDiskUtilization(diskUtil);
		
		// Notify the data message listener if set
		if (this.dataMsgListener != null) {
			this.dataMsgListener.handleSystemPerformanceMessage(
				ResourceNameEnum.GDA_SYSTEM_PERF_MSG_RESOURCE, spd);
		}
	}
	
	/**
	 * Sets the data message listener for receiving system performance updates.
	 * 
	 * @param listener The IDataMessageListener to receive updates
	 */
	public void setDataMessageListener(IDataMessageListener listener)
	{
		if (listener != null) {
			this.dataMsgListener = listener;
			_Logger.info("Data message listener set for SystemPerformanceManager");
		}
	}
	
	/**
	 * Starts the SystemPerformanceManager, beginning periodic telemetry collection.
	 * 
	 * @return true if successfully started or already running, false otherwise
	 */
	public boolean startManager()
	{
		if (!this.isStarted) {
			_Logger.info("SystemPerformanceManager is starting...");
			
			try {
				this.scheduledTask = 
					this.schedExecSvc.scheduleAtFixedRate(
						this.taskRunner, 
						1L, 
						this.pollRate, 
						TimeUnit.SECONDS);
				
				this.isStarted = true;
				_Logger.info("SystemPerformanceManager started successfully");
			} catch (Exception e) {
				_Logger.severe("Failed to start SystemPerformanceManager: " + e.getMessage());
				return false;
			}
		} else {
			_Logger.info("SystemPerformanceManager is already started.");
		}
		
		return this.isStarted;
	}
	
	/**
	 * Stops the SystemPerformanceManager, ending periodic telemetry collection.
	 * 
	 * @return true if successfully stopped
	 */
	public boolean stopManager()
	{
		if (this.isStarted) {
			try {
				if (this.scheduledTask != null) {
					this.scheduledTask.cancel(false);
				}
				
				this.schedExecSvc.shutdown();
				
				// Wait for tasks to terminate
				if (!this.schedExecSvc.awaitTermination(5, TimeUnit.SECONDS)) {
					this.schedExecSvc.shutdownNow();
					
					// Wait again after shutdownNow
					if (!this.schedExecSvc.awaitTermination(5, TimeUnit.SECONDS)) {
						_Logger.warning("SystemPerformanceManager executor did not terminate");
					}
				}
				
				this.isStarted = false;
				_Logger.info("SystemPerformanceManager stopped successfully");
				
			} catch (InterruptedException e) {
				_Logger.severe("Error stopping SystemPerformanceManager: " + e.getMessage());
				this.schedExecSvc.shutdownNow();
				Thread.currentThread().interrupt();
			}
		} else {
			_Logger.info("SystemPerformanceManager is not running.");
		}
		
		return true;
	}
	
	/**
	 * Checks if the manager is currently running.
	 * 
	 * @return true if running, false otherwise
	 */
	public boolean isStarted()
	{
		return this.isStarted;
	}
}