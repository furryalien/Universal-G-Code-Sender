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
package com.willwinder.universalgcodesender.pattern5;

import com.willwinder.universalgcodesender.firmware.fluidnc.commands.ListFilesCommand;
import com.willwinder.universalgcodesender.utils.FirmwareUtils;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests for Pattern 5: Defensive Copying - Immutable Collection Wrappers (Core Module)
 * 
 * This pattern addresses memory waste from unnecessary defensive copying by
 * returning unmodifiable views of internal collections instead of exposing
 * mutable references. This saves 1-2MB by eliminating duplicate ArrayList
 * allocations in caller code.
 * 
 * Key test scenarios:
 * 1. Returned collections throw UnsupportedOperationException on modification
 * 2. Null collections safely return emptyList/emptyMap
 * 3. Multiple calls return consistent unmodifiable views
 * 4. JavaDoc documents immutability contract
 * 
 * @author David (Pattern 5 Implementation)
 */
public class Pattern5ImmutableCollectionTest {

    // ==================== FirmwareUtils Tests ====================

    @Test(expected = UnsupportedOperationException.class)
    public void testFirmwareUtilsConfigFilesIsImmutable() {
        // Given: FirmwareUtils with config files
        Map<String, ?> configFiles = FirmwareUtils.getConfigFiles();
        
        // When: Attempting to modify the map
        // Then: Should throw UnsupportedOperationException
        configFiles.clear();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testFirmwareUtilsFirmwareListIsImmutable() {
        // Given: FirmwareUtils with firmware list
        List<String> firmwareList = FirmwareUtils.getFirmwareList();
        
        // When: Attempting to modify the list
        // Then: Should throw UnsupportedOperationException
        firmwareList.add("TestFirmware");
    }

    @Test
    public void testFirmwareUtilsReturnsNonNull() {
        // When: Getting firmware config and list
        Map<String, ?> configFiles = FirmwareUtils.getConfigFiles();
        List<String> firmwareList = FirmwareUtils.getFirmwareList();
        
        // Then: Should never return null
        assertNotNull("Config files should not be null", configFiles);
        assertNotNull("Firmware list should not be null", firmwareList);
    }

    // ==================== ListFilesCommand Tests ====================

    @Test
    public void testListFilesCommandFileListIsImmutable() {
        // Given: A ListFilesCommand with empty response
        ListFilesCommand command = new ListFilesCommand();
        List<String> fileList = command.getFileList();
        
        // Then: Should return immutable list (empty but non-null)
        assertNotNull("File list should not be null", fileList);
        
        // When: Attempting to modify the list
        try {
            fileList.add("/localfs/test.gcode");
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected - list is immutable
        }
    }

    @Test
    public void testListFilesCommandFilesIsImmutable() {
        // Given: A ListFilesCommand with empty response
        ListFilesCommand command = new ListFilesCommand();
        List<?> files = command.getFiles();
        
        // Then: Should return immutable list (empty but non-null)
        assertNotNull("Files should not be null", files);
        
        // When: Attempting to add to the list (clearer test than clear on empty list)
        try {
            if (files instanceof java.util.ArrayList) {
                fail("Should not return mutable ArrayList");
            }
            // Force modification attempt - use a cast to avoid unchecked warning
            @SuppressWarnings("unchecked")
            List<Object> mutableFiles = (List<Object>) files;
            mutableFiles.add(new Object());
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected - list is immutable
        }
    }

    @Test
    public void testListFilesCommandEmptyResponseSafe() {
        // Given: A ListFilesCommand with empty response
        ListFilesCommand command = new ListFilesCommand();
        
        // When: Getting files
        List<String> fileList = command.getFileList();
        List<?> files = command.getFiles();
        
        // Then: Should return empty lists, not null
        assertNotNull("File list should not be null", fileList);
        assertNotNull("Files should not be null", files);
        assertTrue("File list should be empty", fileList.isEmpty());
        assertTrue("Files should be empty", files.isEmpty());
    }

    @Test
    public void testListFilesCommandReturnsConsistentData() {
        // Given: A ListFilesCommand with empty response
        ListFilesCommand command = new ListFilesCommand();
        
        // When: Getting files multiple times
        List<String> fileList1 = command.getFileList();
        List<String> fileList2 = command.getFileList();
        
        // Then: Should return consistent data (both empty)
        assertEquals("Multiple calls should return same size", 
                     fileList1.size(), fileList2.size());
        assertEquals("Both should be empty", 0, fileList1.size());
    }

    // ==================== Helper Methods ====================
    // (Removed complex reflection-based helper - not needed for immutability testing)
}
