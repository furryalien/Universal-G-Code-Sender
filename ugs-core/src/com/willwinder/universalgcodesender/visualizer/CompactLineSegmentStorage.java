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

/**
 * Pattern 2.2: Compact storage for line segment data using primitive arrays.
 * 
 * <p>This class reduces memory overhead by storing line segment data in packed
 * primitive arrays instead of individual LineSegment objects. Each LineSegment
 * object has ~64 bytes of overhead (object header, references, padding), while
 * this compact storage uses only the raw data bytes.
 * 
 * <p><b>Memory Savings:</b>
 * <ul>
 * <li>LineSegment object: ~120 bytes (64 byte overhead + 2 Position objects + flags)</li>
 * <li>Compact storage: ~58 bytes per segment (6 floats + 2 doubles + 5 booleans + 1 int)</li>
 * <li>Savings: ~50-60% reduction in memory usage</li>
 * <li>For 100,000 segments: ~6-7MB saved</li>
 * </ul>
 * 
 * <p><b>Data Layout:</b>
 * <pre>
 * positions: [x1,y1,z1, x2,y2,z2, x3,y3,z3, ...] (6 floats per segment)
 * feedRates: [feedRate1, feedRate2, feedRate3, ...] (1 double per segment)
 * spindleSpeeds: [speed1, speed2, speed3, ...] (1 double per segment)
 * lineNumbers: [line1, line2, line3, ...] (1 int per segment)
 * flags: packed bits [isZMovement|isArc|isFastTraverse|isRotation|bit4|bit5|bit6|bit7] (1 byte per segment)
 * </pre>
 * 
 * <p><b>Thread Safety:</b> This class is NOT thread-safe. External synchronization
 * required if accessed from multiple threads.
 * 
 * @author wwinder
 * @since 2.0-SNAPSHOT
 */
public class CompactLineSegmentStorage {
    // Bit flags for segment properties
    private static final int FLAG_Z_MOVEMENT = 0x01;      // bit 0
    private static final int FLAG_ARC = 0x02;             // bit 1
    private static final int FLAG_FAST_TRAVERSE = 0x04;   // bit 2
    private static final int FLAG_ROTATION = 0x08;        // bit 3
    
    // Floats per segment: 3 for start position + 3 for end position
    private static final int FLOATS_PER_SEGMENT = 6;
    
    private float[] positions;        // Packed: [x1,y1,z1, x2,y2,z2, ...]
    private double[] feedRates;       // One per segment
    private double[] spindleSpeeds;   // One per segment
    private int[] lineNumbers;        // One per segment
    private byte[] flags;             // Packed boolean flags, one byte per segment
    
    private int segmentCount;
    private int capacity;
    
    /**
     * Creates a new compact storage with the specified initial capacity.
     * 
     * @param initialCapacity initial number of segments to allocate space for
     * @throws IllegalArgumentException if initialCapacity is negative
     */
    public CompactLineSegmentStorage(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Capacity cannot be negative: " + initialCapacity);
        }
        
        this.capacity = initialCapacity;
        this.segmentCount = 0;
        
