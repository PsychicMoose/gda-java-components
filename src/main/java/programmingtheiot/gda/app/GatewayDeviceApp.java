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

package programmingtheiot.gda.app;

import org.apache.commons.cli.*;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main GDA application.
 * Manages the lifecycle of the DeviceDataManager and coordinates
 * all Gateway Device Application functionality.
 */
public class GatewayDeviceApp
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(GatewayDeviceApp.class.getName());
	
	public static final long DEFAULT_TEST_RUNTIME = 60000L;
	
	// private var's
	private DeviceDataManager dataMgr = null;
	private String configFile = ConfigConst.DEFAULT_CONFIG_FILE_NAME;

	// constructors
	
	/**
	 * Default constructor.
	 * Initializes the Gateway Device Application.
	 */
	public GatewayDeviceApp()
	{
		super();
		
		_Logger.info("Initializing GDA...");
	}
	
	/**
	 * Constructor with command line arguments.
	 * 
	 * @param args Command line arguments (currently unused but reserved for future use)
	 */
	public GatewayDeviceApp(String[] args)
	{
		super();
		
		_Logger.info("Initializing GDA with args...");
		
		// Process any command line arguments if needed
		// Currently args are processed in main() before creating the app
	}
	
	
	// static
	
	/**
	 * Main application entry point.
	 * 
	 * @param args Command line arguments
	 */
	public static void main(String[] args)
	{
		// Parse command line arguments
		Map<String, String> argMap = parseArgs(args);

		// Set config file if specified
		if (argMap.containsKey(ConfigConst.CONFIG_FILE_KEY)) {
			System.setProperty(ConfigConst.CONFIG_FILE_KEY, argMap.get(ConfigConst.CONFIG_FILE_KEY));
			_Logger.info("Using config file: " + argMap.get(ConfigConst.CONFIG_FILE_KEY));
		}

		// Create and start the application
		GatewayDeviceApp gwApp = new GatewayDeviceApp();
		
		gwApp.startApp();
		
		// Check if app should run forever or for a fixed duration
		boolean runForever =
			ConfigUtil.getInstance().getBoolean(
				ConfigConst.GATEWAY_DEVICE, ConfigConst.ENABLE_RUN_FOREVER_KEY);
		
		if (runForever) {
			_Logger.info("GDA running in continuous mode. Press Ctrl+C to stop.");
			
			try {
				// Keep the application running until interrupted
				while (true) {
					Thread.sleep(2000L);
				}
			} catch (InterruptedException e) {
				_Logger.info("GDA interrupted. Shutting down...");
			}
			
			gwApp.stopApp(0);
		} else {
			// Run for a fixed duration
			long runtime = DEFAULT_TEST_RUNTIME;
			
			try {
				// Try to get runtime from config
				int configRuntime = ConfigUtil.getInstance().getInteger(
					ConfigConst.GATEWAY_DEVICE, "testRuntime", (int)(DEFAULT_TEST_RUNTIME / 1000));
				runtime = configRuntime * 1000L;
			} catch (Exception e) {
				// Use default runtime if not configured
			}
			
			_Logger.info("GDA running for " + (runtime / 1000) + " seconds...");
			
			try {
				Thread.sleep(runtime);
			} catch (InterruptedException e) {
				_Logger.info("GDA interrupted. Shutting down...");
			}
			
			gwApp.stopApp(0);
		}
	}
	
	/**
	 * Parse any arguments passed in on app startup.
	 * <p>
	 * This method checks if any valid command line args are provided,
	 * including the name of the config file.
	 * 
	 * @param args The non-null and non-empty args array.
	 * @return Map containing parsed arguments
	 */
	private static Map<String, String> parseArgs(String[] args)
	{
		// Store command line values in a map
		Map<String, String> argMap = new HashMap<String, String>();
		
		if (args != null && args.length > 0)  {
			_Logger.info("Parsing " + args.length + " command line arguments...");
			
			// Create the parser and options
			CommandLineParser parser = new DefaultParser();
			Options options = new Options();

			// Add supported options
			options.addOption("c", "config", true, "The relative or absolute path of the config file.");
			options.addOption("h", "help", false, "Display help information.");

			try {
				CommandLine cmdLineArgs = parser.parse(options, args);

				// Check for help option
				if (cmdLineArgs.hasOption("h")) {
					HelpFormatter formatter = new HelpFormatter();
					formatter.printHelp("GatewayDeviceApp", options);
					System.exit(0);
				}

				// Check for config file option
				if (cmdLineArgs.hasOption("c")) {
					String configPath = cmdLineArgs.getOptionValue("c");
					argMap.put(ConfigConst.CONFIG_FILE_KEY, configPath);
					_Logger.info("Config file specified: " + configPath);
				} else {
					_Logger.info("No custom config file specified. Using default.");
				}
			} catch (ParseException e) {
				_Logger.warning("Failed to parse command line args: " + e.getMessage() + 
					". Using defaults.");
			}
		} else {
			_Logger.info("No command line args to parse.");
		}

		return argMap;
	}
	
	
	// public methods
	
	/**
	 * Initializes and starts the application.
	 * Creates and starts the DeviceDataManager unless running in test empty app mode.
	 */
	public void startApp()
	{
		_Logger.info("Starting GDA...");
		
		try {
			// Check if we should run an empty app (for testing)
			boolean testEmptyApp = ConfigUtil.getInstance().getBoolean(
				ConfigConst.GATEWAY_DEVICE, ConfigConst.TEST_EMPTY_APP_KEY);
			
			if (!testEmptyApp) {
				// Create DeviceDataManager - it will handle SystemPerformanceManager internally
				this.dataMgr = new DeviceDataManager();
				
				if (this.dataMgr != null) {
					// Start the DeviceDataManager
					this.dataMgr.startManager();
					_Logger.info("DeviceDataManager started successfully.");
				} else {
					_Logger.severe("Failed to create DeviceDataManager!");
					stopApp(-1);
				}
			} else {
				_Logger.info("Running in empty app test mode - no managers will be started.");
			}
			
			_Logger.info("GDA started successfully.");
			
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to start GDA. Exiting.", e);
			stopApp(-1);
		}
	}
	
	/**
	 * Stops the application.
	 * Stops the DeviceDataManager and exits with the specified code.
	 * 
	 * @param code The exit code to pass to System.exit()
	 */
	public void stopApp(int code)
	{
		_Logger.info("Stopping GDA...");
		
		try {
			// Stop DeviceDataManager if it exists
			if (this.dataMgr != null) {
				this.dataMgr.stopManager();
				_Logger.info("DeviceDataManager stopped successfully.");
			}
			
			_Logger.log(Level.INFO, "GDA stopped successfully with exit code {0}.", code);
			
		} catch (Exception e) {
			_Logger.log(Level.SEVERE, "Failed to cleanly stop GDA. Exiting.", e);
		}
		
		// Exit with the specified code
		System.exit(code);
	}
	
	
	// private methods
	
	// Currently no private methods needed
}