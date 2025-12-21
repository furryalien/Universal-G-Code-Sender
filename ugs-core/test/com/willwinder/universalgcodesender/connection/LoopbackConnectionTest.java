/*
    Copyright 2025 Will Winder

    This file is part of Universal Gcode Sender (UGS).

    UGS is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    UGS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with UGS.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.willwinder.universalgcodesender.connection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Tests for loopback connection that enables testing without physical devices.
 * 
 * @author wwinder
 */
public class LoopbackConnectionTest {
    
    private LoopbackConnection connection;
    private TestConnectionListener listener;
    
    @Before
    public void setUp() {
        connection = new LoopbackConnection();
        listener = new TestConnectionListener();
        connection.addListener(listener);
    }
    
    @After
    public void tearDown() throws Exception {
        if (connection != null && connection.isOpen()) {
            connection.closePort();
        }
    }
    
    @Test
    public void testOpenAndClose() throws Exception {
        connection.setUri("loopback://echo");
        assertFalse("Connection should start closed", connection.isOpen());
        
        boolean opened = connection.openPort();
        assertTrue("Connection should open successfully", opened);
        assertTrue("Connection should be open", connection.isOpen());
        
        connection.closePort();
        assertFalse("Connection should be closed", connection.isOpen());
    }
    
    @Test(expected = ConnectionException.class)
    public void testOpenTwice_ShouldThrow() throws Exception {
        connection.setUri("loopback://echo");
        connection.openPort();
        connection.openPort(); // Should throw
    }
    
    @Test
    public void testEchoMode() throws Exception {
        connection.setUri("loopback://echo");
        connection.openPort();
        
        String testCommand = "G0 X10 Y20\n";
        connection.sendStringToComm(testCommand);
        
        String response = listener.waitForResponse(1000);
        assertEquals("Echo mode should return exact command", "G0 X10 Y20", response);  // ConnectionListenerManager strips \n
    }
    
    @Test
    public void testGrblMode_OkResponse() throws Exception {
        connection.setUri("loopback://grbl");
        connection.openPort();
        
        // Wait for welcome message
        listener.waitForResponse(500);
        listener.clearResponses();
        
        connection.sendStringToComm("G0 X10\n");
        
        String response = listener.waitForResponse(1000);
        assertTrue("GRBL mode should respond with 'ok'", response.contains("ok"));
    }
    
    @Test
    public void testGrblMode_StatusRequest() throws Exception {
        connection.setUri("loopback://grbl");
        connection.openPort();
        
        // Clear welcome message
        listener.waitForResponse(500);
        listener.clearResponses();
        
        connection.sendStringToComm("?\n");
        
        String response = listener.waitForResponse(1000);
        assertTrue("GRBL status should start with <", response.startsWith("<"));
        assertTrue("GRBL status should end with >", response.contains(">"));
        assertTrue("GRBL status should contain state", response.contains("Idle") || response.contains("Run"));
    }
    
    @Test
    public void testGrblMode_SettingsRequest() throws Exception {
        connection.setUri("loopback://grbl");
        connection.openPort();
        
        // Clear welcome message
        listener.waitForResponse(500);
        listener.clearResponses();
        
        connection.sendStringToComm("$$\n");
        
        // $$ returns multiple lines ($0=10, $1=25, etc, then ok)
        // Wait for a bit then check the accumulated buffer
        Thread.sleep(200);
        String allResponses = listener.responseBuffer.toString();
        assertTrue("GRBL settings should contain $", allResponses.contains("$"));
        assertTrue("GRBL settings should end with ok", allResponses.contains("ok"));
    }
    
    @Test
    public void testGrblMode_VersionRequest() throws Exception {
        connection.setUri("loopback://grbl");
        connection.openPort();
        
        // Clear welcome message
        listener.waitForResponse(500);
        listener.clearResponses();
        
        connection.sendStringToComm("$I\n");
        
        // $I returns multiple lines ([VER:...], [OPT:...], ok)
        Thread.sleep(200);
        String allResponses = listener.responseBuffer.toString();
        assertTrue("GRBL version should contain VER", allResponses.contains("VER") || allResponses.contains("OPT"));
    }
    
    @Test
    public void testTinyGMode_JsonResponse() throws Exception {
        connection.setUri("loopback://tinyg");
        connection.openPort();
        
        // Wait for initial message
        listener.waitForResponse(500);
        listener.clearResponses();
        
        connection.sendStringToComm("G0 X10\n");
        
        String response = listener.waitForResponse(1000);
        assertTrue("TinyG mode should respond with JSON", response.startsWith("{"));
        assertTrue("TinyG response should be valid JSON", response.contains("\"r\"") && response.contains("\"f\""));
    }
    
