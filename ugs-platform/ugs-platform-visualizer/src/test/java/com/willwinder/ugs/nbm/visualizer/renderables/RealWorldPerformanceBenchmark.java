/*
    Copyright 2024 Will Winder

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
import com.willwinder.universalgcodesender.visualizer.CompactLineSegmentStorage;
import com.willwinder.universalgcodesender.visualizer.LineSegment;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Real-world performance benchmarks comparing optimized vs legacy rendering paths.
 * Measures actual updateVertexBuffers() performance with various file sizes.
 * 
 * @author wwinder
 */
public class RealWorldPerformanceBenchmark {
    
    @Test
    public void benchmarkUpdateVertexBuffers_SmallFile() throws Exception {
        System.out.println("\n=== Benchmark: Small File (1,000 segments) ===");
        benchmarkRenderingPaths(1_000, 100);
    }
    
    @Test
    public void benchmarkUpdateVertexBuffers_MediumFile() throws Exception {
        System.out.println("\n=== Benchmark: Medium File (10,000 segments) ===");
        benchmarkRenderingPaths(10_000, 50);
    }
    
    @Test
    public void benchmarkUpdateVertexBuffers_LargeFile() throws Exception {
        System.out.println("\n=== Benchmark: Large File (50,000 segments) ===");
        benchmarkRenderingPaths(50_000, 20);
    }
    
    @Test
    public void benchmarkUpdateVertexBuffers_HugeFile() throws Exception {
        System.out.println("\n=== Benchmark: Huge File (100,000 segments) ===");
        benchmarkRenderingPaths(100_000, 10);
    }
    
    /**
     * Benchmarks both optimized and legacy rendering paths.
     * 
     * @param segmentCount number of line segments to render
     * @param iterations number of rendering iterations to average
     */
    private void benchmarkRenderingPaths(int segmentCount, int iterations) throws Exception {
        // Create test data
        List<LineSegment> segments = createTestSegments(segmentCount);
        
        // Setup mock backend
        BackendAPI mockBackend = Mockito.mock(BackendAPI.class);
        when(mockBackend.getWorkPosition()).thenReturn(new Position(0, 0, 0));
        
        // Benchmark optimized path
        long optimizedTime = benchmarkOptimizedPath(segments, mockBackend, iterations);
        
        // Benchmark legacy path
        long legacyTime = benchmarkLegacyPath(segments, mockBackend, iterations);
        
        // Calculate improvement
        double speedup = (double) legacyTime / optimizedTime;
        double improvement = ((legacyTime - optimizedTime) / (double) legacyTime) * 100;
        
        // Calculate expected FPS at different frame times
        double optimizedFps30 = 30.0 * (1.0 / (1.0 + (optimizedTime / 33.33)));
        double legacyFps30 = 30.0 * (1.0 / (1.0 + (legacyTime / 33.33)));
        
        System.out.println("Optimized path: " + optimizedTime + "ms (avg over " + iterations + " iterations)");
        System.out.println("Legacy path:    " + legacyTime + "ms (avg over " + iterations + " iterations)");
        System.out.println("Improvement:    " + String.format("%.1f%%", improvement) + (improvement >= 0 ? " faster" : " slower"));
        System.out.println("Speedup:        " + String.format("%.2fx", speedup));
        System.out.println("FPS impact:     Legacy=" + String.format("%.1f", legacyFps30) + 
                         " FPS, Optimized=" + String.format("%.1f", optimizedFps30) + " FPS @ 30Hz");
        
        // Note: Due to JVM variance, results may vary. Overall trend should show improvement.
        // Most important is that huge files (100K+) show significant improvement (30-40%)
    }
    
    /**
     * Benchmark the optimized rendering path using CompactLineSegmentStorage.
     */
    private long benchmarkOptimizedPath(List<LineSegment> segments, BackendAPI backend, int iterations) throws Exception {
        GcodeModel model = new GcodeModel("Optimized", backend);
        
        // Setup model with compact storage
        setupModelWithCompactStorage(model, segments, true);
        
        // Get the updateVertexBuffers method
        Method updateMethod = GcodeModel.class.getDeclaredMethod("updateVertexBuffers");
        updateMethod.setAccessible(true);
        
        // Warm up
        for (int i = 0; i < 5; i++) {
            updateMethod.invoke(model);
        }
        
        // Benchmark
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            updateMethod.invoke(model);
        }
        long totalTime = (System.nanoTime() - startTime) / 1_000_000; // Convert to ms
        
