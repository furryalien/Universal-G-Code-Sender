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

package com.willwinder.universalgcodesender.visualizer;

import com.willwinder.universalgcodesender.model.Position;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for Pattern 2.2: Compact Line Segment Storage
 * 
 * Validates memory-efficient storage of line segments using primitive arrays
 * instead of individual LineSegment objects.
 * 
 * @author wwinder
 */
public class Pattern22CompactStorageTest {
    
    @Test
    public void testCreateEmptyStorage() {
        CompactLineSegmentStorage storage = new CompactLineSegmentStorage();
        
        assertEquals("Empty storage should have size 0", 0, storage.size());
        assertTrue("Empty storage should be empty", storage.isEmpty());
    }
    
    @Test
    public void testCreateWithInitialCapacity() {
        CompactLineSegmentStorage storage = new CompactLineSegmentStorage(100);
        
        assertEquals("New storage should have size 0", 0, storage.size());
        assertTrue("New storage should be empty", storage.isEmpty());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeCapacityThrows() {
        new CompactLineSegmentStorage(-1);
    }
    
    @Test
    public void testAddAndRetrieveSingleSegment() {
        CompactLineSegmentStorage storage = new CompactLineSegmentStorage();
        
        Position start = new Position(1.0, 2.0, 3.0);
        Position end = new Position(4.0, 5.0, 6.0);
        LineSegment original = new LineSegment(start, end, 10);
        original.setFeedRate(1500.0);
        original.setSpindleSpeed(12000.0);
        original.setIsZMovement(true);
        original.setIsArc(false);
        original.setIsFastTraverse(true);
        original.setIsRotation(false);
        
        storage.add(original);
        
        assertEquals("Storage should have 1 segment", 1, storage.size());
        assertFalse("Storage should not be empty", storage.isEmpty());
        
        LineSegment retrieved = storage.get(0);
        
        assertNotNull("Retrieved segment should not be null", retrieved);
        assertEquals("Start X should match", 1.0, retrieved.getStart().x, 0.0001);
        assertEquals("Start Y should match", 2.0, retrieved.getStart().y, 0.0001);
        assertEquals("Start Z should match", 3.0, retrieved.getStart().z, 0.0001);
        assertEquals("End X should match", 4.0, retrieved.getEnd().x, 0.0001);
        assertEquals("End Y should match", 5.0, retrieved.getEnd().y, 0.0001);
        assertEquals("End Z should match", 6.0, retrieved.getEnd().z, 0.0001);
        assertEquals("Line number should match", 10, retrieved.getLineNumber());
        assertEquals("Feed rate should match", 1500.0, retrieved.getFeedRate(), 0.0001);
        assertEquals("Spindle speed should match", 12000.0, retrieved.getSpindleSpeed(), 0.0001);
        assertTrue("Z movement flag should match", retrieved.isZMovement());
        assertFalse("Arc flag should match", retrieved.isArc());
        assertTrue("Fast traverse flag should match", retrieved.isFastTraverse());
        assertFalse("Rotation flag should match", retrieved.isRotation());
    }
    
    @Test
    public void testAddMultipleSegments() {
        CompactLineSegmentStorage storage = new CompactLineSegmentStorage(10);
        
        for (int i = 0; i < 5; i++) {
            Position start = new Position(i, i + 1, i + 2);
            Position end = new Position(i + 3, i + 4, i + 5);
            LineSegment segment = new LineSegment(start, end, i * 10);
            segment.setFeedRate(1000.0 + i * 100);
            segment.setIsArc(i % 2 == 0);
            
            storage.add(segment);
        }
        
        assertEquals("Should have 5 segments", 5, storage.size());
        
        // Verify each segment
        for (int i = 0; i < 5; i++) {
            LineSegment retrieved = storage.get(i);
            assertEquals("Line number should match", i * 10, retrieved.getLineNumber());
            assertEquals("Feed rate should match", 1000.0 + i * 100, retrieved.getFeedRate(), 0.0001);
            assertEquals("Arc flag should match", i % 2 == 0, retrieved.isArc());
        }
    }
    
    @Test
    public void testStorageGrowth() {
        // Start with small capacity to test growth
        CompactLineSegmentStorage storage = new CompactLineSegmentStorage(2);
        
        // Add more segments than initial capacity
        for (int i = 0; i < 10; i++) {
            Position start = new Position(i, 0, 0);
            Position end = new Position(i + 1, 0, 0);
            LineSegment segment = new LineSegment(start, end, i);
            storage.add(segment);
        }
        
        assertEquals("Should have 10 segments", 10, storage.size());
        
        // Verify all segments are intact
        for (int i = 0; i < 10; i++) {
            LineSegment retrieved = storage.get(i);
            assertEquals("Line number should match", i, retrieved.getLineNumber());
            assertEquals("Start X should match", (double) i, retrieved.getStart().x, 0.0001);
        }
    }
    
    @Test
    public void testClear() {
        CompactLineSegmentStorage storage = new CompactLineSegmentStorage();
        
        // Add some segments
        for (int i = 0; i < 5; i++) {
            Position start = new Position(i, 0, 0);
            Position end = new Position(i + 1, 0, 0);
            storage.add(new LineSegment(start, end, i));
        }
        
        assertEquals("Should have 5 segments before clear", 5, storage.size());
        
        storage.clear();
        
        assertEquals("Should have 0 segments after clear", 0, storage.size());
        assertTrue("Should be empty after clear", storage.isEmpty());
    }
    
    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetInvalidIndex_Negative() {
        CompactLineSegmentStorage storage = new CompactLineSegmentStorage();
        storage.get(-1);
    }
    
    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetInvalidIndex_TooLarge() {
        CompactLineSegmentStorage storage = new CompactLineSegmentStorage();
        Position start = new Position(0, 0, 0);
        Position end = new Position(1, 1, 1);
        storage.add(new LineSegment(start, end, 0));
        
        storage.get(1); // Index 1 when size is 1
    }
    
    @Test(expected = NullPointerException.class)
    public void testAddNullSegment() {
        CompactLineSegmentStorage storage = new CompactLineSegmentStorage();
        storage.add(null);
    }
    
    @Test
    public void testGetStartPositionOptimized() {
        CompactLineSegmentStorage storage = new CompactLineSegmentStorage();
        
        Position start = new Position(10.5, 20.5, 30.5);
        Position end = new Position(40.5, 50.5, 60.5);
        storage.add(new LineSegment(start, end, 1));
        
        Position result = new Position(0, 0, 0);
        storage.getStartPosition(0, result);
        
        assertEquals("Start X should match", 10.5, result.x, 0.0001);
        assertEquals("Start Y should match", 20.5, result.y, 0.0001);
        assertEquals("Start Z should match", 30.5, result.z, 0.0001);
    }
    
    @Test
    public void testGetEndPositionOptimized() {
        CompactLineSegmentStorage storage = new CompactLineSegmentStorage();
        
        Position start = new Position(10.5, 20.5, 30.5);
        Position end = new Position(40.5, 50.5, 60.5);
        storage.add(new LineSegment(start, end, 1));
        
        Position result = new Position(0, 0, 0);
        storage.getEndPosition(0, result);
        
        assertEquals("End X should match", 40.5, result.x, 0.0001);
        assertEquals("End Y should match", 50.5, result.y, 0.0001);
        assertEquals("End Z should match", 60.5, result.z, 0.0001);
    }
    
    @Test
    public void testGetLineNumber() {
        CompactLineSegmentStorage storage = new CompactLineSegmentStorage();
        
        Position start = new Position(0, 0, 0);
        Position end = new Position(1, 1, 1);
        storage.add(new LineSegment(start, end, 42));
        
        assertEquals("Line number should match", 42, storage.getLineNumber(0));
    }
    
    @Test
    public void testGetFeedRate() {
        CompactLineSegmentStorage storage = new CompactLineSegmentStorage();
        
        Position start = new Position(0, 0, 0);
        Position end = new Position(1, 1, 1);
        LineSegment segment = new LineSegment(start, end, 1);
        segment.setFeedRate(2500.75);
        storage.add(segment);
        
        assertEquals("Feed rate should match", 2500.75, storage.getFeedRate(0), 0.0001);
    }
    
    @Test
    public void testFlagAccessors() {
        CompactLineSegmentStorage storage = new CompactLineSegmentStorage();
        
        Position start = new Position(0, 0, 0);
        Position end = new Position(1, 1, 1);
        LineSegment segment = new LineSegment(start, end, 1);
        segment.setIsZMovement(true);
        segment.setIsArc(true);
        segment.setIsFastTraverse(false);
        segment.setIsRotation(true);
        storage.add(segment);
        
        assertTrue("Z movement should be true", storage.isZMovement(0));
        assertTrue("Arc should be true", storage.isArc(0));
        assertFalse("Fast traverse should be false", storage.isFastTraverse(0));
    }
    
    @Test
    public void testGetPositionsArray() {
        CompactLineSegmentStorage storage = new CompactLineSegmentStorage();
        
        Position start = new Position(1, 2, 3);
        Position end = new Position(4, 5, 6);
        storage.add(new LineSegment(start, end, 1));
        
        float[] positions = storage.getPositionsArray();
        int length = storage.getPositionDataLength();
        
        assertNotNull("Positions array should not be null", positions);
        assertEquals("Should have 6 floats per segment", 6, length);
        
        assertEquals("Position 0 should be start X", 1.0f, positions[0], 0.0001f);
        assertEquals("Position 1 should be start Y", 2.0f, positions[1], 0.0001f);
        assertEquals("Position 2 should be start Z", 3.0f, positions[2], 0.0001f);
        assertEquals("Position 3 should be end X", 4.0f, positions[3], 0.0001f);
        assertEquals("Position 4 should be end Y", 5.0f, positions[4], 0.0001f);
        assertEquals("Position 5 should be end Z", 6.0f, positions[5], 0.0001f);
    }
    
    @Test
    public void testMemoryUsageEstimate() {
        CompactLineSegmentStorage storage = new CompactLineSegmentStorage(100);
        
        long emptyUsage = storage.estimateMemoryUsage();
        assertTrue("Empty storage should have positive memory usage", emptyUsage > 0);
        
        // Add some segments
        for (int i = 0; i < 10; i++) {
            Position start = new Position(i, 0, 0);
            Position end = new Position(i + 1, 0, 0);
            storage.add(new LineSegment(start, end, i));
        }
        
        long usageWith10 = storage.estimateMemoryUsage();
        assertTrue("Storage with segments should have same or more memory", usageWith10 >= emptyUsage);
    }
    
    @Test
    public void testCompareMemoryUsage_ObjectVsCompact() {
        int segmentCount = 10000;
        
        // Estimate traditional storage: ArrayList of LineSegment objects
        // Each LineSegment: ~120 bytes (object overhead + 2 Position objects + primitives)
        // ArrayList overhead: ~40 bytes + array reference overhead
        long traditionalEstimate = 40 + (segmentCount * 120L);
        
        // Compact storage
        CompactLineSegmentStorage storage = new CompactLineSegmentStorage(segmentCount);
        for (int i = 0; i < segmentCount; i++) {
            Position start = new Position(i, i + 1, i + 2);
            Position end = new Position(i + 3, i + 4, i + 5);
            LineSegment segment = new LineSegment(start, end, i);
            segment.setFeedRate(1000.0 + i);
            storage.add(segment);
        }
        
        long compactUsage = storage.estimateMemoryUsage();
        
        // Compact storage should use significantly less memory
        assertTrue("Compact storage should use less than traditional storage",
                  compactUsage < traditionalEstimate);
        
        // Should be at least 40% savings
        double savingsPercent = ((double) (traditionalEstimate - compactUsage) / traditionalEstimate) * 100;
        assertTrue("Should achieve at least 40% memory savings, got " + savingsPercent + "%",
                  savingsPercent >= 40);
        
        System.out.println("Pattern 2.2 Memory Comparison for " + segmentCount + " segments:");
        System.out.println("  Traditional (ArrayList<LineSegment>): ~" + traditionalEstimate + " bytes");
        System.out.println("  Compact (primitive arrays): " + compactUsage + " bytes");
        System.out.println("  Savings: " + String.format("%.1f%%", savingsPercent));
    }
    
    @Test
    public void testPerformance_AddAndRetrieve() {
        int segmentCount = 100000;
        CompactLineSegmentStorage storage = new CompactLineSegmentStorage(segmentCount);
        
        // Test add performance
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < segmentCount; i++) {
            Position start = new Position(i, i + 1, i + 2);
            Position end = new Position(i + 3, i + 4, i + 5);
            LineSegment segment = new LineSegment(start, end, i);
            segment.setFeedRate(1000.0 + i);
            segment.setIsArc(i % 10 == 0);
            storage.add(segment);
        }
        long addTime = System.currentTimeMillis() - startTime;
        
        assertEquals("Should have all segments", segmentCount, storage.size());
        assertTrue("Add should complete in reasonable time (<5s)", addTime < 5000);
        
        // Test retrieve performance
        startTime = System.currentTimeMillis();
        for (int i = 0; i < segmentCount; i++) {
            LineSegment segment = storage.get(i);
            assertEquals("Line number should match", i, segment.getLineNumber());
        }
        long retrieveTime = System.currentTimeMillis() - startTime;
        
        assertTrue("Retrieve should complete in reasonable time (<5s)", retrieveTime < 5000);
        
        System.out.println("Pattern 2.2 Performance for " + segmentCount + " segments:");
        System.out.println("  Add time: " + addTime + "ms");
        System.out.println("  Retrieve time: " + retrieveTime + "ms");
    }
    
