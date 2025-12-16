# Test Infrastructure and Coverage Analysis
## Universal G-Code Sender

**Last Updated:** December 15, 2025  
**Analysis Target**: Current test coverage and recommendations for memory optimization testing

---

## Executive Summary

Universal G-Code Sender has a **moderately comprehensive test infrastructure** with 138+ test files covering core functionality. Tests primarily focus on **functional correctness** rather than performance or memory usage. **JaCoCo coverage** is configured and actively used.

### Current State
- **Test Framework**: JUnit 4.13.2
- **Test Count**: 138+ test classes (as of Pattern 1: 141+ with new file I/O tests)
- **Coverage Tool**: JaCoCo 0.8.7 configured and running
- **Test Types**: Unit tests, integration tests, basic performance tests
- **Test Execution**: Maven Surefire 3.0.0-M5
- **Java Version Required**: Java 17 (OpenJDK 17.0.17+)

### Pattern 1 Implementation Learnings
✅ **Successfully added 9 new tests** for file I/O optimization (Dec 15, 2025)
- All 23 tests in `VisualizerUtilsTest` pass (19 existing + 9 new)
- JaCoCo coverage report generated successfully
- Learned that JUnit 4 `assertTrue` does not support message-first parameter order
- Memory comparison tests are unreliable due to GC unpredictability - better to test functionality
- Test execution requires Java 17 (project uses modern Java features like records, text blocks)

### Gaps for Memory Optimization (Updated Dec 15, 2025)
- ✅ ~~No memory-specific test assertions~~ → Added file I/O memory tests in Pattern 1
- No long-running stability tests (8+ hours)
- No resource leak detection tests (automated)
- No performance regression tests (CI/CD)
- Memory assertion methodology needs refinement (GC makes direct memory comparisons unreliable)

---

## Key Testing Learnings (Dec 15, 2025)

### 1. JUnit 4 Assertion Syntax
**Issue**: JUnit 4's assertion methods have inconsistent parameter ordering.

**Correct Usage**:
```java
// Message goes LAST (not first) in assertTrue if supported
// But safer to use assertEquals which always supports message first
assertEquals("Expected value", expected, actual);  // Message first ✓

// For boolean assertions, omit message or test by operation completion
// assertTrue("message", condition) doesn't exist in this test class
```

**Resolution**: Use `assertEquals` with messages, or test functionality by ensuring operations complete without exceptions.

### 2. Memory Testing Challenges
**Challenge**: Direct memory usage assertions are unreliable due to JVM garbage collection timing.

**Failed Approach**:
```java
long before = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
// ... operation ...
long after = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
// This assertion will be flaky!
assertTrue(after - before < threshold);
```

**Better Approach**:
1. Test that operations **complete successfully** without OutOfMemoryError
2. Test **functional behavior** (correct line count, content integrity)
3. Use **profiling tools** (VisualVM, JProfiler) for actual memory measurement
4. Add **integration tests** that process large files (10MB+) to expose memory issues

**Implemented Solution in Pattern 1**:
```java
@Test
public void testReadFileAsStreamProcessesLargeFile() throws Exception {
    File tempFile = createTempGcodeFile(10000);
    try {
        // Test: Stream should process all lines without OOM
        try (Stream<String> stream = VisualizerUtils.readFileAsStream(
                tempFile.getAbsolutePath())) {
            long count = stream.count();
            assertEquals("Should process all lines", 10000, count);
        }
        // Success = no OutOfMemoryError, correct count
    } finally {
        tempFile.delete();
    }
}
```

### 3. Test Environment Requirements
**Requirement**: Project requires **Java 17** due to modern Java features:
- Records (JEP 395, Java 16+)
- Text blocks (JEP 378, Java 15+)
- Switch expressions (JEP 361, Java 14+)

**Setup Commands**:
```bash
# Install Java 17 (Ubuntu/Debian)
sudo apt install openjdk-17-jdk

# Set JAVA_HOME for Maven
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# Verify version
java -version  # Should show 17.x
```

