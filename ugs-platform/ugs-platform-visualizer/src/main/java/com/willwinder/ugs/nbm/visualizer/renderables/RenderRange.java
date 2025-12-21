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

/**
 * Pattern 2.1: Represents a range of line segments to render.
 * This enables chunked/partial rendering of large G-code files.
 * 
 * @author wwinder
 */
public class RenderRange {
    private final int start;
    private final int end;
    
    /**
     * Creates a render range for all segments.
     */
    public static final RenderRange ALL = new RenderRange(0, Integer.MAX_VALUE);
    
    /**
     * Creates a render range from start (inclusive) to end (exclusive).
     * 
     * @param start First segment index to render (inclusive)
     * @param end Last segment index to render (exclusive)
     */
    public RenderRange(int start, int end) {
        if (start < 0) {
            throw new IllegalArgumentException("Start must be non-negative");
        }
        if (end < start) {
            throw new IllegalArgumentException("End must be >= start");
        }
        this.start = start;
        this.end = end;
    }
    
    /**
     * Creates a render range for a single chunk.
     * 
     * @param chunkIndex Zero-based chunk index
     * @param chunkSize Number of segments per chunk
     * @return RenderRange for the specified chunk
     */
    public static RenderRange forChunk(int chunkIndex, int chunkSize) {
        if (chunkIndex < 0) {
            throw new IllegalArgumentException("Chunk index must be non-negative");
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive");
        }
        int start = chunkIndex * chunkSize;
        int end = start + chunkSize;
        return new RenderRange(start, end);
    }
    
    /**
     * Gets the start index (inclusive).
     */
    public int getStart() {
        return start;
    }
    
    /**
     * Gets the end index (exclusive).
     */
    public int getEnd() {
        return end;
    }
    
    /**
     * Gets the number of segments in this range.
     */
    public int getCount() {
        return end - start;
    }
    
    /**
     * Checks if this range is effectively "all segments".
     */
    public boolean isAll() {
        return start == 0 && end == Integer.MAX_VALUE;
    }
    
    /**
     * Clamps this range to the actual available size.
     * 
     * @param actualSize The actual number of segments available
     * @return A new RenderRange clamped to actualSize
     */
    public RenderRange clamp(int actualSize) {
        if (actualSize < 0) {
            throw new IllegalArgumentException("Actual size must be non-negative");
        }
        int clampedStart = Math.min(start, actualSize);
        int clampedEnd = Math.min(end, actualSize);
        return new RenderRange(clampedStart, clampedEnd);
    }
    
    @Override
    public String toString() {
        if (isAll()) {
            return "RenderRange[ALL]";
        }
        return String.format("RenderRange[%d-%d]", start, end);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RenderRange)) return false;
        RenderRange other = (RenderRange) obj;
        return start == other.start && end == other.end;
    }
    
    @Override
    public int hashCode() {
        return 31 * start + end;
    }
}
