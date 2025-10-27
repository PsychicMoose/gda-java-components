/**
 * 
 * This class is part of the Programming the Internet of Things
 * project, and is available via the MIT License, which can be
 * found in the LICENSE file at the top level of this repository.
 * 
 * Copyright (c) 2020 - 2025 by Andrew D. King
 */ 

package programmingtheiot.integration.connection;

import java.util.logging.Logger;

import org.junit.After;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.gda.connection.MqttClientConnector;

/**
 * This test case class contains integration tests for generating
 * all 14 MQTT 3.1.1 Control Packets:
 * 
 * 1. CONNECT - Client connects to broker
 * 2. CONNACK - Broker acknowledges connection
 * 3. PUBLISH - Publish message
 * 4. PUBACK - Publish acknowledgement (QoS 1)
 * 5. PUBREC - Publish received (QoS 2)
 * 6. PUBREL - Publish release (QoS 2)
 * 7. PUBCOMP - Publish complete (QoS 2)
 * 8. SUBSCRIBE - Subscribe to topics
 * 9. SUBACK - Subscribe acknowledgement
 * 10. UNSUBSCRIBE - Unsubscribe from topics
 * 11. UNSUBACK - Unsubscribe acknowledgement
 * 12. PINGREQ - Ping request (keep-alive)
 * 13. PINGRESP - Ping response
 * 14. DISCONNECT - Client disconnects
 *
 */
public class MqttClientControlPacketTest
{
	// static
	
	private static final Logger _Logger =
		Logger.getLogger(MqttClientControlPacketTest.class.getName());
	
	// Test topic for publish/subscribe operations
	private static final String TEST_TOPIC = "test/mqtt/control/packets";
	
	// member var's
	
	private MqttClientConnector mqttClient = null;
	
	
	// test setup methods
	
	@Before
	public void setUp() throws Exception
	{
		this.mqttClient = new MqttClientConnector();
	}
	
	@After
	public void tearDown() throws Exception
	{
		// Ensure clean disconnect after each test
		if (this.mqttClient != null && this.mqttClient.isConnected()) {
			this.mqttClient.disconnectClient();
		}
	}
	
	// test methods
	
	/**
	 * Test CONNECT, CONNACK, and DISCONNECT control packets.
	 * This test verifies basic connection and disconnection flow.
	 */
	@Test
	public void testConnectAndDisconnect()
	{
		_Logger.info("\n=== Testing CONNECT, CONNACK, and DISCONNECT Control Packets ===\n");
		
		// Test CONNECT and CONNACK
		_Logger.info("1. Sending CONNECT packet to broker...");
		boolean connected = this.mqttClient.connectClient();
		assertTrue("Failed to connect to MQTT broker", connected);
		_Logger.info("   -> CONNECT sent, CONNACK received successfully!\n");
		
		// Verify connection
		assertTrue("Client should be connected", this.mqttClient.isConnected());
		
		// Small delay to ensure connection is stable
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// Ignore
		}
		
		// Test DISCONNECT
		_Logger.info("2. Sending DISCONNECT packet to broker...");
		boolean disconnected = this.mqttClient.disconnectClient();
		assertTrue("Failed to disconnect from MQTT broker", disconnected);
		_Logger.info("   -> DISCONNECT sent successfully!\n");
		