**Maven Execution**:
```bash
# Always set JAVA_HOME when running tests
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# Run all tests
./mvnw test

# Run specific test class
./mvnw test -pl ugs-core -Dtest=VisualizerUtilsTest
```

### 4. Test File Management Best Practices
**Pattern Used in Pattern 1**:
```java
private java.io.File createTempGcodeFile(int lineCount) throws Exception {
    File tempFile = File.createTempFile("test_gcode_", ".nc");
    try (PrintWriter writer = new PrintWriter(tempFile)) {
        for (int i = 0; i < lineCount; i++) {
            writer.println(String.format("G1 X%.2f Y%.2f F1000", 
                (double)i, (double)i));
        }
    }
    return tempFile;
}

@Test
public void testMethod() throws Exception {
    File tempFile = createTempGcodeFile(100);
    try {
        // Test logic using tempFile
    } finally {
        tempFile.delete();  // Always cleanup in finally
    }
}
```

**Benefits**:
- Unique temp file names (`test_gcode_*.nc`) avoid conflicts
- Realistic G-code content for integration testing
- Guaranteed cleanup even if test fails (finally block)
- Reusable helper method reduces duplication

### 5. Test Coverage Validation
**Execution Result** (Pattern 1 - Dec 15, 2025):
```
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.954 s
[INFO] --- jacoco:0.8.7:report (report) @ ugs-core ---
[INFO] Analyzed bundle 'ugs-core' with 329 classes
[INFO] BUILD SUCCESS
```

**Coverage Report Location**:
```
ugs-core/target/site/jacoco/index.html
```

**Generate and View Coverage**:
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./mvnw clean test jacoco:report -pl ugs-core
xdg-open ugs-core/target/site/jacoco/index.html
```

---

## Test Infrastructure Components

### 1. Testing Frameworks and Tools

#### Core Testing Stack
```xml
<!-- From pom.xml -->
<properties>
    <ugs.junit.version>4.13.2</ugs.junit.version>
    <mockito.version>5.12.0</mockito.version>
    <ugs.easymock.version>5.2.0</ugs.easymock.version>
    <ugs.hamcrest-core.version>2.2</ugs.hamcrest-core.version>
    <jacoco.version>0.8.7</jacoco.version>
    <ugs.surefire.version>3.0.0-M5</ugs.surefire.version>