        if (initialCapacity > 0) {
            this.positions = new float[initialCapacity * FLOATS_PER_SEGMENT];
            this.feedRates = new double[initialCapacity];
            this.spindleSpeeds = new double[initialCapacity];
            this.lineNumbers = new int[initialCapacity];
            this.flags = new byte[initialCapacity];
        }
    }
    
    /**
     * Creates an empty compact storage with default capacity of 0.
     */
    public CompactLineSegmentStorage() {
        this(0);
    }
    
    /**
     * Adds a line segment to the storage.
     * 
     * @param segment the LineSegment to add
     * @throws NullPointerException if segment is null
     */
    public void add(LineSegment segment) {
        if (segment == null) {
            throw new NullPointerException("Cannot add null segment");
        }
        
        ensureCapacity(segmentCount + 1);
        
        int posIndex = segmentCount * FLOATS_PER_SEGMENT;
        Position start = segment.getStart();
        Position end = segment.getEnd();
        
        // Store positions
        positions[posIndex] = (float) start.x;
        positions[posIndex + 1] = (float) start.y;
        positions[posIndex + 2] = (float) start.z;
        positions[posIndex + 3] = (float) end.x;
        positions[posIndex + 4] = (float) end.y;
        positions[posIndex + 5] = (float) end.z;
        
        // Store properties
        feedRates[segmentCount] = segment.getFeedRate();
        spindleSpeeds[segmentCount] = segment.getSpindleSpeed();
        lineNumbers[segmentCount] = segment.getLineNumber();
        
        // Pack flags
        byte segmentFlags = 0;
        if (segment.isZMovement()) segmentFlags |= FLAG_Z_MOVEMENT;
        if (segment.isArc()) segmentFlags |= FLAG_ARC;
        if (segment.isFastTraverse()) segmentFlags |= FLAG_FAST_TRAVERSE;
        if (segment.isRotation()) segmentFlags |= FLAG_ROTATION;
        flags[segmentCount] = segmentFlags;
        
        segmentCount++;
    }
    
    /**
     * Retrieves a line segment at the specified index.
     * 
     * @param index the index of the segment to retrieve
     * @return a new LineSegment object with the stored data
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public LineSegment get(int index) {
        if (index < 0 || index >= segmentCount) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + segmentCount);
        }
        
        int posIndex = index * FLOATS_PER_SEGMENT;
        
        Position start = new Position(
            positions[posIndex],
            positions[posIndex + 1],
            positions[posIndex + 2]
        );
        
        Position end = new Position(
            positions[posIndex + 3],
            positions[posIndex + 4],
            positions[posIndex + 5]
        );
        
        LineSegment segment = new LineSegment(start, end, lineNumbers[index]);
        segment.setFeedRate(feedRates[index]);
        segment.setSpindleSpeed(spindleSpeeds[index]);
        
        byte segmentFlags = flags[index];
        segment.setIsZMovement((segmentFlags & FLAG_Z_MOVEMENT) != 0);
        segment.setIsArc((segmentFlags & FLAG_ARC) != 0);
        segment.setIsFastTraverse((segmentFlags & FLAG_FAST_TRAVERSE) != 0);
        segment.setIsRotation((segmentFlags & FLAG_ROTATION) != 0);
        
        return segment;
    }
    
    /**
     * Returns the number of segments currently stored.
     * 
     * @return the segment count
     */
    public int size() {
        return segmentCount;
    }
    
    /**
     * Returns true if this storage contains no segments.
     * 
     * @return true if empty
     */
    public boolean isEmpty() {
        return segmentCount == 0;
    }
    
    /**
     * Removes all segments from this storage.
     * Capacity is preserved.
     */
    public void clear() {
        segmentCount = 0;
    }
    
    /**
     * Gets the start position of a segment without creating a new object.
     * 
     * @param index the segment index
     * @param result the Position object to fill (will be modified)
     * @throws IndexOutOfBoundsException if index is out of range
     * @throws NullPointerException if result is null
     */
    public void getStartPosition(int index, Position result) {
        if (index < 0 || index >= segmentCount) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + segmentCount);
        }
        if (result == null) {
            throw new NullPointerException("Result position cannot be null");
        }
        
        int posIndex = index * FLOATS_PER_SEGMENT;
        result.x = positions[posIndex];
        result.y = positions[posIndex + 1];
        result.z = positions[posIndex + 2];
    }
    
    /**
     * Gets the end position of a segment without creating a new object.
     * 
     * @param index the segment index
     * @param result the Position object to fill (will be modified)
     * @throws IndexOutOfBoundsException if index is out of range
     * @throws NullPointerException if result is null
     */
    public void getEndPosition(int index, Position result) {
        if (index < 0 || index >= segmentCount) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + segmentCount);
        }
        if (result == null) {
            throw new NullPointerException("Result position cannot be null");
        }
        
        int posIndex = index * FLOATS_PER_SEGMENT;
        result.x = positions[posIndex + 3];
        result.y = positions[posIndex + 4];
        result.z = positions[posIndex + 5];
    }
    
    /**
     * Gets the line number for a segment.
     * 
     * @param index the segment index
     * @return the line number
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public int getLineNumber(int index) {
        if (index < 0 || index >= segmentCount) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + segmentCount);
        }
        return lineNumbers[index];
    }
    
    /**
     * Gets the feed rate for a segment.
     * 
     * @param index the segment index
     * @return the feed rate
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public double getFeedRate(int index) {
        if (index < 0 || index >= segmentCount) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + segmentCount);
        }
        return feedRates[index];
    }
    
    /**
     * Checks if a segment represents Z-axis movement.
     * 
     * @param index the segment index
     * @return true if this is a Z movement
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public boolean isZMovement(int index) {
        if (index < 0 || index >= segmentCount) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + segmentCount);
        }
        return (flags[index] & FLAG_Z_MOVEMENT) != 0;
    }
    
    /**
     * Checks if a segment is an arc.
     * 
     * @param index the segment index
     * @return true if this is an arc
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public boolean isArc(int index) {
        if (index < 0 || index >= segmentCount) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + segmentCount);
        }
        return (flags[index] & FLAG_ARC) != 0;
    }
    
    /**
     * Checks if a segment is a fast traverse (G0).
     * 
     * @param index the segment index
     * @return true if this is a fast traverse
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public boolean isFastTraverse(int index) {
        if (index < 0 || index >= segmentCount) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + segmentCount);
        }
        return (flags[index] & FLAG_FAST_TRAVERSE) != 0;
    }
    
    /**
     * Returns a direct reference to the position array for efficient rendering.
     * <p><b>WARNING:</b> This array should only be read, never modified.
     * The array contains packed data: [x1,y1,z1,x2,y2,z2,...]
     * 
     * @return the positions array, or null if empty
     */
    public float[] getPositionsArray() {
        return positions;
    }
    
    /**
     * Returns the actual length of valid position data in the array.
     * This is segmentCount * 6 (6 floats per segment).
     * 
     * @return the number of valid floats in the positions array
     */
    public int getPositionDataLength() {
        return segmentCount * FLOATS_PER_SEGMENT;
    }
    
    /**
     * Estimates the memory footprint of this storage in bytes.
     * 
     * @return estimated memory usage in bytes
     */
    public long estimateMemoryUsage() {
        long usage = 0;
        
        // Object overhead (estimate)
        usage += 48; // Base object
        
        // Array overhead + data
        if (positions != null) {
            usage += 24 + (positions.length * 4L); // array overhead + float data
        }
        if (feedRates != null) {
            usage += 24 + (feedRates.length * 8L); // array overhead + double data
        }
        if (spindleSpeeds != null) {
            usage += 24 + (spindleSpeeds.length * 8L); // array overhead + double data
        }
        if (lineNumbers != null) {
            usage += 24 + (lineNumbers.length * 4L); // array overhead + int data
        }
        if (flags != null) {
            usage += 24 + flags.length; // array overhead + byte data
        }
        
        return usage;
    }
    
    /**
     * Ensures that the storage can hold at least the specified number of segments.
     * 
     * @param minCapacity the minimum capacity required
     */
    private void ensureCapacity(int minCapacity) {
        if (minCapacity <= capacity) {
            return;
        }
        
        // Grow by 50% similar to ArrayList
        int newCapacity = Math.max(minCapacity, capacity + (capacity >> 1));
        
        float[] newPositions = new float[newCapacity * FLOATS_PER_SEGMENT];
        double[] newFeedRates = new double[newCapacity];
        double[] newSpindleSpeeds = new double[newCapacity];
        int[] newLineNumbers = new int[newCapacity];
        byte[] newFlags = new byte[newCapacity];
        
        if (segmentCount > 0) {
            System.arraycopy(positions, 0, newPositions, 0, segmentCount * FLOATS_PER_SEGMENT);
            System.arraycopy(feedRates, 0, newFeedRates, 0, segmentCount);
            System.arraycopy(spindleSpeeds, 0, newSpindleSpeeds, 0, segmentCount);
            System.arraycopy(lineNumbers, 0, newLineNumbers, 0, segmentCount);
            System.arraycopy(flags, 0, newFlags, 0, segmentCount);
        }
        
        positions = newPositions;
        feedRates = newFeedRates;
        spindleSpeeds = newSpindleSpeeds;
        lineNumbers = newLineNumbers;
        flags = newFlags;
        capacity = newCapacity;
    }
}
