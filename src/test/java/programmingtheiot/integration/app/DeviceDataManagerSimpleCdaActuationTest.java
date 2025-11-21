/**
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 */

package programmingtheiot.integration.app;

import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.SensorData;
import programmingtheiot.gda.app.DeviceDataManager;

/**
 * Test class for DeviceDataManager humidity threshold crossing logic.
 * Tests the analysis functionality when receiving SensorData messages from the CDA.
 * 
 * This test verifies:
 * - Humidity threshold detection (floor and ceiling values)
 * - Time-based delay before triggering actuation
 * - Proper generation of ActuatorData commands
 * - ON/OFF command logic for humidifier control
 */
public class DeviceDataManagerSimpleCdaActuationTest
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(DeviceDataManagerSimpleCdaActuationTest.class.getName());
	
	// member var's
	
	private DeviceDataManager deviceDataManager = null;
	
	// test setup methods
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		_Logger.info("Setting up test class...");
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
		_Logger.info("Tearing down test class...");
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception
	{
		_Logger.info("Setting up test case...");
		this.deviceDataManager = new DeviceDataManager();
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception
	{
		_Logger.info("Tearing down test case...");
		if (this.deviceDataManager != null) {
			this.deviceDataManager.stopManager();
		}
	}
	
	// test methods
	
	/**
	 * Test method for running the DeviceDataManager humidity actuation logic.
	 * This tests the threshold crossing detection and actuation event generation.
	 * 
	 * Test scenarios:
	 * 1. Normal humidity values - no actuation
	 * 2. Low humidity condition - triggers humidifier ON
	 * 3. Return to nominal - triggers humidifier OFF  
	 * 4. High humidity condition - triggers humidifier OFF
	 */
	@Test
	public void testSendActuationEventsToCda()
	{
		_Logger.info("====================================================");
		_Logger.info("Starting humidity threshold crossing actuation test");
		_Logger.info("====================================================");
		
		// Start the DeviceDataManager
		// NOTE: Be sure your PiotConfig.props is setup properly
		this.deviceDataManager.startManager();
		
		// Get configuration values
		ConfigUtil cfgUtil = ConfigUtil.getInstance();
		
		float nominalVal = cfgUtil.getFloat(ConfigConst.GATEWAY_DEVICE, "nominalHumiditySetting");
		float lowVal     = cfgUtil.getFloat(ConfigConst.GATEWAY_DEVICE, "triggerHumidifierFloor");
		float highVal    = cfgUtil.getFloat(ConfigConst.GATEWAY_DEVICE, "triggerHumidifierCeiling");
		int   delay      = cfgUtil.getInteger(ConfigConst.GATEWAY_DEVICE, "humidityMaxTimePastThreshold");
		
		// For testing, override with shorter delay if configured delay is too long
		if (delay > 10) {
			_Logger.info("Overriding configured delay of " + delay + " seconds to 10 seconds for testing");
			delay = 10;
		}
		
		_Logger.info("Test Configuration:");
		_Logger.info("  Nominal Humidity: " + nominalVal + "%");
		_Logger.info("  Floor Threshold:  " + lowVal + "%");
		_Logger.info("  Ceiling Threshold: " + highVal + "%");  
		_Logger.info("  Time Threshold:   " + delay + " seconds");
		_Logger.info("====================================================");
		
		// Test Sequence No. 1: Low humidity condition
		_Logger.info("\n>>> Test Sequence 1: Low Humidity Condition <<<");
		_Logger.info("Expected: Humidifier should turn ON when humidity stays below floor for " + delay + " seconds");
		generateAndProcessHumiditySensorDataSequence(
			this.deviceDataManager, nominalVal, lowVal, highVal, delay);
		
		// Wait between test sequences
		waitForSeconds(5);
		
		// Test Sequence No. 2: High humidity condition
		_Logger.info("\n>>> Test Sequence 2: High Humidity Condition <<<");
		_Logger.info("Expected: Humidifier should turn OFF when humidity exceeds ceiling for " + delay + " seconds");
		generateAndProcessHighHumiditySequence(
			this.deviceDataManager, nominalVal, highVal, delay);
		
		// Test Sequence No. 3: Rapid fluctuation (should not trigger)
		_Logger.info("\n>>> Test Sequence 3: Rapid Fluctuation (No Trigger Expected) <<<");
		_Logger.info("Expected: No actuation when humidity returns to normal before timeout");
		generateAndProcessRapidFluctuationSequence(
			this.deviceDataManager, nominalVal, lowVal, delay);
		
		// Allow final processing time
		waitForSeconds(3);
		
		_Logger.info("\n====================================================");
		_Logger.info("Humidity threshold crossing actuation test complete");
		_Logger.info("====================================================");
	}
	
	/**
	 * Generate and process a sequence of humidity sensor data for low humidity testing.
	 * This simulates a scenario where humidity drops below the floor threshold.
	 */
	private void generateAndProcessHumiditySensorDataSequence(
		DeviceDataManager ddm, float nominalVal, float lowVal, float highVal, int delay)
	{
		SensorData sd = new SensorData();
		sd.setName("HumiditySensor");
		sd.setLocationID("constraineddevice001");
		sd.setTypeID(ConfigConst.HUMIDITY_SENSOR_TYPE);
		
		// Step 1: Send nominal values first (no action expected)
		_Logger.info("Step 1: Sending nominal humidity: " + nominalVal + "% (no action expected)");
		sd.setValue(nominalVal);
		ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
		waitForSeconds(2);
		
		_Logger.info("Step 2: Sending nominal humidity again: " + nominalVal + "%");
		sd.setValue(nominalVal);
		ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
		waitForSeconds(2);
		
		// Step 2: Send low value (below floor) - starts timer
		float lowTestVal = lowVal - 5;
		_Logger.info("Step 3: Sending LOW humidity: " + lowTestVal + "% (below floor, starting timer)");
		sd.setValue(lowTestVal);
		ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
		
		_Logger.info("Waiting " + (delay + 1) + " seconds for threshold timeout...");
		waitForSeconds(delay + 1);
		
		// Step 3: Send another low value after delay - should trigger ON command
		lowTestVal = lowVal - 3;
		_Logger.info("Step 4: Sending LOW humidity: " + lowTestVal + "% (should trigger humidifier ON)");
		sd.setValue(lowTestVal);
		ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
		waitForSeconds(2);
		
		// Step 4: Humidity improving but still below nominal
		float midVal = (lowVal + nominalVal) / 2;
		_Logger.info("Step 5: Humidity improving: " + midVal + "% (humidifier should stay ON)");
		sd.setValue(midVal);
		ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
		waitForSeconds(2);
		
		// Step 5: Return to nominal - should trigger OFF command
		_Logger.info("Step 6: Humidity at nominal: " + nominalVal + "% (should trigger humidifier OFF)");
		sd.setValue(nominalVal);
		ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
		waitForSeconds(2);
	}
	
	/**
	 * Generate and process a sequence of humidity sensor data for high humidity testing.
	 * This simulates a scenario where humidity rises above the ceiling threshold.
	 */
	private void generateAndProcessHighHumiditySequence(
		DeviceDataManager ddm, float nominalVal, float highVal, int delay)
	{
		SensorData sd = new SensorData();
		sd.setName("HumiditySensor");
		sd.setLocationID("constraineddevice001");
		sd.setTypeID(ConfigConst.HUMIDITY_SENSOR_TYPE);
		
		// Reset to nominal first
		_Logger.info("Step 1: Resetting to nominal humidity: " + nominalVal + "%");
		sd.setValue(nominalVal);
		ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
		waitForSeconds(2);
		
		// Send high value (above ceiling) - starts timer
		float highTestVal = highVal + 5;
		_Logger.info("Step 2: Sending HIGH humidity: " + highTestVal + "% (above ceiling, starting timer)");
		sd.setValue(highTestVal);
		ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
		
		_Logger.info("Waiting " + (delay + 1) + " seconds for threshold timeout...");
		waitForSeconds(delay + 1);
		
		// Send another high value after delay - should trigger OFF command
		highTestVal = highVal + 3;
		_Logger.info("Step 3: Sending HIGH humidity: " + highTestVal + "% (should trigger humidifier OFF)");
		sd.setValue(highTestVal);
		ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
		waitForSeconds(2);
		
		// Return to nominal
		_Logger.info("Step 4: Returning to nominal humidity: " + nominalVal + "%");
		sd.setValue(nominalVal);
		ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
		waitForSeconds(2);
	}
	
	/**
	 * Generate rapid fluctuation sequence that should NOT trigger actuation.
	 * This tests the time threshold logic - actuation should only occur
	 * if the condition persists for the configured time period.
	 */
	private void generateAndProcessRapidFluctuationSequence(
		DeviceDataManager ddm, float nominalVal, float lowVal, int delay)
	{
		SensorData sd = new SensorData();
		sd.setName("HumiditySensor");
		sd.setLocationID("constraineddevice001");
		sd.setTypeID(ConfigConst.HUMIDITY_SENSOR_TYPE);
		
		// Start with nominal
		_Logger.info("Step 1: Starting at nominal humidity: " + nominalVal + "%");
		sd.setValue(nominalVal);
		ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
		waitForSeconds(2);
		
		// Drop below floor briefly
		float lowTestVal = lowVal - 5;
		_Logger.info("Step 2: Brief drop to LOW humidity: " + lowTestVal + "% (starting timer)");
		sd.setValue(lowTestVal);
		ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
		
		// Wait less than threshold time
		int shortDelay = Math.min(delay - 1, 3);
		_Logger.info("Waiting only " + shortDelay + " seconds (less than threshold)...");
		waitForSeconds(shortDelay);
		
		// Return to normal before threshold timeout
		_Logger.info("Step 3: Quick return to nominal: " + nominalVal + "% (no actuation should occur)");
		sd.setValue(nominalVal);
		ddm.handleSensorMessage(ResourceNameEnum.CDA_SENSOR_MSG_RESOURCE, sd);
		waitForSeconds(2);
		
		_Logger.info("Result: No actuation command should have been sent");
	}
	
	/**
	 * Helper method to pause execution for a specified number of seconds.
	 * Provides visual feedback during the wait.
	 * 
	 * @param seconds Number of seconds to wait
	 */
	private void waitForSeconds(int seconds)
	{
		try {
			for (int i = 0; i < seconds; i++) {
				System.out.print(".");
				Thread.sleep(1000);
			}
			System.out.println();
		} catch (InterruptedException e) {
			// ignore
		}
	}
}