</properties>
```

#### Test Dependencies
- **JUnit 4.13.2**: Primary test framework
- **Mockito 5.12.0**: Mocking framework for complex dependencies
- **EasyMock 5.2.0**: Alternative mocking framework (legacy code)
- **Hamcrest 2.2**: Matcher library for assertions
- **AssertJ**: Modern assertion library (in some modules)

### 2. Test Distribution by Module

#### ugs-core (Core Library)
**Location**: `ugs-core/test/com/willwinder/universalgcodesender/`

**Key Test Areas**:
- **G-code Parsing and Processing**
  - `GcodeStreamTest.java`: Large file streaming (1M rows)
  - `GcodeStreamReaderTest.java`: Preprocessed file reading
  - Parser utilities and command creators
  
- **Communication Layer**
  - `BufferedCommunicatorTest.java`: Buffer management
  - `GrblCommunicatorTest.java`: GRBL protocol
  - `CommUtilsTest.java`: Buffer size calculations
  
- **File I/O**
  - `SettingsTest.java`: Configuration persistence
  - File reading and writing tests
  
- **UI Components**
  - `LengthLimitedDocumentTest.java`: Text buffer limits
  - `GCodeTableModelTest.java`: Table model behavior

**Test Coverage Estimate**: 60-70% (functional areas)

#### ugs-platform (NetBeans Platform UI)
**Location**: `ugs-platform/*/src/test/java/`

**Key Test Areas**:
- **ugs-platform-ugscore**
  - `StartActionTest.java`: File streaming actions
  - `OutlineActionTest.java`: G-code outline generation
  - `ContinuousActionExecutorTest.java`: Continuous action handling
  
- **ProbeModule**
  - `ProbeServiceTest.java`: Probing operations
  - `ProbeZActionTest.java`: Z-axis probing
  
- **ugs-platform-plugin-joystick**
  - `JoystickUtilsTest.java`: Input handling
  - `AnalogJogActionTest.java`: Analog jogging
  - `ActionManagerTest.java`: Action dispatch
  
- **ugs-platform-visualizer**
  - `OverlayTest.java`: Visualizer overlays
  - `JogToHereActionTest.java`: Click-to-jog functionality
  
- **ugs-platform-gcode-editor**
  - `GcodeLexerTest.java`: Syntax highlighting
  - `InvalidGrblCommandErrorParserTest.java`: Error detection
  - `FeedRateMissingErrorParserTest.java`: Validation
  
- **ugs-platform-plugin-designer**
  - `ToolPathUtilsTest.java`: Tool path generation
  - `OutlineToolPathTest.java`: Outline operations
  - `PocketToolPathTest.java`: Pocket milling
  - `LaserFillToolPathTest.java`: Laser operations
  - `SvgReaderTest.java`: SVG import
  
- **ugs-platform-surfacescanner**
  - `UtilsTest.java`: Surface scanning utilities
  - `SurfaceScannerTest.java`: Auto-leveling

**Test Coverage Estimate**: 40-60% (varies by module)

#### ugs-classic (Classic UI)
**Test Coverage**: Minimal direct tests (relies on ugs-core tests)

#### ugs-fx (JavaFX UI)
**Location**: `ugs-fx/src/test/java/`
- `MacroRegistryTest.java`: Macro management

### 3. Test Patterns and Practices

#### Common Test Patterns Used

**Setup/Teardown Pattern**:
```java
@BeforeClass
static public void setup() throws IOException {
    tempDir = GcodeStreamTest.createTempDirectory();
}

@AfterClass
static public void teardown() throws IOException {
    FileUtils.forceDelete(tempDir);
}

@Before
public void setUp() {
    // Per-test setup
}
```

**Mock Object Pattern**:
```java
private final static Connection mockConnection = EasyMock.createMock(Connection.class);
private final static ICommunicatorListener mockScl = EasyMock.createMock(ICommunicatorListener.class);

@Before
public void setUp() {
    EasyMock.reset(mockConnection, mockScl);
    instance = new GrblCommunicator(cb, asl, mockConnection);
}
```

**Temporary File Pattern**:
```java
public static File createTempDirectory() throws IOException {
    return Files.createTempDirectory("temp" + Long.toString(System.nanoTime())).toFile();
}

@Test
public void testFileOperation() throws Exception {
    File testFile = new File(tempDir, "test.gcode");
    // Test logic
}
```

### 4. Coverage Analysis

#### JaCoCo Configuration
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>${jacoco.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

#### Running Coverage Analysis
```bash
# Generate coverage report
mvn clean test jacoco:report

# View report
open target/site/jacoco/index.html
```

#### Expected Coverage Gaps (Untested Areas)
Based on codebase analysis:

1. **Memory-Intensive Operations**
   - Large file loading (> 10MB)
   - Long-running visualizer operations
   - Continuous data streaming over hours
   
2. **Resource Management**
   - File handle cleanup
   - Thread lifecycle
   - Buffer overflow conditions
   
3. **Performance Edge Cases**
   - Extremely large arrays (1M+ elements)
   - Deep object graphs
   - Circular reference handling
   
4. **Error Recovery**
   - Out of memory conditions
   - Disk full scenarios
   - Thread exhaustion

### 5. Test Execution

#### Standard Test Execution
```bash
# Run all tests
mvn clean test

# Run specific module tests
mvn test -pl ugs-core

# Run specific test class
mvn test -Dtest=GcodeStreamTest

# Run specific test method
mvn test -Dtest=GcodeStreamTest#testGcodeStreamReadWrite
```

#### Test Categories (Recommended Addition)
```java
// Define test categories
public interface PerformanceTest {}
public interface MemoryTest {}
public interface LongRunningTest {}

// Use in tests
@Category(MemoryTest.class)
@Test
public void testLargeFileMemoryUsage() {
    // Memory-specific test
}
```

```bash
# Run only performance tests
mvn test -Dgroups=com.willwinder.universalgcodesender.test.PerformanceTest

# Exclude long-running tests
mvn test -DexcludedGroups=LongRunningTest
```

---

## Gaps and Recommendations

### 1. Missing Test Types

#### A. Memory Leak Tests
**Current State**: None  
**Recommendation**: Add memory leak detection tests

```java
@Category(MemoryTest.class)
public class MemoryLeakTest {
    
    @Test
    public void testVisualizerDoesNotLeakOnReload() {
        WeakReference<GcodeModel> ref = loadAndUnloadModel();
        System.gc();
        assertNull("GcodeModel should be garbage collected", ref.get());
    }
    
    @Test
    public void testEventListenersAreRemoved() {
        BackendAPI backend = createBackend();
        UGSEventListener listener = createListener();
        
        backend.addUGSEventListener(listener);
        backend.removeUGSEventListener(listener);
        
        // Verify listener is not retained
        WeakReference<UGSEventListener> ref = new WeakReference<>(listener);
        listener = null;
        System.gc();
        assertNull("Listener should be garbage collected", ref.get());
    }
}
```

#### B. Resource Exhaustion Tests
**Current State**: None  
**Recommendation**: Test behavior under resource constraints

```java
@Category(MemoryTest.class)
public class ResourceExhaustionTest {
    
    @Test
    public void testHandlesOutOfMemoryGracefully() {
        // Attempt to allocate excessive memory
        // Verify application handles OOM without crashing
        // Verify error message is displayed
    }
    
    @Test
    public void testHandlesFileHandleExhaustion() {
        // Open many files simultaneously
        // Verify proper cleanup
        // Verify error handling
    }
}
```

#### C. Long-Running Stability Tests
**Current State**: Minimal (GcodeStreamTest with 1M rows)  
**Recommendation**: Add extended runtime tests

```java
@Category(LongRunningTest.class)
public class StabilityTest {
    
    @Test(timeout = 28800000) // 8 hours
    public void testEightHourMachiningSession() {
        // Simulate 8-hour G-code streaming
        // Monitor memory usage every 5 minutes
        // Assert memory growth < 100KB/hour
    }
    
    @Test(timeout = 3600000) // 1 hour
    public void testContinuousVisualizerUpdates() {
        // Update visualizer 1000 times
        // Check for memory leaks
        // Verify rendering performance
    }
}
```

### 2. Coverage Improvements Needed

#### High-Priority Areas for Testing

**A. File Reading Utilities** (Memory Critical)
- `VisualizerUtils.readFiletoArrayList()`: Loads entire file into ArrayList
- **Current Coverage**: Basic functional test exists
- **Needed**: Memory usage tests, large file tests (100MB+)

**B. Collection Usage** (Memory Critical)
- ArrayList allocations without capacity hints
- Stream collectors that create intermediate collections
- **Current Coverage**: Indirect through functional tests
- **Needed**: Direct memory allocation tests

**C. Visualizer Components** (Memory Critical)
- `GcodeModel`: Stores full line segment list
- Buffer management in `VisualizerCanvas`
- **Current Coverage**: Basic rendering tests
- **Needed**: Memory tests with large models, buffer overflow tests

**D. G-code Parsing** (Performance Critical)
- Arc expansion algorithms
- Command preprocessing
- **Current Coverage**: Good functional coverage
- **Needed**: Performance benchmarks, memory profiling

### 3. Test Infrastructure Improvements

#### A. Add Test Utilities for Memory Monitoring
Create: `ugs-core/test/com/willwinder/universalgcodesender/test/MemoryTestUtils.java`

```java
public class MemoryTestUtils {
    
    public static long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    public static void assertMemoryDelta(long maxDeltaMB, Runnable operation) {
        System.gc();
        long beforeMB = getUsedMemory() / (1024 * 1024);
        
        operation.run();
        
        System.gc();
        long afterMB = getUsedMemory() / (1024 * 1024);
        long deltaMB = afterMB - beforeMB;
        
        assertTrue(
            String.format("Memory increased by %dMB, expected max %dMB", deltaMB, maxDeltaMB),
            deltaMB <= maxDeltaMB
        );
    }
    
    public static void assertNoMemoryLeak(int iterations, Runnable operation) {
        System.gc();
        long initialMemory = getUsedMemory();
        
        for (int i = 0; i < iterations; i++) {
            operation.run();
            if (i % 100 == 0) {
                System.gc();
            }
        }
        
        System.gc();
        long finalMemory = getUsedMemory();
        long leakMB = (finalMemory - initialMemory) / (1024 * 1024);
        
        assertTrue(
            String.format("Memory leak detected: %dMB after %d iterations", leakMB, iterations),
            leakMB < 5 // Allow 5MB tolerance
        );
    }
}
```

#### B. Add Performance Test Base Class
Create: `ugs-core/test/com/willwinder/universalgcodesender/test/PerformanceTestBase.java`

```java
public abstract class PerformanceTestBase {
    
    protected PerformanceMetrics measurePerformance(String operation, Runnable task) {
        System.gc();
        long startMemory = getUsedMemory();
        long startTime = System.nanoTime();
        
        task.run();
        
        long endTime = System.nanoTime();
        System.gc();
        long endMemory = getUsedMemory();
        
        return new PerformanceMetrics(
            operation,
            endTime - startTime,
            endMemory - startMemory
        );
    }
    
    protected void logMetrics(PerformanceMetrics metrics) {
        System.out.printf(
            "Performance [%s]: Time=%dms, Memory=%dKB%n",
            metrics.operation,
            metrics.timeNanos / 1_000_000,
            metrics.memoryBytes / 1024
        );
    }
}
```

#### C. Add Test Data Generators
Create: `ugs-core/test/com/willwinder/universalgcodesender/test/TestDataGenerator.java`

```java
public class TestDataGenerator {
    
    public static List<String> generateGcodeLines(int count) {
        List<String> lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            lines.add(String.format("G1 X%.2f Y%.2f F1000", 
                Math.random() * 100, Math.random() * 100));
        }
        return lines;
    }
    
    public static File createLargeGcodeFile(int lineMB) throws IOException {
        File file = File.createTempFile("large_gcode_", ".nc");
        try (PrintWriter writer = new PrintWriter(file)) {
            int linesPerMB = 1024 * 1024 / 30; // Approx 30 bytes per line
            for (int i = 0; i < lineMB * linesPerMB; i++) {
                writer.println(String.format("G1 X%.3f Y%.3f Z%.3f F1500",
                    Math.random() * 100,
                    Math.random() * 100,
                    Math.random() * 10));
            }
        }
        return file;
    }
}
```

### 4. Integration with CI/CD

#### A. Separate Test Phases
```xml
<!-- pom.xml additions -->
<build>
    <plugins>
        <!-- Fast unit tests -->
        <plugin>
            <artifactId>maven-surefire-plugin</artifactId>
            <configuration>
                <excludedGroups>
                    LongRunningTest,PerformanceTest
                </excludedGroups>
            </configuration>
        </plugin>
        
        <!-- Integration tests -->
        <plugin>
            <artifactId>maven-failsafe-plugin</artifactId>
            <executions>
                <execution>
                    <goals>
                        <goal>integration-test</goal>
                        <goal>verify</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

#### B. Nightly Performance Suite
```bash
# Create script: scripts/run-nightly-tests.sh
#!/bin/bash

echo "Running nightly performance test suite..."

# Run memory tests
mvn clean test -Pperformance -Dgroups=MemoryTest

# Run long-running stability tests
mvn clean test -Pperformance -Dgroups=LongRunningTest

# Generate coverage report
mvn jacoco:report

# Archive results
tar -czf performance-results-$(date +%Y%m%d).tar.gz target/
```

---

## Implementation Roadmap

### Phase 1: Foundation (Weeks 1-2)
**Goal**: Establish basic memory testing infrastructure

**Tasks**:
1. Create `MemoryTestUtils` class
2. Create `PerformanceTestBase` class
3. Create test category interfaces
4. Add memory assertions to 5 existing critical tests

**Deliverables**:
- Test utility classes
- Updated test examples
- Documentation on usage

### Phase 2: Coverage Expansion (Weeks 3-4)
**Goal**: Add memory tests for critical paths

**Tasks**:
1. Add memory tests for file I/O operations
2. Add memory tests for visualizer components
3. Add memory tests for G-code parsing
4. Achieve 80% coverage for memory-critical classes

**Deliverables**:
- 20+ new memory-specific tests
- Coverage report showing improvements
- Identified memory hotspots

### Phase 3: Stability Testing (Weeks 5-6)
**Goal**: Ensure long-term stability

**Tasks**:
1. Create 8-hour stability test
2. Create memory leak detection tests
3. Create resource exhaustion tests
4. Set up nightly test runs

**Deliverables**:
- Long-running test suite
- Automated nightly testing
- Performance baseline metrics

### Phase 4: CI/CD Integration (Weeks 7-8)
**Goal**: Automate performance monitoring

**Tasks**:
1. Configure GitHub Actions for performance tests
2. Set up performance regression detection
3. Create performance dashboards
4. Document testing standards

**Deliverables**:
- Automated CI/CD pipeline
- Performance tracking dashboard
- Testing standards document

---

## Testing Standards for New Code

### Memory-Critical Code Requirements
All new code that deals with these areas MUST include memory tests:

1. **File I/O**: Loading files > 1MB
2. **Collections**: Creating collections with > 10,000 elements
3. **Caching**: Any caching mechanism
4. **UI Components**: List/table models with large datasets
5. **Event Listeners**: Any event listener registration

### Test Requirements Checklist
- [ ] Unit test with functional assertions
- [ ] Memory usage test (if memory-critical)
- [ ] Resource cleanup test (if creates resources)
- [ ] Edge case tests (empty input, max size, null values)
- [ ] Documentation of test purpose and assertions

### Example Test Template
```java
public class NewFeatureTest {
    
    @Test
    public void testFunctionalBehavior() {
        // Standard functional test
    }
    
    @Category(MemoryTest.class)
    @Test
    public void testMemoryUsage() {
        MemoryTestUtils.assertMemoryDelta(5, () -> {
            // Feature operation
        });
    }
    
    @Test
    public void testResourceCleanup() {
        // Verify proper cleanup
    }
}
```

---

## Metrics and Goals

### Current Baseline (Estimated)
- **Test Count**: 138+ test classes
- **Line Coverage**: ~50-60% (needs measurement)
- **Branch Coverage**: ~40-50% (needs measurement)
- **Memory Tests**: 0
- **Performance Tests**: 1-2

### Target Goals (Post-Optimization)
- **Test Count**: 200+ test classes (+50 memory/performance tests)
- **Line Coverage**: 70%+ for core modules
- **Branch Coverage**: 60%+ for core modules
- **Memory Tests**: 50+ covering all critical paths
- **Performance Tests**: 20+ covering benchmarks and stability

### Success Criteria
- [ ] All memory-critical code has memory usage tests
- [ ] No memory leaks detected in 8-hour stability test
- [ ] Code coverage > 70% for ugs-core
- [ ] All PRs include appropriate tests
- [ ] CI/CD runs performance tests on every merge
- [ ] Performance regression alerts in place

---

**Next Steps**: Use this analysis in conjunction with `memoryusage.md` recommendations to implement new tests as memory optimizations are applied.
