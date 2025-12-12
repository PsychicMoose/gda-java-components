package programmingtheiot.gda.system;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import programmingtheiot.data.ActuatorData;
import programmingtheiot.data.SensorData;
import programmingtheiot.data.SystemPerformanceData;

public class DataPersistenceManager {
    private static final Logger logger = Logger.getLogger(DataPersistenceManager.class.getName());
    
    private static final String LOG_FILE = "data/panopticon_log.jsonl";  // JSONL = JSON Lines
    private Gson gson;
    private PrintWriter writer;
    
    public DataPersistenceManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        initLogFile();
    }
    
    private void initLogFile() {
        try {
            // Create directory if it doesn't exist
            new java.io.File("data").mkdirs();
            
            // Open file in append mode
            this.writer = new PrintWriter(new FileWriter(LOG_FILE, true), true);
            logger.info("Data persistence initialized: " + LOG_FILE);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to initialize persistence log file", e);
        }
    }
    
    public void logSensorData(SensorData data) {
        if (writer != null) {
            try {
                String json = gson.toJson(data);
                writer.println(json);
                logger.fine("Persisted sensor data: " + data.getName());
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to persist sensor data", e);
            }
        }
    }
    
    public void logActuatorData(ActuatorData data) {
        if (writer != null) {
            try {
                String json = gson.toJson(data);
                writer.println(json);
                logger.fine("Persisted actuator data");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to persist actuator data", e);
            }
        }
    }
    
    public void logSystemPerformanceData(SystemPerformanceData data) {
        if (writer != null) {
            try {
                String json = gson.toJson(data);
                writer.println(json);
                logger.fine("Persisted system performance data");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to persist system perf data", e);
            }
        }
    }
    
    public void close() {
        if (writer != null) {
            writer.close();
            logger.info("Data persistence closed");
        }
    }
}