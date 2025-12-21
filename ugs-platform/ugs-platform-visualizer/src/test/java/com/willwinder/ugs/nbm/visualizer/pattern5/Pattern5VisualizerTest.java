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
package com.willwinder.ugs.nbm.visualizer.pattern5;

import com.willwinder.ugs.nbm.visualizer.renderables.GcodeModel;
import com.willwinder.universalgcodesender.model.BackendAPI;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for Pattern 5: GcodeModel Immutable Collections (Visualizer Module)
 * 
 * @author David (Pattern 5 Implementation)
 */
public class Pattern5VisualizerTest {

    @Test
    public void testGcodeModelLineListNullSafety() {
        // Given: A new GcodeModel with no segments
        BackendAPI mockBackend = Mockito.mock(BackendAPI.class);
        GcodeModel model = new GcodeModel("test", mockBackend);
        
        // When: Getting line list
        List<?> lineList = model.getLineList();
        
        // Then: Should return empty list, not null
        assertNotNull("Line list should not be null", lineList);
        assertTrue("Line list should be empty", lineList.isEmpty());
    }

    @Test
    public void testGcodeModelLineListIsImmutable() {
        // Given: A GcodeModel
        BackendAPI mockBackend = Mockito.mock(BackendAPI.class);
        GcodeModel model = new GcodeModel("test", mockBackend);
        List<?> lineList = model.getLineList();
        
        // When: Attempting to modify the returned list
        // Then: Should throw UnsupportedOperationException or be unmodifiable
        try {
            if (lineList instanceof java.util.ArrayList) {
                fail("Should not return mutable ArrayList");
            }
            // Test immutability by attempting to add
            @SuppressWarnings("unchecked")
            List<Object> mutableList = (List<Object>) lineList;
            mutableList.add(new Object());
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected - list is immutable
        }
    }

    @Test
    public void testGcodeModelLineListConsistency() {
        // Given: A GcodeModel
        BackendAPI mockBackend = Mockito.mock(BackendAPI.class);
        GcodeModel model = new GcodeModel("test", mockBackend);
        
        // When: Getting line list multiple times
        List<?> lineList1 = model.getLineList();
        List<?> lineList2 = model.getLineList();
        
        // Then: Both should have same size (view of same underlying list)
        assertEquals("Multiple calls should return consistent views", 
                     lineList1.size(), lineList2.size());
    }
}
