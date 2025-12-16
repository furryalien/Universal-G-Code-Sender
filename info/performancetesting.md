# Performance Testing and Monitoring Guide
## Universal G-Code Sender

**Last Updated:** December 15, 2025  
**Target**: 4MB maximum runtime memory usage for long machining sessions on older laptops

---

## Current State

### Existing Performance Infrastructure

#### Test Framework
- **Testing Framework**: JUnit 4.13.2
- **Test Runner**: Maven Surefire 3.0.0-M5
- **Coverage Tool**: JaCoCo 0.8.7
- **Mocking Frameworks**: Mockito 5.12.0, EasyMock 5.2.0

#### Existing Test Cases
- **Total Test Files**: 138+
- **Test Coverage Areas**:
  - Core G-code parsing and streaming (`GcodeStreamTest`)
  - File I/O operations
  - Controller communication (`BufferedCommunicatorTest`, `GrblCommunicatorTest`)
  - UI components
  - Joystick actions
  - Probe module
  - Visualizer components

#### Current Performance Tests
1. **GcodeStreamTest.testGcodeStreamReadWrite()**: Tests streaming 1,000,000 rows
   - Location: `ugs-core/test/com/willwinder/universalgcodesender/utils/GcodeStreamTest.java`
   - Tests both write and read performance of preprocessed G-code
   - Validates metadata integrity during large file operations

2. **BufferedCommunicatorTest**: Tests buffer management and command queueing
   - Location: `ugs-core/test/com/willwinder/universalgcodesender/communicator/BufferedCommunicatorTest.java`
   - Tests buffer size calculations (101 byte buffer)
   - Tests command streaming and acknowledgment handling

### Performance Limitations
- **No Memory Profiling**: No automated memory usage tracking during test runs
- **No Baseline Metrics**: No established performance baselines for comparison
- **No Long-Running Tests**: Existing tests focus on correctness, not sustained performance
- **No Resource Monitoring**: No tracking of CPU, memory, or I/O during operations

---

## Recommended Performance Testing Infrastructure

### 1. Memory Profiling Setup

#### JVM Arguments for Testing
Add to `pom.xml` surefire plugin configuration:
```xml
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>${ugs.surefire.version}</version>
    <configuration>
        <argLine>
            -Xms256m 
            -Xmx512m 
            -XX:+UseG1GC 
            -XX:MaxGCPauseMillis=200
            -XX:+PrintGCDetails 
            -XX:+PrintGCDateStamps 
            -Xloggc:${project.build.directory}/gc.log
            -XX:+HeapDumpOnOutOfMemoryError
            -XX:HeapDumpPath=${project.build.directory}/heap-dump.hprof
        </argLine>
    </configuration>
</plugin>
```

#### Recommended Profiling Tools
1. **VisualVM** (Free, bundled with JDK)
   - Real-time memory and CPU monitoring
   - Heap dump analysis
   - Thread profiling
   - GC monitoring

2. **JProfiler** (Commercial, recommended for detailed analysis)
   - Advanced memory leak detection
   - Allocation hot spots
   - CPU profiling with call trees
   - Database and I/O profiling

3. **YourKit Java Profiler** (Commercial alternative)
   - Low overhead profiling
   - Integration with IDEs
   - Advanced memory analysis

4. **JMH (Java Microbenchmark Harness)** (Free, for micro-benchmarks)
   - Precise performance measurements
   - Statistical analysis
   - JIT compiler warm-up handling

### 2. Performance Test Categories

#### A. Memory Stress Tests
Create `PerformanceTest` category:

```java
@Category(PerformanceTest.class)
public class LargeFileMemoryTest {
    
    @Test
    public void testLargeFileLoading_10MB() {
        // Load 10MB G-code file
        // Track memory before/after
        // Assert memory increase < 4MB
    }
    
    @Test
    public void testLargeFileStreaming_100MB() {
        // Stream 100MB file
        // Monitor memory over time
        // Assert no memory leaks
    }
    
    @Test
    public void testLongRunningVisualization_8Hours() {
        // Simulate 8-hour machining session
        // Update visualizer continuously
        // Track memory growth rate
    }
}
```