    @Test
    public void testTinyGMode_StatusRequest() throws Exception {
        connection.setUri("loopback://tinyg");
        connection.openPort();
        
        // Clear initial message
        listener.waitForResponse(500);
        listener.clearResponses();
        
        connection.sendStringToComm("?\n");
        
        String response = listener.waitForResponse(1000);
        assertTrue("TinyG status should be JSON", response.startsWith("{"));
        assertTrue("TinyG status should contain sr", response.contains("\"sr\""));
    }
    
    @Test
    public void testCustomMode_WithResponse() throws Exception {
        connection.setUri("loopback://custom?response=CUSTOM_OK\\n");
        connection.openPort();
        
        connection.sendStringToComm("TEST\n");
        
        String response = listener.waitForResponse(1000);
        assertEquals("Custom mode should return configured response", "CUSTOM_OK", response);  // ConnectionListenerManager strips \n
    }
    
    @Test
    public void testMultipleCommands() throws Exception {
        connection.setUri("loopback://grbl");
        connection.openPort();
        
        // Clear welcome message
        listener.waitForResponse(500);
        listener.clearResponses();
        
        for (int i = 0; i < 5; i++) {
            connection.sendStringToComm("G0 X" + i + "\n");
            String response = listener.waitForResponse(1000);
            assertTrue("Each command should get 'ok' response", response.contains("ok"));
        }
    }
    
    @Test
    public void testGetDevices() {
        List<? extends IConnectionDevice> devices = connection.getDevices();
        
        assertNotNull("Device list should not be null", devices);
        assertFalse("Device list should not be empty", devices.isEmpty());
        
        boolean foundEcho = devices.stream().anyMatch(d -> d.getAddress().equals("echo"));
        boolean foundGrbl = devices.stream().anyMatch(d -> d.getAddress().equals("grbl"));
        boolean foundTinyG = devices.stream().anyMatch(d -> d.getAddress().equals("tinyg"));
        
        assertTrue("Should have echo device", foundEcho);
        assertTrue("Should have grbl device", foundGrbl);
        assertTrue("Should have tinyg device", foundTinyG);
    }
    
    @Test
    public void testResponseDelay() throws Exception {
        connection.setUri("loopback://echo");
        connection.setResponseDelay(100); // 100ms delay
        connection.openPort();
        
        long startTime = System.currentTimeMillis();
        connection.sendStringToComm("TEST\n");
        listener.waitForResponse(1000);
        long elapsed = System.currentTimeMillis() - startTime;
        
        assertTrue("Response should have delay", elapsed >= 100);
    }
    
    @Test
    public void testZeroDelay() throws Exception {
        connection.setUri("loopback://echo");
        connection.setResponseDelay(0); // Instant response
        connection.openPort();
        
        long startTime = System.currentTimeMillis();
        connection.sendStringToComm("TEST\n");
        listener.waitForResponse(1000);
        long elapsed = System.currentTimeMillis() - startTime;
        
        assertTrue("Response should be fast", elapsed < 50);
    }
    
