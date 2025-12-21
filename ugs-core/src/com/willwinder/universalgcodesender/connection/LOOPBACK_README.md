# Loopback Connection - Testing Without Hardware

The `LoopbackConnection` provides a virtual serial connection for testing UGS serial communication without requiring physical CNC hardware.

## Overview

This connection type simulates different CNC controllers, allowing developers to:
- Test serial communication logic
- Develop features without hardware access
- Create automated integration tests
- Validate command parsing and response handling

## Usage

### Basic Connection

```java
Connection connection = ConnectionFactory.getConnection(ConnectionDriver.LOOPBACK);
connection.openConnection("loopback://echo", null, 9600);
connection.sendStringToComm("G0 X10 Y20\n");
connection.closeConnection();
```

### URI Format

```
loopback://<mode>[?param=value]
```

## Simulator Modes

### ECHO Mode
Echoes back exactly what is sent. Useful for testing basic communication.

```java
connection.openConnection("loopback://echo", null, 9600);
connection.sendStringToComm("G0 X10\n");
// Response: "G0 X10\n"
```

### GRBL Mode
Simulates a GRBL 1.1 controller with realistic responses.

```java
connection.openConnection("loopback://grbl", null, 9600);
```

**Supported Commands:**
- `$$` - Returns settings ($0=10, $1=25, etc.)
- `$#` - Returns coordinate offsets
- `$G` - Returns parser state
- `$I` - Returns version info
- `$H` - Homing (returns "ok")
- `$X` - Unlock (returns "ok")
- `?` - Status request (returns `<Idle|MPos:...>`)
- G-code commands - Returns "ok"

**Example:**
```java
connection.sendStringToComm("G0 X10\n");  // Response: "ok"
connection.sendStringToComm("?\n");        // Response: "<Idle|MPos:0.000,0.000,0.000|FS:0,0|WCO:0.000,0.000,0.000>"
connection.sendStringToComm("$$\n");       // Response: "$0=10\n$1=25\n$2=0\n...\nok"
```

### TINYG Mode
Simulates a TinyG controller with JSON responses.

```java
connection.openConnection("loopback://tinyg", null, 9600);
connection.sendStringToComm("G0 X10\n");
// Response: {"r":{},"f":[1,0,255]}
```

**Supported Commands:**
- `{"sr":null}` or `?` - Status request (returns current state)
- G-code commands - Returns JSON success response
- `{"sys":null}` - System status

### CUSTOM Mode
Returns a user-defined response for any command.

```java
connection.openConnection("loopback://custom?response=MY_RESPONSE", null, 9600);
connection.sendStringToComm("ANY_COMMAND\n");
// Response: "MY_RESPONSE"
```

## Configuration

### Response Delay
Simulate realistic response timing:

```java
LoopbackConnection connection = new LoopbackConnection();
connection.setResponseDelay(50);  // 50ms delay per response
connection.openConnection("loopback://grbl", null, 9600);
```

Default: 10ms delay

### Custom Response
For CUSTOM mode, specify the response in the URI:

```java
connection.openConnection("loopback://custom?response=ok\\n", null, 9600);
```

**Note:** Use `\\n` in URIs to represent newline characters.

## Testing Examples

### Unit Test Example

```java
@Test
public void testGrblCommunication() throws Exception {
    Connection connection = new LoopbackConnection();
    connection.setUri("loopback://grbl");
    connection.openPort();
    
    TestListener listener = new TestListener();
    connection.addListener(listener);
    
    connection.sendStringToComm("G0 X10\n");
    
    String response = listener.waitForResponse(1000);
    assertEquals("ok", response);
    
    connection.closePort();
}
```

### Integration Test Example

```java
@Test
public void testCompleteWorkflow() throws Exception {
    // Use loopback connection for integration testing
    BackendAPI backend = new BackendAPI();
    backend.setConnection(ConnectionFactory.getConnection(ConnectionDriver.LOOPBACK));
    backend.connect("loopback://grbl", 9600);
    
    // Test without hardware
    backend.sendGcodeCommand("G0 X10");
    // Verify behavior...
    
    backend.disconnect();
}
```

## Device Discovery

The loopback connection provides predefined "devices" for convenience:

```java
List<? extends IConnectionDevice> devices = connection.getDevices();
// Returns: echo, grbl, tinyg, custom
```

These can be used in UI dropdowns or test fixtures.

## Implementation Details

- **Thread-safe:** Uses BlockingQueue for command buffering
- **Background processing:** Responses are generated on a separate thread
- **Realistic timing:** Configurable delay simulates real hardware latency
- **Line-based:** Responses are split by `\n` character (matching ConnectionListenerManager behavior)

## Limitations

- Does not simulate connection errors or timeouts
- XModem transfers not supported (throws UnsupportedOperationException)
- TinyG JSON responses are simplified (not full protocol)
- GRBL state machine is not fully simulated

## See Also

- `LoopbackConnection.java` - Implementation
- `LoopbackConnectionTest.java` - Comprehensive test examples
- `ConnectionFactory.java` - Connection creation
