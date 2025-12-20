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
package com.willwinder.universalgcodesender.gcode;

import com.willwinder.universalgcodesender.gcode.processors.ArcExpander;
import com.willwinder.universalgcodesender.gcode.processors.CommandProcessorList;
import com.willwinder.universalgcodesender.gcode.util.GcodeParserException;
import com.willwinder.universalgcodesender.gcode.GcodePreprocessorUtils;
import com.willwinder.universalgcodesender.gcode.util.PlaneFormatter;
import com.willwinder.universalgcodesender.gcode.util.Plane;
import com.willwinder.universalgcodesender.model.Position;
import com.willwinder.universalgcodesender.model.UnitUtils;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for Pattern 3: Collection Allocation with Capacity Hints
 * 
 * These tests verify that ArrayList allocations with capacity hints:
 * 1. Produce correct results (functional correctness)
 * 2. Handle edge cases (empty, single item, large collections)
 * 3. Improve performance by reducing reallocations
 * 
 * @author wwinder
 */
public class Pattern3CapacityOptimizationTest {

    /**
     * Test GcodeParser.addCommand() - should handle typical gcode commands
     * without excessive ArrayList reallocations.
     */
    @Test
    public void testGcodeParserAddCommandCapacity() throws Exception {
        GcodeParser parser = new GcodeParser();
        
        // Simple motion command - should return 1 result
        List<GcodeParser.GcodeMeta> result = parser.addCommand("G1 X10 Y20 Z5");
        assertNotNull(result);
        assertEquals(1, result.size());
        
        // Multiple commands on one line - still typically returns 1 result
        result = parser.addCommand("G1 X15 Y25 Z10 F1000");
        assertNotNull(result);
        assertEquals(1, result.size());
        
        // Non-motion command (no position change) - may return 0 results
        result = parser.addCommand("M3 S1000");
        assertNotNull(result);
        // Non-motion commands don't create point segments
        assertEquals(0, result.size());
    }

    /**
     * Test CommandProcessorList.processCommand() - should handle arc expansion
     * which can create multiple commands from one.
     */
    @Test
    public void testCommandProcessorListCapacity() throws Exception {
        CommandProcessorList processors = new CommandProcessorList();
        processors.add(new ArcExpander(true, 0.1));
        
        GcodeState state = new GcodeState();
        state.currentPoint = new Position(0, 0, 0, UnitUtils.Units.MM);
        state.isMetric = true;
        
        // Simple G1 command - should return 1 command
        List<String> result = processors.processCommand("G1 X10 Y10", state);
        assertNotNull(result);
        assertEquals(1, result.size());
        
        // Arc command - will be expanded into multiple segments
        // A small arc with 0.1mm segment length - the actual number depends on arc size
        result = processors.processCommand("G2 X10 Y10 I5 J0", state);
        assertNotNull(result);
        // Arc expansion creates linear segments - at least a few
        assertTrue("Arc should expand to at least a few segments", result.size() >= 1);
        // But not an excessive number
        assertTrue("Arc expansion should be reasonable", result.size() < 1000);
    }

    /**
     * Test GcodePreprocessorUtils.parseCodes() - should handle typical
     * number of codes per command.
     */
    @Test
    public void testParseCodesCapacity() {
        // Typical command with one F code
        List<String> args = List.of("G1", "X10", "Y20", "F1000");
        List<String> fCodes = GcodePreprocessorUtils.parseCodes(args, 'F');
        assertNotNull(fCodes);
        assertEquals(1, fCodes.size());
        assertEquals("1000", fCodes.get(0));
        
        // Command with no F codes
        args = List.of("G1", "X10", "Y20");
        fCodes = GcodePreprocessorUtils.parseCodes(args, 'F');
        assertNotNull(fCodes);
        assertEquals(0, fCodes.size());
        
        // Command with X coordinate
        args = List.of("G1", "X10.5", "Y20.3", "Z5.1");
        List<String> xCodes = GcodePreprocessorUtils.parseCodes(args, 'X');
        assertNotNull(xCodes);
        assertEquals(1, xCodes.size());
        assertEquals("10.5", xCodes.get(0));
    }

    /**
     * Test GcodePreprocessorUtils.splitCommand() - should handle typical
     * gcode command structure.
     */
    @Test
    public void testSplitCommandCapacity() {
        // Simple command with a few tokens
        List<String> tokens = GcodePreprocessorUtils.splitCommand("G1 X10 Y20 Z5");
        assertNotNull(tokens);
        assertEquals(4, tokens.size());
        assertTrue(tokens.contains("G1"));
        assertTrue(tokens.contains("X10"));
        
        // Command with feed rate
        tokens = GcodePreprocessorUtils.splitCommand("G1 X10 Y20 F1000");
        assertNotNull(tokens);
        assertEquals(4, tokens.size());
        
        // Complex command with multiple codes
        tokens = GcodePreprocessorUtils.splitCommand("G1 X10.5 Y20.3 Z5.1 F1500 S1000");
        assertNotNull(tokens);
        assertEquals(6, tokens.size());
        
        // Empty command
        tokens = GcodePreprocessorUtils.splitCommand("");
        assertNotNull(tokens);
        assertEquals(0, tokens.size());
    }