        return totalTime / iterations;
    }
    
    /**
     * Benchmark the legacy rendering path using List<LineSegment>.
     */
    private long benchmarkLegacyPath(List<LineSegment> segments, BackendAPI backend, int iterations) throws Exception {
        GcodeModel model = new GcodeModel("Legacy", backend);
        
        // Setup model without compact storage (legacy mode)
        setupModelWithCompactStorage(model, segments, false);
        
        // Get the updateVertexBuffers method
        Method updateMethod = GcodeModel.class.getDeclaredMethod("updateVertexBuffers");
        updateMethod.setAccessible(true);
        
        // Warm up
        for (int i = 0; i < 5; i++) {
            updateMethod.invoke(model);
        }
        
        // Benchmark
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            updateMethod.invoke(model);
        }
        long totalTime = (System.nanoTime() - startTime) / 1_000_000; // Convert to ms
        
        return totalTime / iterations;
    }
    
    /**
     * Setup GcodeModel with test data and configure for optimized or legacy mode.
     */
    private void setupModelWithCompactStorage(GcodeModel model, List<LineSegment> segments, boolean useOptimized) throws Exception {
        // Set gcodeLineList
        Field gcodeLineListField = GcodeModel.class.getDeclaredField("gcodeLineList");
        gcodeLineListField.setAccessible(true);
        gcodeLineListField.set(model, segments);
        
        // Set isDrawable
        Field isDrawableField = GcodeModel.class.getDeclaredField("isDrawable");
        isDrawableField.setAccessible(true);
        isDrawableField.set(model, true);
        
        // Set numberOfVertices
        Field numberOfVerticesField = GcodeModel.class.getDeclaredField("numberOfVertices");
        numberOfVerticesField.setAccessible(true);
        numberOfVerticesField.set(model, segments.size() * 2);
        
        // Allocate vertex and color buffers
        Field lineVertexDataField = GcodeModel.class.getDeclaredField("lineVertexData");
        lineVertexDataField.setAccessible(true);
        lineVertexDataField.set(model, new float[segments.size() * 2 * 3]);
        
        Field lineColorDataField = GcodeModel.class.getDeclaredField("lineColorData");
        lineColorDataField.setAccessible(true);
        lineColorDataField.set(model, new byte[segments.size() * 2 * 4]);
        
        if (useOptimized) {
            // Create compact storage
            CompactLineSegmentStorage compactStorage = new CompactLineSegmentStorage(segments.size());
            for (LineSegment ls : segments) {
                compactStorage.add(ls);
            }
            
            Field compactStorageField = GcodeModel.class.getDeclaredField("compactStorage");
            compactStorageField.setAccessible(true);
            compactStorageField.set(model, compactStorage);
            
            Field useCompactStorageField = GcodeModel.class.getDeclaredField("useCompactStorage");
            useCompactStorageField.setAccessible(true);
            useCompactStorageField.set(model, true);
        } else {
            // Disable compact storage for legacy mode
            Field useCompactStorageField = GcodeModel.class.getDeclaredField("useCompactStorage");
            useCompactStorageField.setAccessible(true);
            useCompactStorageField.set(model, false);
        }
    }
    
    /**
     * Create realistic test segments with varied properties.
     */
    private List<LineSegment> createTestSegments(int count) {
        List<LineSegment> segments = new ArrayList<>(count);
        
        double x = 0, y = 0, z = 0;
        for (int i = 0; i < count; i++) {
            // Simulate realistic toolpath patterns
            if (i % 100 == 0) {
                // Z movement every 100 segments
                z += 0.1;
            } else if (i % 50 == 0) {
                // Rapid traverse every 50 segments
                x += 10;
                y += 10;
            } else if (i % 7 == 0) {
                // Arc every 7 segments
                double angle = Math.toRadians(i * 5);
                x += Math.cos(angle);
                y += Math.sin(angle);
            } else {
                // Linear movement
                x += 0.5;
                y += 0.3;
            }
            
            Position start = new Position(x, y, z);
            Position end = new Position(x + 1, y + 1, z);
            
            LineSegment segment = new LineSegment(start, end, i);
            segment.setFeedRate(800.0 + (i % 500));
            segment.setSpindleSpeed(8000.0 + (i % 2000));
            segment.setIsZMovement(i % 100 == 0);
            segment.setIsArc(i % 7 == 0);
            segment.setIsFastTraverse(i % 50 == 0);
            
            segments.add(segment);
        }
        
        return segments;
    }
}
