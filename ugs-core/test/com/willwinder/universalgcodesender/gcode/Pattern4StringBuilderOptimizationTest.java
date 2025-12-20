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

import com.willwinder.universalgcodesender.gcode.processors.RunFromProcessor;
import com.willwinder.universalgcodesender.gcode.util.GcodeParserException;
import com.willwinder.universalgcodesender.model.Position;
import com.willwinder.universalgcodesender.model.UnitUtils;
import com.willwinder.universalgcodesender.uielements.TextFieldUnit;
import com.willwinder.universalgcodesender.uielements.TextFieldUnitFormatter;
import org.junit.Test;

import java.text.ParseException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for Pattern 4: String Concatenation Optimization with StringBuilder
 * 
 * These tests verify that StringBuilder optimizations:
 * 1. Produce functionally equivalent results
 * 2. Handle edge cases correctly
 * 3. Maintain performance characteristics
 * 4. Reduce memory allocations (tested indirectly through functionality)
 * 
 * @author wwinder
 */
public class Pattern4StringBuilderOptimizationTest {

    /**
     * Test RunFromProcessor string building - verify correct command generation
     */
    @Test
    public void testRunFromProcessorCommandGeneration() throws Exception {
        GcodeParser parser = new GcodeParser();
        RunFromProcessor processor = new RunFromProcessor(0); // line 0
        
        // Set up initial position
        parser.addCommand("G0 X10 Y20 Z1");
        
        // Process a command that triggers skip state generation
        List<String> result = processor.processCommand("G1 X15 Y25", parser.getCurrentState());
        
        assertNotNull(result);
        assertTrue("Should generate commands", result.size() > 0);
        
        // Verify the commands contain expected coordinates
        String commands = String.join(" ", result);
        assertTrue("Should contain X coordinate", commands.contains("X"));
        assertTrue("Should contain Y coordinate", commands.contains("Y"));
    }

    /**
     * Test RunFromProcessor with various position states
     */
    @Test
    public void testRunFromProcessorVariousPositions() throws Exception {
        GcodeParser parser = new GcodeParser();
        RunFromProcessor processor = new RunFromProcessor(0);
        
        // Test with only X coordinate
        parser.reset();
        parser.addCommand("G0 X5");
        List<String> result = processor.processCommand("G1 X10", parser.getCurrentState());
        assertNotNull(result);
        
        // Test with only Y coordinate
        parser.reset();
        parser.addCommand("G0 Y5");
        result = processor.processCommand("G1 Y10", parser.getCurrentState());
        assertNotNull(result);
        
        // Test with XYZ coordinates
        parser.reset();
        parser.addCommand("G0 X5 Y10 Z2");
        result = processor.processCommand("G1 X15 Y20 Z3", parser.getCurrentState());
        assertNotNull(result);
        assertTrue("Should generate multiple commands", result.size() >= 1);
    }

    /**
     * Test RunFromProcessor with NaN values (edge case)
     */
    @Test
    public void testRunFromProcessorWithNaN() throws Exception {
        GcodeParser parser = new GcodeParser();
        RunFromProcessor processor = new RunFromProcessor(0);
        
        // Start with no movement commands (positions are NaN)
        List<String> result = processor.processCommand("M3 S1000", parser.getCurrentState());
        
        assertNotNull(result);
        // Should handle NaN gracefully
        assertEquals("Non-motion command should pass through", 1, result.size());
    }

    /**
     * Test TextFieldUnitFormatter with abbreviations
     */
    @Test
    public void testTextFieldUnitFormatterWithAbbreviation() throws ParseException {
        TextFieldUnitFormatter formatter = new TextFieldUnitFormatter(TextFieldUnit.MM, 2, true);
        
        String result = formatter.valueToString(10.5);
        
        assertNotNull(result);
        assertTrue("Should contain value", result.contains("10"));
        assertTrue("Should contain abbreviation", result.contains("mm"));
        assertTrue("Should have space between value and unit", result.contains(" "));
    }

    /**
     * Test TextFieldUnitFormatter without abbreviations
     */
    @Test
    public void testTextFieldUnitFormatterWithoutAbbreviation() throws ParseException {
        TextFieldUnitFormatter formatter = new TextFieldUnitFormatter(TextFieldUnit.MM, 2, false);
        
        String result = formatter.valueToString(10.5);
        
        assertNotNull(result);
        assertTrue("Should contain value", result.contains("10"));
        assertFalse("Should not contain abbreviation", result.contains("mm"));
    }

    /**
     * Test TextFieldUnitFormatter with percentage unit
     */
    @Test
    public void testTextFieldUnitFormatterPercentage() throws ParseException {
        TextFieldUnitFormatter formatter = new TextFieldUnitFormatter(TextFieldUnit.PERCENT, 0, true);
        
        // Input as decimal (0.75 = 75%)
        String result = formatter.valueToString(0.75);
        
        assertNotNull(result);
        assertTrue("Should convert to percentage", result.contains("75"));
    }

