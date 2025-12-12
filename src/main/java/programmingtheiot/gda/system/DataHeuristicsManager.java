package programmingtheiot.gda.system;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class DataHeuristicsManager {
    private Map<Integer, Queue<Float>> dataWindows;  // typeID -> recent values
    private static final int WINDOW_SIZE = 5;  // Moving average over 5 samples
    
    public DataHeuristicsManager() {
        this.dataWindows = new HashMap<>();
    }
    
    public float applySmoothing(int typeID, float value) {
        // Get or create queue for this sensor type
        Queue<Float> window = dataWindows.computeIfAbsent(typeID, k -> new LinkedList<>());
        
        // Add new value
        window.add(value);
        
        // Keep window size limited
        if (window.size() > WINDOW_SIZE) {
            window.poll();
        }
        
        // Calculate moving average
        float sum = 0;
        for (float v : window) {
            sum += v;
        }
        
        return sum / window.size();
    }
}