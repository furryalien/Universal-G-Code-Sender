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

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A loopback connection for testing serial communication without physical devices.
 * This connection simulates a hardware controller by providing configurable responses.
 * 
 * <p>Usage examples:</p>
 * <pre>
 * // Basic echo mode (default)
 * loopback://echo
 * 
 * // GRBL simulator
 * loopback://grbl
 * 
 * // TinyG simulator
 * loopback://tinyg
 * 
 * // Custom response mode
 * loopback://custom?response=ok
 * </pre>
 * 
 * @author wwinder
 */
public class LoopbackConnection extends AbstractConnection {
    private static final Logger logger = Logger.getLogger(LoopbackConnection.class.getName());
    
    private volatile boolean isOpen = false;
    private SimulatorMode mode = SimulatorMode.ECHO;
    private String customResponse = "ok\n";
    private int responseDelayMs = 10; // Simulate slight delay
    
    private final BlockingQueue<String> commandQueue = new LinkedBlockingQueue<>();
    private Thread responseThread;
    
    /**
     * Simulator modes for different controller types
     */
    public enum SimulatorMode {
        ECHO,       // Echo back the command
        GRBL,       // Simulate GRBL controller responses
        TINYG,      // Simulate TinyG controller responses
        CUSTOM      // Use custom response string
    }
    
    public LoopbackConnection() {
        // Default constructor
    }
    
    @Override
    public void setUri(String uri) {
        try {
            // Parse URI: loopback://mode?param=value
            String modeStr = StringUtils.substringBetween(uri, ConnectionDriver.LOOPBACK.getProtocol(), "?");
            if (modeStr == null) {
                modeStr = StringUtils.substringAfter(uri, ConnectionDriver.LOOPBACK.getProtocol());
            }
            
            if (StringUtils.isNotBlank(modeStr)) {
                modeStr = modeStr.toLowerCase();
                switch (modeStr) {
                    case "grbl" -> mode = SimulatorMode.GRBL;
                    case "tinyg" -> mode = SimulatorMode.TINYG;
                    case "custom" -> {
                        mode = SimulatorMode.CUSTOM;
                        // Parse custom response from query params
                        String response = StringUtils.substringBetween(uri, "response=", "&");
                        if (response == null) {
                            response = StringUtils.substringAfter(uri, "response=");
                        }
                        if (StringUtils.isNotBlank(response)) {
                            customResponse = response.replace("\\n", "\n");
                        }
                    }
                    default -> mode = SimulatorMode.ECHO;
                }
            }
            
            logger.log(Level.INFO, "Loopback connection configured with mode: {0}", mode);
        } catch (Exception e) {
            throw new ConnectionException("Couldn't parse connection string " + uri, e);
        }
    }
    
    @Override
    public boolean openPort() throws Exception {
        if (isOpen) {
            throw new ConnectionException("Loopback connection is already open");
        }
        
        isOpen = true;
        
        // Start response thread
        responseThread = new Thread(this::processCommands, "LoopbackConnection-ResponseThread");
        responseThread.setDaemon(true);
        responseThread.start();
        
        // Send initial connection messages based on mode
        sendInitialMessages();
        
        logger.log(Level.INFO, "Loopback connection opened in {0} mode", mode);
        return true;
    }
    
    private void sendInitialMessages() {
        switch (mode) {
            case GRBL -> {
                // GRBL sends welcome message on connection
                simulateResponse("\n");
                simulateResponse("Grbl 1.1h ['$' for help]\n");
            }
            case TINYG -> {
                // TinyG sends JSON status on connection
                simulateResponse("{\"r\":{\"fv\":0.970,\"fb\":440.20,\"hp\":3,\"hv\":0,\"id\":\"3X3566-YMB\"},\"f\":[1,0,6,6887]}\n");
            }
        }
    }
    