    @Test
    public void testGetMode() {
        connection.setUri("loopback://grbl");
        assertEquals("Mode should be GRBL", LoopbackConnection.SimulatorMode.GRBL, connection.getMode());
        
        connection.setUri("loopback://tinyg");
        assertEquals("Mode should be TINYG", LoopbackConnection.SimulatorMode.TINYG, connection.getMode());
        
        connection.setUri("loopback://echo");
        assertEquals("Mode should be ECHO", LoopbackConnection.SimulatorMode.ECHO, connection.getMode());
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void testXmodemReceive_ShouldThrow() throws Exception {
        connection.xmodemReceive();
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void testXmodemSend_ShouldThrow() throws Exception {
        connection.xmodemSend(new byte[0]);
    }
    
    @Test(expected = ConnectionException.class)
    public void testSendWhenClosed_ShouldThrow() throws Exception {
        connection.setUri("loopback://echo");
        connection.sendStringToComm("TEST\n"); // Should throw - not open
    }
    
    @Test
    public void testCloseWhenAlreadyClosed_ShouldBeOk() throws Exception {
        connection.setUri("loopback://echo");
        connection.closePort(); // Should not throw even though not open
    }
    
    @Test
    public void testConcurrentCommands() throws Exception {
        connection.setUri("loopback://grbl");
        connection.setResponseDelay(5);
        connection.openPort();
        
        // Clear welcome message
        listener.waitForResponse(500);
        listener.clearResponses();
        
        // Send multiple commands rapidly
        for (int i = 0; i < 10; i++) {
            connection.sendStringToComm("G0 X" + i + "\n");
        }
        
        // Verify all responses received
        int responseCount = 0;
        for (int i = 0; i < 10; i++) {
            String response = listener.waitForResponse(1000);
            if (response != null && response.contains("ok")) {
                responseCount++;
            }
        }
        
        assertEquals("Should receive all responses", 10, responseCount);
    }
    
    @Test
    public void testBufferStressTest_MultipleRuns() throws Exception {
        connection.setUri("loopback://grbl");
        connection.setResponseDelay(1); // Fast response for stress test
        connection.openPort();
        
        // Clear welcome message
        Thread.sleep(100);
        listener.clearResponses();
        
        // Load the buffer stress test file
        String testFilePath = findTestFile("buffer_stress_test.gcode");
        assertNotNull("buffer_stress_test.gcode should be found", testFilePath);
        
        List<String> gcodeLines = Files.readAllLines(Paths.get(testFilePath), StandardCharsets.UTF_8);
        
        // Filter out comments and empty lines, count actual G-code commands
        long commandCount = gcodeLines.stream()
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .filter(line -> !line.startsWith("("))
            .filter(line -> !line.startsWith(";"))
            .count();
        
        System.out.println("Buffer stress test contains " + commandCount + " G-code commands");
        
        // Run the test 5 times
        int runCount = 5;
        AtomicInteger totalResponseCount = new AtomicInteger(0);
        
        for (int run = 0; run < runCount; run++) {
            System.out.println("Starting run " + (run + 1) + " of " + runCount);
            listener.clearResponses();
            
            // Send all commands
            for (String line : gcodeLines) {
                String trimmed = line.trim();
                // Skip comments and empty lines
                if (trimmed.isEmpty() || trimmed.startsWith("(") || trimmed.startsWith(";")) {
                    continue;
                }
                connection.sendStringToComm(trimmed + "\n");
            }
            
            // Wait for all responses with generous timeout
            long startTime = System.currentTimeMillis();
            long timeout = 10000; // 10 seconds per run
            int responseCount = 0;
            
            while (System.currentTimeMillis() - startTime < timeout) {
                String response = listener.waitForResponse(100);
                if (response != null) {
                    listener.clearResponses(); // Reset latch for next response
                    if (response.contains("ok")) {
                        responseCount++;
                    }
                    if (responseCount >= commandCount) {
                        break;
                    }
                }
            }
            
            totalResponseCount.addAndGet(responseCount);
            System.out.println("Run " + (run + 1) + ": received " + responseCount + " responses");
            
            assertTrue("Run " + (run + 1) + " should receive most responses (got " + responseCount + 
                      " out of " + commandCount + ")", responseCount >= commandCount * 0.95);
        }
        
        System.out.println("Total responses across all runs: " + totalResponseCount.get());
        System.out.println("Expected total: " + (commandCount * runCount));
        
        // Verify we received responses for most commands across all runs
        long expectedTotal = commandCount * runCount;
        assertTrue("Should receive at least 95% of expected responses across all runs", 
                  totalResponseCount.get() >= expectedTotal * 0.95);
        
        connection.closePort();
    }
    
    /**
     * Helper method to find test files in the project structure
     */
    private String findTestFile(String filename) {
        // Try multiple possible locations
        String[] possiblePaths = {
            "../test_files/" + filename,
            "../../test_files/" + filename,
            "../../../test_files/" + filename,
            "test_files/" + filename,
            "./test_files/" + filename
        };
        
        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists()) {
                return file.getAbsolutePath();
            }
        }
        
        // Try from workspace root
        String workspaceRoot = System.getProperty("user.dir");
        while (workspaceRoot != null && !workspaceRoot.isEmpty()) {
            File testFile = new File(workspaceRoot, "test_files/" + filename);
            if (testFile.exists()) {
                return testFile.getAbsolutePath();
            }
            // Go up one directory
            File parent = new File(workspaceRoot).getParentFile();
            if (parent == null) break;
            workspaceRoot = parent.getAbsolutePath();
        }
        
        return null;
    }
    
    /**
     * Helper class to capture connection responses
     */
    private static class TestConnectionListener implements IConnectionListener {
        private final StringBuilder responseBuffer = new StringBuilder();
        private volatile CountDownLatch responseLatch = new CountDownLatch(1);
        private final AtomicReference<String> lastResponse = new AtomicReference<>("");
        
        @Override
        public void handleResponseMessage(String response) {
            synchronized (responseBuffer) {
                responseBuffer.append(response).append("\n");
                lastResponse.set(response);
            }
            responseLatch.countDown();
        }
        
        @Override
        public void onConnectionClosed() {
            // Not used in this test
        }
        
        public String waitForResponse(long timeoutMs) throws InterruptedException {
            if (responseLatch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                synchronized (responseBuffer) {
                    return lastResponse.get();
                }
            }
            return null;
        }
        
        public void clearResponses() {
            synchronized (responseBuffer) {
                responseBuffer.setLength(0);
                lastResponse.set("");
                responseLatch = new CountDownLatch(1);
            }
        }
    }
}