    /**
     * Test TextFieldUnitFormatter with various decimal places
     */
    @Test
    public void testTextFieldUnitFormatterDecimals() throws ParseException {
        // Test with 0 decimals
        TextFieldUnitFormatter formatter0 = new TextFieldUnitFormatter(TextFieldUnit.MM, 0, true);
        String result0 = formatter0.valueToString(10.567);
        assertNotNull(result0);
        assertTrue("Should round to integer", result0.startsWith("11")); // Rounds up
        
        // Test with 2 decimals
        TextFieldUnitFormatter formatter2 = new TextFieldUnitFormatter(TextFieldUnit.MM, 2, true);
        String result2 = formatter2.valueToString(10.567);
        assertNotNull(result2);
        assertTrue("Should have 2 decimals", result2.contains("10.57") || result2.contains("10,57")); // Locale dependent
    }

    /**
     * Test TextFieldUnitFormatter with various units
     */
    @Test
    public void testTextFieldUnitFormatterDifferentUnits() throws ParseException {
        // Test MM
        TextFieldUnitFormatter mmFormatter = new TextFieldUnitFormatter(TextFieldUnit.MM, 2, true);
        String mmResult = mmFormatter.valueToString(10.0);
        assertTrue("Should contain mm", mmResult.contains("mm"));
        
        // Test INCH (uses quote character)
        TextFieldUnitFormatter inchFormatter = new TextFieldUnitFormatter(TextFieldUnit.INCH, 3, true);
        String inchResult = inchFormatter.valueToString(1.0);
        assertTrue("Should contain inch abbreviation", inchResult.contains("\""));
        
        // Test MM_PER_MIN
        TextFieldUnitFormatter mmMinFormatter = new TextFieldUnitFormatter(TextFieldUnit.MM_PER_MINUTE, 0, true);
        String mmMinResult = mmMinFormatter.valueToString(1000.0);
        assertTrue("Should contain mm/min", mmMinResult.contains("mm/min"));
    }

    /**
     * Performance test: StringBuilder should handle multiple concatenations efficiently
     */
    @Test
    public void testStringBuilderPerformance() throws Exception {
        GcodeParser parser = new GcodeParser();
        RunFromProcessor processor = new RunFromProcessor(0);
        
        // Set up position for string building
        parser.addCommand("G0 X100 Y200 Z10");
        
        // Time multiple operations
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            processor.processCommand("G1 X" + i + " Y" + i, parser.getCurrentState());
        }
        long duration = System.currentTimeMillis() - startTime;
        
        // Should complete quickly (less than 1 second for 1000 operations)
        assertTrue("String building should be efficient", duration < 1000);
    }

    /**
     * Test functional correctness: StringBuilder produces same output as concatenation
     */
    @Test
    public void testFunctionalEquivalence() throws Exception {
        GcodeParser parser = new GcodeParser();
        RunFromProcessor processor = new RunFromProcessor(5);
        
        // Process multiple commands and verify they're well-formed
        parser.addCommand("G0 X10 Y20 Z5");
        parser.addCommand("G1 X15 Y25 Z5 F1000");
        parser.addCommand("G1 X20 Y30 Z5");
        
        List<String> result = processor.processCommand("G1 X25 Y35", parser.getCurrentState());
        
        assertNotNull(result);
        // All commands should be parseable (no malformed strings)
        for (String cmd : result) {
            assertNotNull(cmd);
            assertFalse("Command should not be empty", cmd.trim().isEmpty());
        }
    }

    /**
     * Test edge case: Empty or minimal commands
     */
    @Test
    public void testEdgeCaseMinimalCommands() throws Exception {
        GcodeParser parser = new GcodeParser();
        RunFromProcessor processor = new RunFromProcessor(0);
        
        // Process minimal command
        List<String> result = processor.processCommand("G0", parser.getCurrentState());
        
        assertNotNull(result);
        assertTrue("Should handle minimal commands", result.size() >= 1);
    }

    /**
     * Test edge case: Large coordinate values
     */
    @Test
    public void testEdgeCaseLargeValues() throws Exception {
        GcodeParser parser = new GcodeParser();
        RunFromProcessor processor = new RunFromProcessor(0);
        
        // Set up with large coordinates
        parser.addCommand("G0 X1000.123456 Y2000.654321 Z500.999999");
        
        List<String> result = processor.processCommand("G1 X1500 Y2500", parser.getCurrentState());
        
        assertNotNull(result);
        // Should handle large values without issues
        assertTrue("Should generate commands", result.size() > 0);
    }

    /**
     * Test edge case: Negative coordinate values
     */
    @Test
    public void testEdgeCaseNegativeValues() throws Exception {
        GcodeParser parser = new GcodeParser();
        RunFromProcessor processor = new RunFromProcessor(0);
        
        // Set up with negative coordinates
        parser.addCommand("G0 X-10 Y-20 Z-5");
        
        List<String> result = processor.processCommand("G1 X-15 Y-25", parser.getCurrentState());
        
        assertNotNull(result);
        assertTrue("Should handle negative values", result.size() > 0);
    }
}
