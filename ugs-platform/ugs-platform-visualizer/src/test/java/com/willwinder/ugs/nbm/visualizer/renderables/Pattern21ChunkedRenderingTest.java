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
package com.willwinder.ugs.nbm.visualizer.renderables;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Pattern 2.1: Tests for chunked rendering functionality.
 * Validates RenderRange class used for partial/chunked rendering of large G-code files.
 * 
 * Memory Optimization: Enables loading only visible chunks instead of entire file,
 * reducing memory usage from O(n) to O(chunk_size) for visualization.
 * 
 * @author wwinder
 */
public class Pattern21ChunkedRenderingTest {
    
    @Test
    public void testRenderRange_BasicConstruction() {
        RenderRange range = new RenderRange(0, 100);
        
        assertEquals("Start should be 0", 0, range.getStart());
        assertEquals("End should be 100", 100, range.getEnd());
        assertEquals("Count should be 100", 100, range.getCount());
        assertFalse("Should not be ALL", range.isAll());
    }
    
    @Test
    public void testRenderRange_AllRange() {
        RenderRange range = RenderRange.ALL;
        
        assertEquals("Start should be 0", 0, range.getStart());
        assertEquals("End should be MAX_VALUE", Integer.MAX_VALUE, range.getEnd());
        assertTrue("Should be ALL", range.isAll());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRenderRange_NegativeStartThrows() {
        new RenderRange(-1, 100);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRenderRange_EndBeforeStartThrows() {
        new RenderRange(100, 50);
    }
    
    @Test
    public void testRenderRange_EmptyRange() {
        RenderRange range = new RenderRange(50, 50);
        
        assertEquals("Empty range should have count 0", 0, range.getCount());
        assertEquals("Start should equal end", range.getStart(), range.getEnd());
    }
    
    @Test
    public void testForChunk_FirstChunk() {
        RenderRange range = RenderRange.forChunk(0, 1000);
        
        assertEquals("First chunk should start at 0", 0, range.getStart());
        assertEquals("First chunk should end at 1000", 1000, range.getEnd());
        assertEquals("Chunk should have 1000 segments", 1000, range.getCount());
    }
    
    @Test
    public void testForChunk_SecondChunk() {
        RenderRange range = RenderRange.forChunk(1, 1000);
        
        assertEquals("Second chunk should start at 1000", 1000, range.getStart());
        assertEquals("Second chunk should end at 2000", 2000, range.getEnd());
        assertEquals("Chunk should have 1000 segments", 1000, range.getCount());
    }
    
    @Test
    public void testForChunk_MultipleChunks() {
        int chunkSize = 500;
        
        for (int i = 0; i < 10; i++) {
            RenderRange range = RenderRange.forChunk(i, chunkSize);
            
            assertEquals("Chunk " + i + " should start correctly", 
                i * chunkSize, range.getStart());
            assertEquals("Chunk " + i + " should end correctly", 
                (i + 1) * chunkSize, range.getEnd());
            assertEquals("Chunk " + i + " should have correct count", 
                chunkSize, range.getCount());
        }
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testForChunk_NegativeIndexThrows() {
        RenderRange.forChunk(-1, 1000);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testForChunk_ZeroSizeThrows() {
        RenderRange.forChunk(0, 0);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testForChunk_NegativeSizeThrows() {
        RenderRange.forChunk(0, -100);
    }
    
    @Test
    public void testClamp_WithinBounds() {
        RenderRange range = new RenderRange(100, 200);
        RenderRange clamped = range.clamp(500);
        
        assertEquals("Clamping within bounds should not change start", 100, clamped.getStart());
        assertEquals("Clamping within bounds should not change end", 200, clamped.getEnd());
    }
    
    @Test
    public void testClamp_ExceedsBounds() {
        RenderRange range = new RenderRange(100, 1000);
        RenderRange clamped = range.clamp(500);
        
        assertEquals("Start should remain unchanged", 100, clamped.getStart());
        assertEquals("End should be clamped to actual size", 500, clamped.getEnd());
        assertEquals("Count should reflect clamped size", 400, clamped.getCount());
    }
    
    @Test
    public void testClamp_StartExceedsBounds() {
        RenderRange range = new RenderRange(600, 1000);
        RenderRange clamped = range.clamp(500);
        
        assertEquals("Start should be clamped to actual size", 500, clamped.getStart());
        assertEquals("End should be clamped to actual size", 500, clamped.getEnd());
        assertEquals("Count should be 0 when fully outside", 0, clamped.getCount());
    }
    
    @Test
    public void testClamp_AllRange() {
        RenderRange range = RenderRange.ALL;
        RenderRange clamped = range.clamp(1000);
        
        assertEquals("Start should be 0", 0, clamped.getStart());
        assertEquals("End should be clamped to actual size", 1000, clamped.getEnd());
        assertEquals("Count should be actual size", 1000, clamped.getCount());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testClamp_NegativeSizeThrows() {
        RenderRange range = new RenderRange(0, 100);
        range.clamp(-1);
    }
    
    @Test
    public void testClamp_ZeroSize() {
        RenderRange range = new RenderRange(0, 100);
        RenderRange clamped = range.clamp(0);
        
        assertEquals("Start should be 0", 0, clamped.getStart());
        assertEquals("End should be 0", 0, clamped.getEnd());
        assertEquals("Count should be 0", 0, clamped.getCount());
    }
    
    @Test
    public void testEquals_SameValues() {
        RenderRange range1 = new RenderRange(100, 200);
        RenderRange range2 = new RenderRange(100, 200);
        
        assertEquals("Ranges with same values should be equal", range1, range2);
        assertEquals("Hash codes should match", range1.hashCode(), range2.hashCode());
    }
    
    @Test
    public void testEquals_DifferentValues() {
        RenderRange range1 = new RenderRange(100, 200);
        RenderRange range2 = new RenderRange(100, 300);
        
        assertNotEquals("Ranges with different values should not be equal", range1, range2);
    }
    
    @Test
    public void testEquals_SameInstance() {
        RenderRange range = new RenderRange(100, 200);
        
        assertEquals("Range should equal itself", range, range);
    }
    
    @Test
    public void testEquals_Null() {
        RenderRange range = new RenderRange(100, 200);
        
        assertNotEquals("Range should not equal null", range, null);
    }
    
    @Test
    public void testEquals_DifferentType() {
        RenderRange range = new RenderRange(100, 200);
        
        assertNotEquals("Range should not equal different type", range, "100-200");
    }
    
    @Test
    public void testToString_RegularRange() {
        RenderRange range = new RenderRange(100, 200);
        String str = range.toString();
        
        assertTrue("toString should contain start value", str.contains("100"));
        assertTrue("toString should contain end value", str.contains("200"));
    }
    
    @Test
    public void testToString_AllRange() {
        RenderRange range = RenderRange.ALL;
        String str = range.toString();
        
        assertTrue("toString should indicate ALL", str.contains("ALL"));
    }
    
    @Test
    public void testMemoryEfficiency_LargeFileSimulation() {
        // Simulate 100,000 line G-code file
        int totalLines = 100000;
        int chunkSize = 10000;
        int numChunks = (totalLines + chunkSize - 1) / chunkSize;
        
        // Verify we can create chunks to cover entire file
        int coveredLines = 0;
        for (int i = 0; i < numChunks; i++) {
            RenderRange chunk = RenderRange.forChunk(i, chunkSize);
            RenderRange clamped = chunk.clamp(totalLines);
            coveredLines += clamped.getCount();
        }
        
        assertEquals("All chunks should cover entire file", totalLines, coveredLines);
    }
    
    @Test
    public void testChunkCoverage_NoGaps() {
        int totalLines = 5000;
        int chunkSize = 1000;
        
        // Verify consecutive chunks have no gaps
        for (int i = 0; i < 4; i++) {
            RenderRange current = RenderRange.forChunk(i, chunkSize);
            RenderRange next = RenderRange.forChunk(i + 1, chunkSize);
            
            assertEquals("Chunks should be contiguous (no gaps)", 
                current.getEnd(), next.getStart());
        }
    }
    
    @Test
    public void testChunkCoverage_NoOverlaps() {
        int chunkSize = 1000;
        
        // Verify consecutive chunks don't overlap
        for (int i = 0; i < 10; i++) {
            RenderRange current = RenderRange.forChunk(i, chunkSize);
            RenderRange next = RenderRange.forChunk(i + 1, chunkSize);
            
            assertTrue("Next chunk should start where current ends (no overlap)",
                next.getStart() >= current.getEnd());
        }
    }
}
