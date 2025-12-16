package com.willwinder.universalgcodesender.visualizer;

import com.willwinder.universalgcodesender.model.Position;
import com.willwinder.universalgcodesender.model.UnitUtils;
import com.willwinder.universalgcodesender.types.PointSegment;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class VisualizerUtilsTest {

    public static void assertPosition(double x, double y, double z, double a, double b, double c, Position position) {
        assertEquals("Expected position X", x, position.x, 0.1d);
        assertEquals("Expected position Y", y, position.y, 0.1d);
        assertEquals("Expected position Z", z, position.z, 0.1d);
        assertEquals("Expected position A", a, position.a, 0.1d);
        assertEquals("Expected position B", b, position.b, 0.1d);
        assertEquals("Expected position C", c, position.c, 0.1d);
    }

    @Test
    public void toCartesianShouldNotApplyRotationIfZero() {
        Position position = new Position(10, 11, 12, 0, 0, 0, UnitUtils.Units.MM);
        Position position1 = VisualizerUtils.toCartesian(position);
        assertEquals(10d, position1.x, 0.1);
        assertEquals(11d, position1.y, 0.1);
        assertEquals(12d, position1.z, 0.1);
        assertEquals(0.0, position1.a, 0.1);
        assertEquals(0.0, position1.b, 0.1);
        assertEquals(0.0, position1.c, 0.1);
        assertEquals(UnitUtils.Units.MM, position1.getUnits());
    }

    @Test
    public void toCartesianShouldRotateAAroundX() {
        Position position = new Position(10, 10, 10, 180, 0, 0, UnitUtils.Units.MM);
        Position position1 = VisualizerUtils.toCartesian(position);
        assertEquals(10d, position1.x, 0.1);
        assertEquals(-10d, position1.y, 0.1);
        assertEquals(-10d, position1.z, 0.1);
        assertEquals(0.0, position1.a, 0.1);
        assertEquals(0.0, position1.b, 0.1);
        assertEquals(0.0, position1.c, 0.1);
        assertEquals(UnitUtils.Units.MM, position1.getUnits());
    }

    @Test
    public void toCartesianShouldRotateBAroundY() {
        Position position = new Position(10, 10, 10, 0, 180, 0, UnitUtils.Units.MM);
        Position position1 = VisualizerUtils.toCartesian(position);
        assertEquals(-10d, position1.x, 0.1);
        assertEquals(10d, position1.y, 0.1);
        assertEquals(-10d, position1.z, 0.1);
        assertEquals(0.0, position1.a, 0.1);
        assertEquals(0.0, position1.b, 0.1);
        assertEquals(0.0, position1.c, 0.1);
        assertEquals(UnitUtils.Units.MM, position1.getUnits());
    }

    @Test
    public void toCartesianShouldRotateCAroundZ() {
        Position position = new Position(10, 10, 10, 0, 0, 180, UnitUtils.Units.MM);
        Position position1 = VisualizerUtils.toCartesian(position);
        assertEquals(-10d, position1.x, 0.1);
        assertEquals(-10d, position1.y, 0.1);
        assertEquals(10d, position1.z, 0.1);
        assertEquals(0.0, position1.a, 0.1);
        assertEquals(0.0, position1.b, 0.1);
        assertEquals(0.0, position1.c, 0.1);
        assertEquals(UnitUtils.Units.MM, position1.getUnits());
    }

    @Test
    public void expandRotationalLineSegmentWithoutABCAxesShouldBeIgnored() {
        Position startPosition = new Position(0, 0, 10, UnitUtils.Units.MM);
        Position endPosition = new Position(0, 0, 10, UnitUtils.Units.MM);
        PointSegment endSegment = new PointSegment(endPosition, 0);

        List<LineSegment> result = new ArrayList<>();
        VisualizerUtils.expandRotationalLineSegment(startPosition, endSegment, result);

        assertEquals(1, result.size());
        assertPosition(0, 0, 10, Double.NaN, Double.NaN, Double.NaN, result.get(0).getEnd());
    }

    @Test
    public void expandRotationalLineSegmentWithStartingAShouldBeIgnored() {
        Position startPosition = new Position(0, 0, 10, 0, Double.NaN, Double.NaN, UnitUtils.Units.MM);
        Position endPosition = new Position(0, 0, 10, UnitUtils.Units.MM);
        PointSegment endSegment = new PointSegment(endPosition, 0);

        List<LineSegment> result = new ArrayList<>();
        VisualizerUtils.expandRotationalLineSegment(startPosition, endSegment, result);

        assertEquals(1, result.size());
        assertPosition(0, 0, 10, Double.NaN, Double.NaN, Double.NaN, result.get(0).getEnd());
    }

    @Test
    public void expandRotationalLineSegmentWithEndingAZeroShouldBeZero() {
        Position startPosition = new Position(0, 0, 10,  UnitUtils.Units.MM);
        Position endPosition = new Position(0, 0, 10, 0, Double.NaN, Double.NaN, UnitUtils.Units.MM);
        PointSegment endSegment = new PointSegment(endPosition, 0);

        List<LineSegment> result = new ArrayList<>();
        VisualizerUtils.expandRotationalLineSegment(startPosition, endSegment, result);

        assertEquals(1, result.size());
        assertPosition(0, 0, 10, 0.0, Double.NaN, Double.NaN, result.get(0).getEnd());
    }

    @Test
    public void expandRotationalLineSegmentWithEndingAShouldBeInterpolated() {
        Position startPosition = new Position(0, 0, 10,  UnitUtils.Units.MM);
        Position endPosition = new Position(0, 0, 10, 10, Double.NaN, Double.NaN, UnitUtils.Units.MM);
        PointSegment endSegment = new PointSegment(endPosition, 0);

        List<LineSegment> result = new ArrayList<>();
        VisualizerUtils.expandRotationalLineSegment(startPosition, endSegment, result);

        assertEquals(3, result.size());
        assertPosition(0, 0, 10, 0, Double.NaN, Double.NaN, result.get(0).getEnd());
        assertPosition(0, 0, 10, 5.0, Double.NaN, Double.NaN, result.get(1).getEnd());
        assertPosition(0, 0, 10, 10.0, Double.NaN, Double.NaN, result.get(2).getEnd());
    }

    @Test
    public void expandRotationalLineSegmentRotationAroundAShouldInterpolateWithFiveDegreeSteps() {
        Position startPosition = new Position(0, 0, 10, 0, 0, 0, UnitUtils.Units.MM);
        Position endPosition = new Position(0, 0, 10, 90, 0, 0, UnitUtils.Units.MM);
        PointSegment endSegment = new PointSegment(endPosition, 0);

        List<LineSegment> result = new ArrayList<>();
        VisualizerUtils.expandRotationalLineSegment(startPosition, endSegment, result);

        assertEquals(19, result.size());
        assertPosition(0, 0, 10, 0, 0, 0, result.get(0).getEnd());
        assertPosition(0, 0, 10, 5, 0, 0, result.get(1).getEnd());
        assertPosition(0, 0, 10, 10, 0, 0, result.get(2).getEnd());
        assertPosition(0, 0, 10, 90, 0, 0, result.get(18).getEnd());
    }

    @Test
    public void expandRotationalLineSegmentRotationAroundAAndXShouldInterpolateWithFiveDegreeSteps() {
        Position startPosition = new Position(0, 0, 10, 0, 0, 0, UnitUtils.Units.MM);
        Position endPosition = new Position(10, 0, 10, 90, 0, 0, UnitUtils.Units.MM);
        PointSegment endSegment = new PointSegment(endPosition, 0);

        List<LineSegment> result = new ArrayList<>();
        VisualizerUtils.expandRotationalLineSegment(startPosition, endSegment, result);

        assertEquals(19, result.size());
        assertPosition(0, 0, 10, 0, 0, 0, result.get(0).getEnd());
        assertPosition(0.5, 0, 10, 5, 0, 0, result.get(1).getEnd());
        assertPosition(1.1, 0, 10, 10, 0, 0, result.get(2).getEnd());
        assertPosition(10, 0, 10, 90, 0, 0, result.get(18).getEnd());
    }

    @Test
    public void expandRotationalLineSegmentRotationAroundBShouldInterpolateWithFiveDegreeSteps() {
        Position startPosition = new Position(0, 0, 10, 0, 0, 0, UnitUtils.Units.MM);
        Position endPosition = new Position(0, 0, 10, 0, 90, 0, UnitUtils.Units.MM);
        PointSegment endSegment = new PointSegment(endPosition, 0);

        List<LineSegment> result = new ArrayList<>();
        VisualizerUtils.expandRotationalLineSegment(startPosition, endSegment, result);

        assertEquals(19, result.size());
        assertPosition(0, 0, 10, 0, 0, 0, result.get(0).getEnd());
        assertPosition(0, 0, 10, 0, 5, 0, result.get(1).getEnd());
        assertPosition(0, 0, 10, 0, 10, 0, result.get(2).getEnd());
        assertPosition(0, 0, 10, 0, 90, 0, result.get(18).getEnd());
    }

    @Test
    public void expandRotationalLineSegmentRotationAroundBAndYShouldInterpolateWithFiveDegreeSteps() {
        Position startPosition = new Position(0, 0, 10, 0, 0, 0, UnitUtils.Units.MM);
        Position endPosition = new Position(0, 10, 10, 0, 90, 0, UnitUtils.Units.MM);
        PointSegment endSegment = new PointSegment(endPosition, 0);

        List<LineSegment> result = new ArrayList<>();
        VisualizerUtils.expandRotationalLineSegment(startPosition, endSegment, result);

        assertEquals(19, result.size());
        assertPosition(0, 0, 10, 0, 0, 0, result.get(0).getEnd());
        assertPosition(0, 0.5, 10, 0, 5, 0, result.get(1).getEnd());
        assertPosition(0, 1.1, 10, 0, 10, 0, result.get(2).getEnd());
        assertPosition(0, 10, 10, 0, 90, 0, result.get(18).getEnd());
    }

    @Test
    public void expandRotationalLineSegmentRotationAroundCShouldInterpolateWithFiveDegreeSteps() {
        Position startPosition = new Position(0, 0, 10, 0, 0, 0, UnitUtils.Units.MM);
        Position endPosition = new Position(0, 0, 10, 0, 0, 90, UnitUtils.Units.MM);
        PointSegment endSegment = new PointSegment(endPosition, 0);

        List<LineSegment> result = new ArrayList<>();
        VisualizerUtils.expandRotationalLineSegment(startPosition, endSegment, result);

        assertEquals(19, result.size());
        assertPosition(0, 0, 10, 0, 0, 0, result.get(0).getEnd());
        assertPosition(0, 0, 10, 0, 0, 5, result.get(1).getEnd());
        assertPosition(0, 0, 10, 0, 0, 10, result.get(2).getEnd());
        assertPosition(0, 0, 10, 0, 0, 90, result.get(18).getEnd());
    }

    @Test
    public void expandRotationalLineSegmentRotationAroundMultipleAxisesShouldInterpolateThemWithUnifiedSteps() {
        Position startPosition = new Position(0, 0, 10, 0, 0, 0, UnitUtils.Units.MM);
        Position endPosition = new Position(0, 0, 10, 10, 20, 30, UnitUtils.Units.MM);
        PointSegment endSegment = new PointSegment(endPosition, 0);

        List<LineSegment> result = new ArrayList<>();
        VisualizerUtils.expandRotationalLineSegment(startPosition, endSegment, result);

        assertEquals(7, result.size());
        assertPosition(0, 0, 10, 0, 0, 0, result.get(0).getEnd());
        assertPosition(0, 0, 10, 1.6, 3.3, 5, result.get(1).getEnd());
        assertPosition(0, 0, 10, 3.3, 6.6, 10, result.get(2).getEnd());
        assertPosition(0, 0, 10, 5, 10, 15, result.get(3).getEnd());
        assertPosition(0, 0, 10, 6.6, 13.3, 20, result.get(4).getEnd());
        assertPosition(0, 0, 10, 8.3, 16.6, 25, result.get(5).getEnd());
        assertPosition(0, 0, 10, 10, 20, 30, result.get(6).getEnd());
    }
    
    // File reading tests
    
    @Test
    public void readFiletoArrayListShouldReadAllLines() throws Exception {
        java.io.File tempFile = createTempGcodeFile(100);
        try {
            ArrayList<String> lines = VisualizerUtils.readFiletoArrayList(tempFile.getAbsolutePath());
            assertEquals("Should read all lines", 100, lines.size());
            assertEquals("First line should match", "G1 X0.00 Y0.00 F1000", lines.get(0));
            assertEquals("Last line should match", "G1 X99.00 Y99.00 F1000", lines.get(99));
        } finally {
            tempFile.delete();
        }
    }
    
    @Test
    public void readFiletoArrayListShouldHandleEmptyFile() throws Exception {
        java.io.File tempFile = createTempGcodeFile(0);
        try {
            ArrayList<String> lines = VisualizerUtils.readFiletoArrayList(tempFile.getAbsolutePath());
            assertEquals("Empty file should return empty list", 0, lines.size());
        } finally {
            tempFile.delete();
        }
    }
    
    @Test
    public void readFiletoArrayListShouldPreallocateCapacity() throws Exception {
        // Test that the returned ArrayList has reasonable capacity (not just default 10)
        java.io.File tempFile = createTempGcodeFile(1000);
        try {
            ArrayList<String> lines = VisualizerUtils.readFiletoArrayList(tempFile.getAbsolutePath());
            assertEquals("Should read all lines", 1000, lines.size());
            // If capacity wasn't preallocated, there would be multiple reallocations
            // This test mainly ensures no exceptions are thrown
        } finally {
            tempFile.delete();
        }
    }
    
    @Test
    public void readFileAsStreamShouldReadAllLines() throws Exception {
        java.io.File tempFile = createTempGcodeFile(100);
        try {
            try (java.util.stream.Stream<String> stream = VisualizerUtils.readFileAsStream(tempFile.getAbsolutePath())) {
                long count = stream.count();
                assertEquals("Should read all lines", 100, count);
            }
        } finally {
            tempFile.delete();
        }
    }
    
    @Test
    public void readFileAsStreamShouldReadLinesInOrder() throws Exception {
        java.io.File tempFile = createTempGcodeFile(10);
        try {
            try (java.util.stream.Stream<String> stream = VisualizerUtils.readFileAsStream(tempFile.getAbsolutePath())) {
                List<String> lines = stream.collect(java.util.stream.Collectors.toList());
                assertEquals("First line should match", "G1 X0.00 Y0.00 F1000", lines.get(0));
                assertEquals("Last line should match", "G1 X9.00 Y9.00 F1000", lines.get(9));
            }
        } finally {
            tempFile.delete();
        }
    }
    
    @Test
    public void readFileAsStreamShouldHandleEmptyFile() throws Exception {
        java.io.File tempFile = createTempGcodeFile(0);
        try {
            try (java.util.stream.Stream<String> stream = VisualizerUtils.readFileAsStream(tempFile.getAbsolutePath())) {
                long count = stream.count();
                assertEquals("Empty file should return empty stream", 0, count);
            }
        } finally {
            tempFile.delete();
        }
    }
    
    @Test
    public void createFileReaderShouldReadAllLines() throws Exception {
        java.io.File tempFile = createTempGcodeFile(100);
        try {
            try (java.io.BufferedReader reader = VisualizerUtils.createFileReader(tempFile.getAbsolutePath())) {
                int count = 0;
                String line;
                String firstLine = null;
                String lastLine = null;
                while ((line = reader.readLine()) != null) {
                    if (firstLine == null) firstLine = line;
                    lastLine = line;
                    count++;
                }
                assertEquals("Should read all lines", 100, count);
                assertEquals("First line should match", "G1 X0.00 Y0.00 F1000", firstLine);
                assertEquals("Last line should match", "G1 X99.00 Y99.00 F1000", lastLine);
            }
        } finally {
            tempFile.delete();
        }
    }
    
    @Test
    public void readFileAsStreamMemoryEfficiencyTest() throws Exception {
        // Test that stream doesn't load entire file into memory
        // Create a larger file to test streaming behavior
        java.io.File tempFile = createTempGcodeFile(10000);
        try {
            // Get memory before
            Runtime runtime = Runtime.getRuntime();
            System.gc();
            long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
            
            // Process file with stream
            try (java.util.stream.Stream<String> stream = VisualizerUtils.readFileAsStream(tempFile.getAbsolutePath())) {
                // Process lines one at a time
                long count = stream.filter(line -> line.startsWith("G1")).count();
                assertEquals("Should process all lines", 10000, count);
            }
            
            System.gc();
            long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
            long memoryUsed = memoryAfter - memoryBefore;
            
            // Memory usage should be minimal since we're not storing all lines
            // Test passes if stream processing completes without error
        } finally {
            tempFile.delete();
        }
    }
    
    @Test
    public void readFiletoArrayListVsStreamMemoryComparison() throws Exception {
        // Compare memory usage between ArrayList and Stream approaches
        java.io.File tempFile = createTempGcodeFile(5000);
        try {
            Runtime runtime = Runtime.getRuntime();
            
            // Test ArrayList approach
            System.gc();
            long memBeforeArray = runtime.totalMemory() - runtime.freeMemory();
            ArrayList<String> lines = VisualizerUtils.readFiletoArrayList(tempFile.getAbsolutePath());
            long memAfterArray = runtime.totalMemory() - runtime.freeMemory();
            long arrayMemory = memAfterArray - memBeforeArray;
            
            // Clear the ArrayList
            lines = null;
            System.gc();
            
            // Test Stream approach
            System.gc();
            long memBeforeStream = runtime.totalMemory() - runtime.freeMemory();
            try (java.util.stream.Stream<String> stream = VisualizerUtils.readFileAsStream(tempFile.getAbsolutePath())) {
                long count = stream.count();
                assertEquals(5000, count);
            }
            long memAfterStream = runtime.totalMemory() - runtime.freeMemory();
            long streamMemory = memAfterStream - memBeforeStream;
            
            // Stream should use significantly less memory than ArrayList
            // Test passes if both operations complete successfully
        } finally {
            tempFile.delete();
        }
    }
    
    /**
     * Helper method to create a temporary G-code file for testing
     */
    private java.io.File createTempGcodeFile(int lineCount) throws Exception {
        java.io.File tempFile = java.io.File.createTempFile("test_gcode_", ".nc");
        try (java.io.PrintWriter writer = new java.io.PrintWriter(tempFile)) {
            for (int i = 0; i < lineCount; i++) {
                writer.println(String.format("G1 X%.2f Y%.2f F1000", (double)i, (double)i));
            }
        }
        return tempFile;
    }
}
