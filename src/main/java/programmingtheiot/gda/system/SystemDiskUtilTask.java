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

import java.io.File;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;

/**
 * SystemDiskUtilTask monitors disk utilization for the system.
 * It calculates the percentage of used disk space for the configured disk path.
 */
public class SystemDiskUtilTask extends BaseSystemUtilTask
{
	// private var's
	private static final Logger _Logger = Logger.getLogger(SystemDiskUtilTask.class.getName());
	
	private String diskPath = null;
	private File diskFile = null;
	
	
	// constructors
	
	/**
	 * Default constructor.
	 * Initializes the disk path from configuration or uses the root path.
	 */
	public SystemDiskUtilTask()
	{
		super("DiskUtil", 0);
		
		// Try to get disk path from configuration
		// Using the string directly to avoid dependency on ConfigConst
		try {
			this.diskPath = ConfigUtil.getInstance().getProperty(
				ConfigConst.GATEWAY_DEVICE, 
				"diskUtilPath",
				"/"  // Default to root path
			);
		} catch (Exception e) {
			// If ConfigUtil fails, just use default
			this.diskPath = "/";
		}
		
		// If Windows, default to C:\ if not configured
		if (System.getProperty("os.name").toLowerCase().contains("windows") && 
			this.diskPath.equals("/")) {
			this.diskPath = "C:\\";
		}
		
		this.diskFile = new File(this.diskPath);
		
		if (!this.diskFile.exists()) {
			_Logger.warning("Disk path does not exist: " + this.diskPath + ". Using current directory.");
			this.diskFile = new File(".");
		}
		
		_Logger.info("SystemDiskUtilTask initialized for path: " + this.diskFile.getAbsolutePath());
	}
	
	
	// protected methods
	
	/**
	 * Template method implementation to retrieve disk utilization.
	 * 
	 * @return float The disk utilization percentage (0.0 to 100.0)
	 */
	@Override
	protected float getSystemUtil()
	{
		float diskUtil = 0.0f;
		
		try {
			// Method 1: Using File class (simpler, cross-platform)
			long totalSpace = this.diskFile.getTotalSpace();
			long freeSpace = this.diskFile.getFreeSpace();
			
			if (totalSpace > 0) {
				long usedSpace = totalSpace - freeSpace;
				diskUtil = ((float) usedSpace / (float) totalSpace) * 100.0f;
				
				_Logger.fine("Disk utilization for " + this.diskFile.getAbsolutePath() + 
					": Total=" + formatBytes(totalSpace) + 
					", Free=" + formatBytes(freeSpace) + 
					", Used=" + formatBytes(usedSpace) + 
					" (" + String.format("%.2f", diskUtil) + "%)");
			}
			
		} catch (SecurityException e) {
			_Logger.warning("Security exception accessing disk information: " + e.getMessage());
			
			// Fallback: Try using NIO FileStore
			try {
				diskUtil = getDiskUtilUsingNIO();
			} catch (Exception ex) {
				_Logger.severe("Failed to get disk utilization: " + ex.getMessage());
			}
			
		} catch (Exception e) {
			_Logger.severe("Error getting disk utilization: " + e.getMessage());
		}
		
		// Ensure the value is within valid range
		if (diskUtil < 0.0f) {
			diskUtil = 0.0f;
		} else if (diskUtil > 100.0f) {
			diskUtil = 100.0f;
		}
		
		return diskUtil;
	}
	
	/**
	 * Alternative method using NIO FileStore for disk utilization.
	 * This provides more detailed filesystem information.
	 * 
	 * @return float The disk utilization percentage
	 * @throws Exception if unable to access filesystem information
	 */
	private float getDiskUtilUsingNIO() throws Exception
	{
		float diskUtil = 0.0f;
		
		Path path = Paths.get(this.diskPath);
		FileStore fileStore = Files.getFileStore(path);
		
		long totalSpace = fileStore.getTotalSpace();
		long usableSpace = fileStore.getUsableSpace();
		
		if (totalSpace > 0) {
			long usedSpace = totalSpace - usableSpace;
			diskUtil = ((float) usedSpace / (float) totalSpace) * 100.0f;
			
			_Logger.fine("Disk utilization (NIO) for " + fileStore.name() + 
				": Total=" + formatBytes(totalSpace) + 
				", Usable=" + formatBytes(usableSpace) + 
				", Used=" + formatBytes(usedSpace) + 
				" (" + String.format("%.2f", diskUtil) + "%)");
		}
		
		return diskUtil;
	}
	
	/**
	 * Helper method to format bytes into human-readable format.
	 * 
	 * @param bytes The number of bytes to format
	 * @return String representation of bytes (e.g., "1.5 GB")
	 */
	private String formatBytes(long bytes)
	{
		if (bytes < 1024) {
			return bytes + " B";
		}
		
		int exp = (int) (Math.log(bytes) / Math.log(1024));
		String unit = "KMGTPE".charAt(exp - 1) + "B";
		
		return String.format("%.1f %s", bytes / Math.pow(1024, exp), unit);
	}
	
	/**
	 * Gets overall disk utilization across all file systems.
	 * This method can be used to get an aggregate view of all mounted filesystems.
	 * 
	 * @return float The average disk utilization across all filesystems
	 */
	public float getAllFileSystemsUtilization()
	{
		float totalUtil = 0.0f;
		int count = 0;
		
		try {
			Iterable<FileStore> fileStores = FileSystems.getDefault().getFileStores();
			
			for (FileStore store : fileStores) {
				try {
					long total = store.getTotalSpace();
					long usable = store.getUsableSpace();
					
					if (total > 0) {
						float util = ((float)(total - usable) / (float)total) * 100.0f;
						totalUtil += util;
						count++;
						
						_Logger.fine("FileStore " + store.name() + " utilization: " + 
							String.format("%.2f", util) + "%");
					}
				} catch (Exception e) {
					_Logger.warning("Could not access FileStore " + store.name() + ": " + e.getMessage());
				}
			}
			
			if (count > 0) {
				return totalUtil / count;
			}
			
		} catch (Exception e) {
			_Logger.severe("Error getting all filesystems utilization: " + e.getMessage());
		}
		
		return 0.0f;
	}
}