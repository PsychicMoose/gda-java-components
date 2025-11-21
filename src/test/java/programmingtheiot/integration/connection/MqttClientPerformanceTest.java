package programmingtheiot.integration.connection;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import programmingtheiot.common.ConfigConst;
import programmingtheiot.common.ConfigUtil;
import programmingtheiot.common.ResourceNameEnum;
import programmingtheiot.data.DataUtil;
import programmingtheiot.data.SensorData;
import programmingtheiot.gda.connection.MqttClientConnector;

public class MqttClientPerformanceTest
{
    private static final Logger _Logger =
        Logger.getLogger(MqttClientPerformanceTest.class.getName());
    
    public static final int MAX_TEST_RUNS = 10000;
    
    private MqttClientConnector mqttClient = null;
    private static PrintWriter resultsWriter = null;
    private static String resultsFileName = null;
    
    @BeforeClass
    public static void setUpClass() throws Exception
    {
        // Reduce console logging to only SEVERE
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.SEVERE);
        
        // Create results file with timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        resultsFileName = "mqtt_gda_performance_" + timestamp + ".txt";
        resultsWriter = new PrintWriter(new FileWriter(resultsFileName));
        
        // Write header
        resultsWriter.println("=================================================");
        resultsWriter.println("GDA MQTT Performance Test Results");
        resultsWriter.println("Timestamp: " + LocalDateTime.now());
        resultsWriter.println("Max Test Runs: " + MAX_TEST_RUNS);
        
        // Check if TLS is enabled
        ConfigUtil config = ConfigUtil.getInstance();
        boolean tlsEnabled = config.getBoolean(ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.ENABLE_CRYPT_KEY);
        int port = tlsEnabled ? 
            config.getInteger(ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.SECURE_PORT_KEY) :
            config.getInteger(ConfigConst.MQTT_GATEWAY_SERVICE, ConfigConst.PORT_KEY);
            
        resultsWriter.println("TLS Enabled: " + tlsEnabled);
        resultsWriter.println("Port: " + port);
        resultsWriter.println("=================================================");
        resultsWriter.flush();
        
        System.out.println("Performance test started. Results will be written to: " + resultsFileName);
        System.out.println("TLS: " + (tlsEnabled ? "ENABLED" : "DISABLED") + " | Port: " + port);
    }
    
    @Before
    public void setUp() throws Exception
    {
        this.mqttClient = new MqttClientConnector();
    }
    
    @After
    public void tearDown() throws Exception
    {
        if (this.mqttClient != null && this.mqttClient.isConnected()) {
            this.mqttClient.disconnectClient();
        }
    }
    
    @Test
    public void testConnectAndDisconnect()
    {
        System.out.print("Running Connect/Disconnect test... ");
        
        long startMillis = System.currentTimeMillis();
        
        assertTrue(this.mqttClient.connectClient());
        assertTrue(this.mqttClient.disconnectClient());
        
        long endMillis = System.currentTimeMillis();
        long elapsedMillis = endMillis - startMillis;
        
        resultsWriter.println("\nConnect/Disconnect Test:");
        resultsWriter.println("  Time: " + elapsedMillis + " ms");
        resultsWriter.flush();
        
        System.out.println("Done (" + elapsedMillis + " ms)");
    }
    
    @Test
    public void testPublishQoS0()
    {
        System.out.print("Running QoS 0 test (" + MAX_TEST_RUNS + " messages)... ");
        execTestPublish(MAX_TEST_RUNS, 0);
    }
    
    @Test
    public void testPublishQoS1()
    {
        System.out.print("Running QoS 1 test (" + MAX_TEST_RUNS + " messages)... ");
        execTestPublish(MAX_TEST_RUNS, 1);
    }
    
    @Test
    public void testPublishQoS2()
    {
        System.out.print("Running QoS 2 test (" + MAX_TEST_RUNS + " messages)... ");
        execTestPublish(MAX_TEST_RUNS, 2);
    }
    
    private void execTestPublish(int maxTestRuns, int qos)
    {
        assertTrue(this.mqttClient.connectClient());
        
        SensorData sensorData = new SensorData();
        String payload = DataUtil.getInstance().sensorDataToJson(sensorData);
        int payloadLen = payload.length();
        
        long startMillis = System.currentTimeMillis();
        
        // Show progress for long-running tests
        for (int sequenceNo = 1; sequenceNo <= maxTestRuns; sequenceNo++) {
            this.mqttClient.publishMessage(ResourceNameEnum.CDA_MGMT_STATUS_CMD_RESOURCE, payload, qos);
            
            // Show progress every 2000 messages
            if (sequenceNo % 2000 == 0) {
                System.out.print(".");
            }
        }
        
        long endMillis = System.currentTimeMillis();
        long elapsedMillis = endMillis - startMillis;
        
        assertTrue(this.mqttClient.disconnectClient());
        
        // Calculate statistics
        double avgPerMsg = (double) elapsedMillis / maxTestRuns;
        double msgsPerSecond = (maxTestRuns * 1000.0) / elapsedMillis;
        
        // Write to file
        resultsWriter.println("\nQoS " + qos + " Test:");
        resultsWriter.println("  Messages: " + maxTestRuns);
        resultsWriter.println("  Payload size: " + payloadLen + " bytes");
        resultsWriter.println("  Total time: " + elapsedMillis + " ms");
        resultsWriter.println("  Avg per message: " + String.format("%.3f", avgPerMsg) + " ms");
        resultsWriter.println("  Messages per second: " + String.format("%.1f", msgsPerSecond));
        resultsWriter.flush();
        
        System.out.println(" Done (" + elapsedMillis + " ms)");
    }
    
    @org.junit.AfterClass
    public static void tearDownClass() throws Exception
    {
        if (resultsWriter != null) {
            resultsWriter.println("\n=================================================");
            resultsWriter.println("Test completed at: " + LocalDateTime.now());
            resultsWriter.println("=================================================");
            resultsWriter.close();
            
            System.out.println("\nAll tests complete. Results saved to: " + resultsFileName);
        }
    }
}