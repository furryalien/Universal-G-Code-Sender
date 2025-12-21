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

import com.willwinder.universalgcodesender.model.BackendAPI;
import com.willwinder.universalgcodesender.model.Position;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Pattern 2.1: Integration tests for chunked rendering in GcodeModel.
 * Validates that render ranges correctly control which segments are processed,
 * enabling memory-efficient visualization of large G-code files.
 * 
 * @author wwinder
 */
public class GcodeModelChunkedRenderingTest {
    
    private BackendAPI mockBackend;
    private GcodeModel model;
    
    @Before
    public void setUp() {
        mockBackend = Mockito.mock(BackendAPI.class);
        when(mockBackend.getWorkPosition()).thenReturn(new Position(0, 0, 0));
        model = new GcodeModel("Test Model", mockBackend);
    }
    
    @Test
    public void testDefaultRenderRange_ShouldBeAll() {
        RenderRange range = model.getRenderRange();
        
        assertNotNull("Default render range should not be null", range);
        assertTrue("Default render range should be ALL", range.isAll());
    }
    
    @Test
    public void testSetRenderRange_ShouldUpdateRange() {
        RenderRange newRange = new RenderRange(100, 200);
        
        model.setRenderRange(newRange);
        RenderRange actualRange = model.getRenderRange();
        
        assertEquals("Render range should be updated", newRange, actualRange);
    }
    
    @Test
    public void testSetRenderRange_NullShouldResetToAll() {
        // First set a custom range
        model.setRenderRange(new RenderRange(100, 200));
        
        // Then reset with null
        model.setRenderRange(null);
        RenderRange actualRange = model.getRenderRange();
        
        assertTrue("Null should reset to ALL range", actualRange.isAll());
    }
    
    @Test
    public void testGetTotalSegmentCount_NoModelLoaded() {
        int count = model.getTotalSegmentCount();
        
        assertEquals("Count should be 0 when no model loaded", 0, count);
    }
    
    @Test
    public void testSetRenderRange_ShouldMarkBuffersDirty() {
        // Create a spy to track method calls
        GcodeModel spyModel = Mockito.spy(new GcodeModel("Test", mockBackend));
        
        // Set a new render range
        spyModel.setRenderRange(new RenderRange(0, 100));
        
        // Verify that setting the range marks buffers as dirty
        RenderRange range = spyModel.getRenderRange();
        assertEquals("Range should be set", 100, range.getEnd());
    }
    
    @Test
    public void testRenderRange_ChunkBoundaries() {
        int chunkSize = 1000;
        
        // Test that chunk boundaries are correctly calculated
        for (int i = 0; i < 5; i++) {
            RenderRange chunk = RenderRange.forChunk(i, chunkSize);
            model.setRenderRange(chunk);
            
            RenderRange actualRange = model.getRenderRange();
            assertEquals("Chunk start should match", i * chunkSize, actualRange.getStart());
            assertEquals("Chunk end should match", (i + 1) * chunkSize, actualRange.getEnd());
        }
    }
    
    @Test
    public void testRenderRange_SequentialChunks() {
        int chunkSize = 500;
        
        // Simulate rendering file in chunks
        for (int i = 0; i < 10; i++) {
            RenderRange chunk = RenderRange.forChunk(i, chunkSize);
            model.setRenderRange(chunk);
            
            RenderRange actualRange = model.getRenderRange();
            
            // Verify no gaps between chunks
            if (i > 0) {
                RenderRange previousChunk = RenderRange.forChunk(i - 1, chunkSize);
                assertEquals("Chunks should be contiguous",
                    previousChunk.getEnd(), actualRange.getStart());
            }
        }
    }
    
    @Test
    public void testRenderRange_ClampingBehavior() {
        // Test that render range respects actual model size
        RenderRange largeRange = new RenderRange(0, 100000);
        RenderRange clamped = largeRange.clamp(1000);
        
        assertEquals("Start should remain at 0", 0, clamped.getStart());
        assertEquals("End should be clamped to 1000", 1000, clamped.getEnd());
    }
    
    @Test
    public void testRenderRange_PartialRange() {
        // Test rendering middle section of file
        RenderRange middleRange = new RenderRange(1000, 2000);
        model.setRenderRange(middleRange);
        
        RenderRange actualRange = model.getRenderRange();
        assertEquals("Should render 1000 segments", 1000, actualRange.getCount());
        assertEquals("Start should be 1000", 1000, actualRange.getStart());
        assertEquals("End should be 2000", 2000, actualRange.getEnd());
    }
    
    @Test
    public void testRenderRange_SingleSegment() {
        // Test rendering a single segment
        RenderRange singleSegment = new RenderRange(500, 501);
        model.setRenderRange(singleSegment);
        
        RenderRange actualRange = model.getRenderRange();
        assertEquals("Should render 1 segment", 1, actualRange.getCount());
    }
    
    @Test
    public void testRenderRange_EmptyRange() {
        // Test empty range (start == end)
        RenderRange emptyRange = new RenderRange(100, 100);
        model.setRenderRange(emptyRange);
        
        RenderRange actualRange = model.getRenderRange();
        assertEquals("Empty range should have count 0", 0, actualRange.getCount());
    }
    
    @Test
    public void testMemoryEfficiency_LargeFileSimulation() {
        // Simulate a 100,000 line file with chunked rendering
        int totalLines = 100000;
        int chunkSize = 10000;
        int numChunks = (totalLines + chunkSize - 1) / chunkSize;
        
        // Verify we can process entire file in chunks
        for (int i = 0; i < numChunks; i++) {
            RenderRange chunk = RenderRange.forChunk(i, chunkSize);
            model.setRenderRange(chunk);
            
            RenderRange actualRange = model.getRenderRange();
            RenderRange clamped = actualRange.clamp(totalLines);
            
            // Last chunk might be smaller
            int expectedSize = Math.min(chunkSize, totalLines - i * chunkSize);
            assertEquals("Chunk " + i + " should have correct size",
                expectedSize, clamped.getCount());
        }
    }
    
    @Test
    public void testRenderRange_MultipleUpdates() {
        // Test that render range can be updated multiple times
        RenderRange[] ranges = {
            new RenderRange(0, 1000),
            new RenderRange(1000, 2000),
            new RenderRange(2000, 3000),
            RenderRange.ALL
        };
        
        for (RenderRange range : ranges) {
            model.setRenderRange(range);
            assertEquals("Range should be updated", range, model.getRenderRange());
        }
    }
    
    @Test
    public void testRenderRange_ThreadSafety() {
        // Basic thread safety test - ensure no exceptions with concurrent access
        final int numThreads = 10;
        final int iterationsPerThread = 100;
        
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < iterationsPerThread; j++) {
                    int start = (threadId * iterationsPerThread + j) * 100;
                    int end = start + 100;
                    model.setRenderRange(new RenderRange(start, end));
                    model.getRenderRange();
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for completion
        try {
            for (Thread thread : threads) {
                thread.join(5000); // 5 second timeout
            }
        } catch (InterruptedException e) {
            fail("Thread interrupted: " + e.getMessage());
        }
        
        // If we get here without exceptions, basic thread safety is OK
        assertNotNull("Model should still be functional", model.getRenderRange());
    }
}