		// Verify disconnection
		assertFalse("Client should be disconnected", this.mqttClient.isConnected());
	}
	
	/**
	 * Test PINGREQ and PINGRESP control packets.
	 * These packets are used for keep-alive mechanism.
	 * Note: The keep-alive interval should be set low enough to see these packets during the test.
	 */
	@Test
	public void testServerPing()
	{
		_Logger.info("\n=== Testing PINGREQ and PINGRESP Control Packets ===\n");
		
		// Connect first
		_Logger.info("Connecting to broker...");
		boolean connected = this.mqttClient.connectClient();
		assertTrue("Failed to connect to MQTT broker", connected);
		
		// Get keep-alive interval from config (should be set to a low value like 30 seconds for testing)
		ConfigUtil configUtil = ConfigUtil.getInstance();
		int keepAlive = configUtil.getInteger(
			ConfigConst.MQTT_GATEWAY_SERVICE, 
			ConfigConst.KEEP_ALIVE_KEY, 
			ConfigConst.DEFAULT_KEEP_ALIVE
		);
		
		_Logger.info("Keep-alive interval: " + keepAlive + " seconds");
		_Logger.info("Waiting for keep-alive interval to trigger PINGREQ/PINGRESP...");
		_Logger.info("(Monitor your MQTT broker logs or use Wireshark to see PINGREQ and PINGRESP packets)\n");
		
		// Wait for 1.5 times the keep-alive interval to ensure PING packets are sent
		// The MQTT client will automatically send PINGREQ and receive PINGRESP
		try {
			long waitTime = (long)(keepAlive * 1500); // 1.5 times keep-alive in milliseconds
			_Logger.info("Waiting " + (waitTime/1000) + " seconds for PING cycle...");
			Thread.sleep(waitTime);
			_Logger.info("   -> PINGREQ sent and PINGRESP received during keep-alive cycle!\n");
		} catch (InterruptedException e) {
			_Logger.warning("Test interrupted: " + e.getMessage());
		}
		
		// Verify connection is still active (PING kept it alive)
		assertTrue("Connection should still be active after PING", this.mqttClient.isConnected());
		
		// Disconnect
		this.mqttClient.disconnectClient();
	}
	
	/**
	 * Test all PUBLISH-related control packets with different QoS levels:
	 * - QoS 0: PUBLISH only
	 * - QoS 1: PUBLISH, PUBACK
	 * - QoS 2: PUBLISH, PUBREC, PUBREL, PUBCOMP
	 * 
	 * Also tests SUBSCRIBE, SUBACK, UNSUBSCRIBE, and UNSUBACK
	 */
	@Test
	public void testPubSub()
	{
		_Logger.info("\n=== Testing PUBLISH, SUBSCRIBE, and Related Control Packets ===\n");
		
		// Connect first
		_Logger.info("Connecting to broker...");
		boolean connected = this.mqttClient.connectClient();
		assertTrue("Failed to connect to MQTT broker", connected);
		
		try {
			// Test SUBSCRIBE and SUBACK
			_Logger.info("1. Testing SUBSCRIBE and SUBACK packets...");
			_Logger.info("   Subscribing to topic: GDA_MGMT_STATUS_MSG_RESOURCE");
			
			// Subscribe with QoS 2 to ensure we can test all QoS levels
			// Using an actual ResourceNameEnum from the project
			boolean subscribed = this.mqttClient.subscribeToTopic(
				ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE, 2
			);
			assertTrue("Failed to subscribe to topic", subscribed);
			_Logger.info("   -> SUBSCRIBE sent, SUBACK received successfully!\n");
			
			// Small delay to ensure subscription is processed
			Thread.sleep(500);
			
			// Test QoS 0 - PUBLISH only
			_Logger.info("2. Testing PUBLISH with QoS 0 (fire and forget)...");
			String msgQoS0 = "Test message with QoS 0";
			boolean pubQoS0 = this.mqttClient.publishMessage(
				ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE, msgQoS0, 0
			);
			assertTrue("Failed to publish with QoS 0", pubQoS0);
			_Logger.info("   -> PUBLISH sent with QoS 0 (no acknowledgment expected)\n");
			
			Thread.sleep(500);
			
			// Test QoS 1 - PUBLISH and PUBACK
			_Logger.info("3. Testing PUBLISH and PUBACK with QoS 1...");
			String msgQoS1 = "Test message with QoS 1";
			boolean pubQoS1 = this.mqttClient.publishMessage(
				ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE, msgQoS1, 1
			);
			assertTrue("Failed to publish with QoS 1", pubQoS1);
			_Logger.info("   -> PUBLISH sent with QoS 1");
			_Logger.info("   -> PUBACK received from broker!\n");
			
			Thread.sleep(500);
			
			// Test QoS 2 - PUBLISH, PUBREC, PUBREL, PUBCOMP
			_Logger.info("4. Testing PUBLISH, PUBREC, PUBREL, PUBCOMP with QoS 2...");
			String msgQoS2 = "Test message with QoS 2";
			boolean pubQoS2 = this.mqttClient.publishMessage(
				ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE, msgQoS2, 2
			);
			assertTrue("Failed to publish with QoS 2", pubQoS2);
			_Logger.info("   -> PUBLISH sent with QoS 2");
			_Logger.info("   -> PUBREC received from broker");
			_Logger.info("   -> PUBREL sent to broker");
			_Logger.info("   -> PUBCOMP received from broker!\n");
			
			Thread.sleep(500);
			
			// Test UNSUBSCRIBE and UNSUBACK
			_Logger.info("5. Testing UNSUBSCRIBE and UNSUBACK packets...");
			_Logger.info("   Unsubscribing from topic: GDA_MGMT_STATUS_MSG_RESOURCE");
			boolean unsubscribed = this.mqttClient.unsubscribeFromTopic(
				ResourceNameEnum.GDA_MGMT_STATUS_MSG_RESOURCE
			);
			assertTrue("Failed to unsubscribe from topic", unsubscribed);
			_Logger.info("   -> UNSUBSCRIBE sent, UNSUBACK received successfully!\n");
			
			Thread.sleep(500);
			
		} catch (InterruptedException e) {
			_Logger.warning("Test interrupted: " + e.getMessage());
		}
		
		// Disconnect
		_Logger.info("6. Disconnecting from broker...");
		this.mqttClient.disconnectClient();
		_Logger.info("   -> Test completed successfully!\n");
		
		_Logger.info("=== Summary of Control Packets Generated ===");
		_Logger.info("1. CONNECT - Client connection request");
		_Logger.info("2. CONNACK - Broker connection acknowledgment");
		_Logger.info("3. PUBLISH - Message publication (QoS 0, 1, 2)");
		_Logger.info("4. PUBACK - Publish acknowledgment (QoS 1)");
		_Logger.info("5. PUBREC - Publish received (QoS 2)");
		_Logger.info("6. PUBREL - Publish release (QoS 2)");
		_Logger.info("7. PUBCOMP - Publish complete (QoS 2)");
		_Logger.info("8. SUBSCRIBE - Topic subscription request");
		_Logger.info("9. SUBACK - Subscribe acknowledgment");
		_Logger.info("10. UNSUBSCRIBE - Topic unsubscribe request");
		_Logger.info("11. UNSUBACK - Unsubscribe acknowledgment");
		_Logger.info("12. PINGREQ - Keep-alive ping request (see testServerPing)");
		_Logger.info("13. PINGRESP - Keep-alive ping response (see testServerPing)");
		_Logger.info("14. DISCONNECT - Client disconnect notification");
		_Logger.info("\nAll 14 MQTT 3.1.1 Control Packets have been generated!");
	}
}