package org.firstinspires.ftc.teamcode.utility.intakeUtility;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class RealTimeBallFilter {
    // A window of 3 elements is perfect for catching 1-2 frame spikes
    private final int windowSize = 7;
    private final Queue<Integer> window = new ArrayDeque<>();
    private int currentStableCount = 0;

    public int update(int rawReading) {
        // 1. Maintain the sliding window size
        window.add(rawReading);
        if (window.size() > windowSize) {
            window.poll();
        }

        // 2. Count the frequencies of values currently in our window safely
        Map<Integer, Integer> frequencies = new HashMap<>();
        for (int val : window) {
            // .merge() safely adds 1 to the existing value, or initializes it to 1 if it doesn't exist.
            // This prevents the NullPointerException unboxing warning entirely.
            frequencies.merge(val, 1, Integer::sum);
        }

        // 3. Find the most frequent value (the Mode)
        int mostFrequent = rawReading;
        int maxCount = 0;
        for (Map.Entry<Integer, Integer> entry : frequencies.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostFrequent = entry.getKey();
            }
        }

        // 4. Update and return our stable baseline
        if (maxCount >= 2) {
            currentStableCount = mostFrequent;
        }

        return currentStableCount;
    }
}