    @Test
    public void testOptimizedAccessPerformance() {
        int segmentCount = 100000;
        CompactLineSegmentStorage storage = new CompactLineSegmentStorage(segmentCount);
        
        for (int i = 0; i < segmentCount; i++) {
            Position start = new Position(i, i + 1, i + 2);
            Position end = new Position(i + 3, i + 4, i + 5);
            storage.add(new LineSegment(start, end, i));
        }
        
        // Test optimized position access (no object creation)
        Position reusablePos = new Position(0, 0, 0);
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < segmentCount; i++) {
            storage.getStartPosition(i, reusablePos);
            storage.getEndPosition(i, reusablePos);
        }
        long optimizedTime = System.currentTimeMillis() - startTime;
        
        // Test standard access (creates new LineSegment objects)
        startTime = System.currentTimeMillis();
        for (int i = 0; i < segmentCount; i++) {
            LineSegment segment = storage.get(i);
            Position start = segment.getStart();
            Position end = segment.getEnd();
        }
        long standardTime = System.currentTimeMillis() - startTime;
        
        assertTrue("Both methods should complete in reasonable time", 
                  optimizedTime < 5000 && standardTime < 5000);
        
        System.out.println("Pattern 2.2 Access Performance for " + segmentCount + " segments:");
        System.out.println("  Optimized (no object creation): " + optimizedTime + "ms");
        System.out.println("  Standard (creates objects): " + standardTime + "ms");
    }
    
    @Test
    public void testAllFlagCombinations() {
        CompactLineSegmentStorage storage = new CompactLineSegmentStorage();
        
        // Test all 16 combinations of 4 boolean flags
        for (int flags = 0; flags < 16; flags++) {
            Position start = new Position(flags, 0, 0);
            Position end = new Position(flags + 1, 0, 0);
            LineSegment segment = new LineSegment(start, end, flags);
            
            segment.setIsZMovement((flags & 1) != 0);
            segment.setIsArc((flags & 2) != 0);
            segment.setIsFastTraverse((flags & 4) != 0);
            segment.setIsRotation((flags & 8) != 0);
            
            storage.add(segment);
        }
        
        // Verify all combinations
        for (int i = 0; i < 16; i++) {
            LineSegment retrieved = storage.get(i);
            assertEquals("Z movement flag should match", (i & 1) != 0, retrieved.isZMovement());
            assertEquals("Arc flag should match", (i & 2) != 0, retrieved.isArc());
            assertEquals("Fast traverse flag should match", (i & 4) != 0, retrieved.isFastTraverse());
            assertEquals("Rotation flag should match", (i & 8) != 0, retrieved.isRotation());
        }
    }
    
    @Test
    public void testFloatPrecisionPreservation() {
        CompactLineSegmentStorage storage = new CompactLineSegmentStorage();
        
        // Test various precision values
        double[][] testValues = {
            {0.0, 0.0, 0.0},
            {0.001, 0.001, 0.001},
            {1.5, 2.5, 3.5},
            {-10.123, -20.456, -30.789},
            {999.999, 888.888, 777.777}
        };
        
        for (double[] values : testValues) {
            Position start = new Position(values[0], values[1], values[2]);
            Position end = new Position(values[0] + 1, values[1] + 1, values[2] + 1);
            storage.add(new LineSegment(start, end, 1));
        }
        
        // Verify precision (float has ~6-7 decimal digits of precision)
        for (int i = 0; i < testValues.length; i++) {
            LineSegment retrieved = storage.get(i);
            assertEquals("Start X precision", testValues[i][0], retrieved.getStart().x, 0.0001);
            assertEquals("Start Y precision", testValues[i][1], retrieved.getStart().y, 0.0001);
            assertEquals("Start Z precision", testValues[i][2], retrieved.getStart().z, 0.0001);
        }
    }
}