#### B. CPU Profiling Tests
```java
@Category(PerformanceTest.class)
public class GcodeParsingPerformanceTest {
    
    @Test
    public void testParsingSpeed_1000LinesPerSecond() {
        // Measure G-code parsing throughput
        // Assert > 1000 lines/second
    }
    
    @Test
    public void testArcExpansionPerformance() {
        // Test arc to line segment conversion
        // Measure CPU time and allocations
    }
}
```

#### C. I/O Performance Tests
```java
@Category(PerformanceTest.class)
public class StreamingPerformanceTest {
    
    @Test
    public void testSerialPortThroughput() {
        // Test command transmission rate
        // Measure latency and throughput
    }
    
    @Test
    public void testFileReadPerformance() {
        // Test buffered vs unbuffered reading
        // Measure read rates for various file sizes
    }
}
```

### 3. Performance Metrics to Capture

#### Memory Metrics
- **Heap Usage**: Current heap size, max heap size, used heap
- **Non-Heap Usage**: Metaspace, code cache, compressed class space
- **GC Activity**: GC count, GC time, GC pause time
- **Object Allocation Rate**: Objects allocated per second
- **Memory Leak Detection**: Heap growth over time

#### CPU Metrics
- **CPU Time**: Total CPU time consumed
- **Thread Count**: Active threads, peak threads
- **Method Hot Spots**: Most frequently called methods
- **JIT Compilation**: Time spent in JIT compilation

#### I/O Metrics
- **File Read/Write Rate**: Bytes per second
- **Serial Port Throughput**: Commands per second
- **Buffer Utilization**: Average buffer fill percentage

### 4. Automated Performance Monitoring

#### Integration with Build Pipeline
Add Maven profile for performance tests:
```xml
<profile>
    <id>performance</id>
    <build>
        <plugins>
            <plugin>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <groups>com.willwinder.universalgcodesender.test.PerformanceTest</groups>
                    <argLine>-Xmx4m</argLine>
                    <!-- Force 4MB limit to test optimization -->
                </configuration>
            </plugin>
        </plugins>
    </build>
</profile>
```

Run with: `mvn clean test -Pperformance`

#### Continuous Performance Tracking
Create `PerformanceTracker` utility class:
```java
public class PerformanceTracker {
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private static final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    
    public static PerformanceSnapshot captureSnapshot() {
        return new PerformanceSnapshot(
            memoryBean.getHeapMemoryUsage(),
            memoryBean.getNonHeapMemoryUsage(),
            threadBean.getThreadCount(),
            ManagementFactory.getGarbageCollectorMXBeans()
        );
    }
    
    public static void logMetrics(String operation, PerformanceSnapshot before, PerformanceSnapshot after) {
        long heapDelta = after.heapUsed - before.heapUsed;
        long timeElapsed = after.timestamp - before.timestamp;
        
        logger.info(String.format(
            "Performance [%s]: Heap Δ=%dKB, Time=%dms, Threads=%d",
            operation, heapDelta / 1024, timeElapsed, after.threadCount
        ));
    }
}
```

### 5. Performance Benchmarking Suite

#### Create Benchmark Module
Location: `ugs-core/test/com/willwinder/universalgcodesender/benchmarks/`

**GcodeParsingBenchmark.java**:
```java
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class GcodeParsingBenchmark {
    
    @Param({"1000", "10000", "100000"})
    private int lineCount;
    
    private List<String> gcodeLines;
    private GcodeParser parser;
    
    @Setup
    public void setup() {
        parser = new GcodeParser();
        gcodeLines = generateTestGcode(lineCount);
    }
    
    @Benchmark
    public void parseGcode() {
        for (String line : gcodeLines) {
            parser.addCommand(line);
        }
    }
}
```

Run benchmarks: `mvn clean test -Pbenchmark`

### 6. Performance Testing Checklist

#### Before Each Release
- [ ] Run full performance test suite
- [ ] Profile memory usage with VisualVM during typical workflow
- [ ] Test with maximum file size (suggest 100MB G-code file)
- [ ] Run 8-hour stress test simulating long machining session
- [ ] Check for memory leaks with heap dump analysis
- [ ] Verify GC behavior is acceptable (< 5% time in GC)
- [ ] Test on minimum spec hardware (2GB RAM, dual-core CPU)

#### Performance Regression Testing
- [ ] Compare benchmark results with previous release
- [ ] Alert if any metric degrades > 10%
- [ ] Document performance improvements in release notes