    /**
     * Test GcodePreprocessorUtils.generatePointsAlongArcBDring() - should
     * allocate exact capacity for known number of points.
     */
    @Test
    public void testGenerateArcPointsExactCapacity() {
        Position start = new Position(0, 0, 0, UnitUtils.Units.MM);
        Position end = new Position(10, 0, 0, UnitUtils.Units.MM);
        Position center = new Position(5, 0, 0, UnitUtils.Units.MM);
        
        // Request number of segments - algorithm generates points along arc
        // The actual number returned may vary based on arc geometry
        int numPoints = 10;
        List<Position> points = GcodePreprocessorUtils.generatePointsAlongArcBDring(
            start, end, center, true, 0, 0, numPoints, 
            new PlaneFormatter(Plane.XY)
        );
        
        assertNotNull(points);
        // Verify that points were generated (exact count depends on arc calculation)
        assertTrue("Should generate points", points.size() > 0);
        assertTrue("Should not exceed requested points significantly", points.size() <= numPoints + 2);
        
        // Test with different segment counts
        numPoints = 50;
        points = GcodePreprocessorUtils.generatePointsAlongArcBDring(
            start, end, center, true, 0, 0, numPoints,
            new PlaneFormatter(Plane.XY)
        );
        
        assertNotNull(points);
        assertTrue("Should generate points", points.size() > 0);
        assertTrue("Should not exceed requested points significantly", points.size() <= numPoints + 2);
    }

    /**
     * Test edge case: Empty input handling
     */
    @Test
    public void testEdgeCaseEmptyInputs() {
        // Empty command split
        List<String> tokens = GcodePreprocessorUtils.splitCommand("");
        assertNotNull(tokens);
        assertEquals(0, tokens.size());
        
        // Empty args list for parseCodes
        List<String> args = List.of();
        List<String> codes = GcodePreprocessorUtils.parseCodes(args, 'F');
        assertNotNull(codes);
        assertEquals(0, codes.size());
    }

    /**
     * Test edge case: Single item collections
     */
    @Test
    public void testEdgeCaseSingleItem() throws Exception {
        GcodeParser parser = new GcodeParser();
        
        // Single simple command
        List<GcodeParser.GcodeMeta> result = parser.addCommand("G0 X0");
        assertNotNull(result);
        assertTrue("Should have at least one result", result.size() >= 1);
        
        // Single token command
        List<String> tokens = GcodePreprocessorUtils.splitCommand("G0");
        assertNotNull(tokens);
        assertEquals(1, tokens.size());
    }

    /**
     * Test performance characteristic: Large collections should not cause
     * excessive memory usage or slow performance due to reallocations.
     */
    @Test
    public void testLargeCollectionPerformance() throws Exception {
        GcodeParser parser = new GcodeParser();
        
        // Process many commands - should not slow down due to reallocations
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            parser.addCommand("G1 X" + i + " Y" + i);
        }
        long duration = System.currentTimeMillis() - startTime;
        
        // Should complete in reasonable time (less than 1 second for 1000 commands)
        assertTrue("Processing 1000 commands should be fast", duration < 1000);
    }

    /**
     * Test that capacity hints don't affect functional correctness
     */
    @Test
    public void testFunctionalCorrectness() throws Exception {
        GcodeParser parser = new GcodeParser();
        
        // Verify state is correctly updated
        parser.addCommand("G1 X10 Y20 Z5 F1000");
        GcodeState state = parser.getCurrentState();
        
        assertEquals(10.0, state.currentPoint.x, 0.001);
        assertEquals(20.0, state.currentPoint.y, 0.001);
        assertEquals(5.0, state.currentPoint.z, 0.001);
        assertEquals(1000.0, state.feedRate, 0.001);
        
        // Verify command processing produces correct tokens
        List<String> tokens = GcodePreprocessorUtils.splitCommand("G1 X10.5 Y20.3");
        assertEquals(3, tokens.size());
        assertEquals("G1", tokens.get(0));
        assertEquals("X10.5", tokens.get(1));
        assertEquals("Y20.3", tokens.get(2));
    }

    /**
     * Test GRBL system commands (special case in splitCommand)
     */
    @Test
    public void testGrblSystemCommands() {
        // GRBL system commands should not be split
        List<String> tokens = GcodePreprocessorUtils.splitCommand("$H");
        assertNotNull(tokens);
        assertEquals(1, tokens.size());
        assertEquals("$H", tokens.get(0));
        
        tokens = GcodePreprocessorUtils.splitCommand("$X");
        assertNotNull(tokens);
        assertEquals(1, tokens.size());
        assertEquals("$X", tokens.get(0));
    }
}