    @Override
    public void closePort() throws Exception {
        if (!isOpen) {
            return;
        }
        
        isOpen = false;
        
        if (responseThread != null) {
            responseThread.interrupt();
            try {
                responseThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            responseThread = null;
        }
        
        commandQueue.clear();
        logger.log(Level.INFO, "Loopback connection closed");
    }
    
    @Override
    public void sendByteImmediately(byte b) throws Exception {
        if (!isOpen) {
            throw new ConnectionException("Connection is not open");
        }
        // For real-time commands, respond immediately
        byte[] data = new byte[]{b};
        logger.log(Level.FINE, "Loopback received byte: {0}", String.format("0x%02X", b));
    }
    
    @Override
    public void sendStringToComm(String command) throws Exception {
        if (!isOpen) {
            throw new ConnectionException("Connection is not open");
        }
        
        logger.log(Level.FINE, "Loopback received: {0}", command.trim());
        commandQueue.offer(command);
    }
    
    private void processCommands() {
        while (isOpen && !Thread.currentThread().isInterrupted()) {
            try {
                String command = commandQueue.take();
                
                // Simulate processing delay
                if (responseDelayMs > 0) {
                    Thread.sleep(responseDelayMs);
                }
                
                String response = generateResponse(command);
                if (response != null && !response.isEmpty()) {
                    simulateResponse(response);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error processing command in loopback", e);
            }
        }
    }
    
    private String generateResponse(String command) {
        String trimmed = command.trim();
        
        return switch (mode) {
            case ECHO -> command; // Echo back exactly what was sent
            case GRBL -> generateGrblResponse(trimmed);
            case TINYG -> generateTinyGResponse(trimmed);
            case CUSTOM -> customResponse;
        };
    }
    
    private String generateGrblResponse(String command) {
        // Remove line number if present (N### format)
        String cmd = command.replaceFirst("^N\\d+\\s*", "").trim();
        
        // GRBL responses
        if (cmd.startsWith("$")) {
            // Settings commands
            if (cmd.equals("$$")) {
                return "$0=10\n$1=25\n$2=0\n$3=0\nok\n";
            } else if (cmd.equals("$#")) {
                return "[G54:0.000,0.000,0.000]\n[G55:0.000,0.000,0.000]\nok\n";
            } else if (cmd.equals("$G")) {
                return "[GC:G0 G54 G17 G21 G90 G94 M5 M9 T0 F0 S0]\nok\n";
            } else if (cmd.equals("$I")) {
                return "[VER:1.1h.20190825:]\n[OPT:V,15,128]\nok\n";
            } else if (cmd.equals("$N")) {
                return "$N0=\n$N1=\nok\n";
            } else if (cmd.startsWith("$H")) {
                return "ok\n";
            } else if (cmd.startsWith("$X")) {
                return "ok\n";
            }
            return "ok\n";
        } else if (cmd.equals("?")) {
            // Status request
            return "<Idle|MPos:0.000,0.000,0.000|FS:0,0|WCO:0.000,0.000,0.000>\n";
        } else if (cmd.startsWith("~") || cmd.startsWith("!") || cmd.equals("\u0018")) {
            // Real-time commands (no response needed typically)
            return "";
        } else if (cmd.isEmpty()) {
            return "ok\n";
        } else {
            // Regular G-code command
            return "ok\n";
        }
    }
    
    private String generateTinyGResponse(String command) {
        String cmd = command.trim();
        
        // TinyG uses JSON responses
        if (cmd.startsWith("{")) {
            // JSON command
            return "{\"r\":{},\"f\":[1,0,3,0]}\n";
        } else if (cmd.equals("?")) {
            // Status request
            return "{\"sr\":{\"stat\":3,\"momo\":1,\"coor\":1,\"plan\":0,\"path\":0,\"dist\":0,\"frmo\":1,\"posx\":0.000,\"posy\":0.000,\"posz\":0.000}}\n";
        } else if (cmd.startsWith("$")) {
            // Settings
            return "{\"r\":{},\"f\":[1,0,3,0]}\n";
        } else {
            // Regular G-code
            return "{\"r\":{},\"f\":[1,0,3,0]}\n";
        }
    }
    
    private void simulateResponse(String response) {
        try {
            byte[] data = response.getBytes(StandardCharsets.UTF_8);
            getConnectionListenerManager().handleResponse(data, 0, data.length);
            logger.log(Level.FINE, "Loopback sent: {0}", response.trim());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error simulating response", e);
        }
    }
    
    @Override
    public boolean isOpen() {
        return isOpen;
    }
    
    @Override
    public List<? extends IConnectionDevice> getDevices() {
        List<LoopbackConnectionDevice> devices = new ArrayList<>();
        devices.add(new LoopbackConnectionDevice("echo", "Echo loopback (testing)"));
        devices.add(new LoopbackConnectionDevice("grbl", "GRBL Simulator"));
        devices.add(new LoopbackConnectionDevice("tinyg", "TinyG Simulator"));
        devices.add(new LoopbackConnectionDevice("custom", "Custom response mode"));
        return devices;
    }
    
    /**
     * Sets the response delay in milliseconds.
     * @param delayMs delay in milliseconds (0 for instant response)
     */
    public void setResponseDelay(int delayMs) {
        this.responseDelayMs = Math.max(0, delayMs);
    }
    
    /**
     * Gets the current simulator mode.
     * @return the current mode
     */
    public SimulatorMode getMode() {
        return mode;
    }
    
    @Override
    public byte[] xmodemReceive() throws IOException {
        throw new UnsupportedOperationException("XModem not supported in loopback mode");
    }
    
    @Override
    public void xmodemSend(byte[] data) throws IOException {
        throw new UnsupportedOperationException("XModem not supported in loopback mode");
    }
}