### 7. Performance Test Execution

#### Running Performance Tests
```bash
# Run all performance tests
mvn clean test -Dgroups=com.willwinder.universalgcodesender.test.PerformanceTest

# Run with memory constraints
mvn clean test -Dgroups=PerformanceTest -DargLine="-Xmx512m"

# Run specific performance test
mvn test -Dtest=LargeFileMemoryTest -Dgroups=PerformanceTest

# Run with profiler attached
mvn test -Dgroups=PerformanceTest -agentlib:hprof=cpu=samples,depth=20
```

#### Analyzing Results
1. **Memory Analysis**:
   - Open heap dump in VisualVM
   - Look for object retention paths
   - Identify largest allocations
   - Check for duplicate strings/arrays

2. **CPU Analysis**:
   - Review method call tree
   - Identify hot spots (> 5% CPU time)
   - Check for excessive object creation
   - Look for inefficient loops

3. **GC Analysis**:
   - Parse GC logs with GCViewer
   - Check GC frequency and duration
   - Identify memory pressure patterns
   - Optimize GC parameters if needed

### 8. Performance Reporting

#### Automated Report Generation
Create `PerformanceReport` class:
```java
public class PerformanceReport {
    public void generateReport(String testName, PerformanceMetrics metrics) {
        // Generate JSON report
        // Include: heap usage, GC stats, timing, throughput
        // Store in target/performance-reports/
    }
    
    public void compareWithBaseline(PerformanceMetrics current, PerformanceMetrics baseline) {
        // Calculate deltas
        // Flag regressions (> 10% worse)
        // Highlight improvements
    }
}
```

#### Report Contents
- **Summary**: Pass/fail status, key metrics
- **Memory Profile**: Heap usage over time, GC activity
- **CPU Profile**: Hot methods, thread activity
- **Comparison**: vs. previous run, vs. baseline
- **Recommendations**: Optimization suggestions

### 9. Integration with CI/CD

#### GitHub Actions Workflow
```yaml
name: Performance Tests

on:
  pull_request:
    branches: [ master ]
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM

jobs:
  performance:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 17
        uses: actions/setup-java@v2
        with:
          java-version: '17'
      - name: Run Performance Tests
        run: mvn clean test -Pperformance
      - name: Upload Performance Report
        uses: actions/upload-artifact@v2
        with:
          name: performance-report
          path: target/performance-reports/
      - name: Check Performance Regression
        run: python scripts/check_performance_regression.py
```

---

## Recommendations for Implementation

### Phase 1: Foundation (Week 1-2)
1. Add JMX monitoring to existing tests
2. Create `PerformanceTracker` utility class
3. Add memory assertions to existing large file tests
4. Set up VisualVM profiling workflow documentation

### Phase 2: Test Suite (Week 3-4)
1. Create performance test category
2. Implement memory stress tests
3. Add long-running stability tests
4. Create baseline performance metrics

### Phase 3: Automation (Week 5-6)
1. Integrate with Maven build
2. Add performance profile
3. Create automated reporting
4. Set up CI/CD integration

### Phase 4: Monitoring (Week 7-8)
1. Add JMH benchmarks for hot paths
2. Implement performance regression detection
3. Create performance dashboard
4. Document profiling best practices

---

## Tools and Resources

### Required Tools
- **JDK 17**: With VisualVM bundled
- **Maven 3.6+**: Build system
- **JUnit 4.13.2**: Test framework
- **JaCoCo 0.8.7**: Code coverage

### Optional Tools
- **JMH**: For micro-benchmarking
- **JProfiler/YourKit**: Advanced profiling (commercial)
- **GCViewer**: GC log analysis
- **Eclipse MAT**: Memory analyzer for heap dumps

### Documentation Links
- [JVisualVM Guide](https://visualvm.github.io/)
- [JMH Samples](http://hg.openjdk.java.net/code-tools/jmh/file/tip/jmh-samples/src/main/java/org/openjdk/jmh/samples/)
- [GC Tuning Guide](https://docs.oracle.com/en/java/javase/17/gctuning/)
- [Memory Management Best Practices](https://docs.oracle.com/cd/E13150_01/jrockit_jvm/jrockit/geninfo/diagnos/memman.html)

---

**Next Steps**: Proceed to implementation phases as memory optimization recommendations are identified in `memoryusage.md`.
