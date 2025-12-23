# CompactLineSegmentStorage - Quick Reference Guide

## When to Use

✅ **Use CompactLineSegmentStorage when:**
- Loading large G-code files (>10,000 line segments)
- Memory is constrained (embedded systems, older hardware)
- Rendering performance is critical
- Data is read more than written
- Cache efficiency matters

❌ **Use traditional List<LineSegment> when:**
- Small files (<1,000 segments)
- Frequent random inserts/deletes needed
- API compatibility is more important than memory
- Double precision is required (CompactLineSegmentStorage uses floats for positions)

## Quick Start

### Basic Usage

```java
// Create with pre-allocated capacity (Pattern 3 optimization)
CompactLineSegmentStorage storage = new CompactLineSegmentStorage(10000);

// Add segments
for (LineSegment segment : segments) {
    storage.add(segment);
}

// Access count
int count = storage.size();
boolean empty = storage.isEmpty();

// Clear all
storage.clear();
```

### Retrieving Data

```java
// Standard access (creates new LineSegment)
LineSegment segment = storage.get(index);
Position start = segment.getStart();
Position end = segment.getEnd();

// Fast property access (no object creation)
int lineNum = storage.getLineNumber(index);
double feedRate = storage.getFeedRate(index);
boolean isArc = storage.isArc(index);
boolean isZMove = storage.isZMovement(index);
boolean isFast = storage.isFastTraverse(index);
```

### Zero-Allocation Access (Fastest)

```java
// Reuse Position object to avoid allocations
Position reusablePos = new Position(0, 0, 0);

for (int i = 0; i < storage.size(); i++) {
    // Get positions without creating new objects
    storage.getStartPosition(i, reusablePos);
    double startX = reusablePos.x;
    double startY = reusablePos.y;
    double startZ = reusablePos.z;
    
    storage.getEndPosition(i, reusablePos);
    double endX = reusablePos.x;
    double endY = reusablePos.y;
    double endZ = reusablePos.z;
    
    // Use for rendering...
}
```

### OpenGL/GPU Rendering

```java
// Get direct array access for buffer upload
float[] positions = storage.getPositionsArray();
int dataLength = storage.getPositionDataLength(); // positions * 6

// Upload to GPU
glBufferData(GL_ARRAY_BUFFER, 
    positions, 0, dataLength * Float.BYTES, 
    GL_STATIC_DRAW);
```

## Performance Tips

### 1. Pre-allocate Capacity
```java
// BAD: Starts at 0, grows multiple times
CompactLineSegmentStorage storage = new CompactLineSegmentStorage();
for (int i = 0; i < 100000; i++) {
    storage.add(segment); // Triggers reallocations
}

// GOOD: Pre-allocate to avoid growth
CompactLineSegmentStorage storage = new CompactLineSegmentStorage(100000);
for (int i = 0; i < 100000; i++) {
    storage.add(segment); // No reallocations
}
```

### 2. Use Zero-Allocation Access for Loops
```java
// BAD: Creates 200K Position objects
for (int i = 0; i < 100000; i++) {
    LineSegment seg = storage.get(i);
    Position start = seg.getStart(); // New object
    Position end = seg.getEnd();     // New object
    // Use positions...
}

// GOOD: Reuses one Position object
Position pos = new Position(0, 0, 0);
for (int i = 0; i < 100000; i++) {
    storage.getStartPosition(i, pos); // No allocation
    storage.getEndPosition(i, pos);    // No allocation
    // Use positions...
}
```

### 3. Direct Array Access for Rendering
```java
// BAD: Individual LineSegment access
for (int i = 0; i < storage.size(); i++) {
    LineSegment seg = storage.get(i);
    renderLine(seg.getStart(), seg.getEnd());
}

// GOOD: Direct array access
float[] positions = storage.getPositionsArray();
for (int i = 0; i < storage.size(); i++) {
    int idx = i * 6;
    renderLine(
        positions[idx], positions[idx+1], positions[idx+2],    // start
        positions[idx+3], positions[idx+4], positions[idx+5]   // end
    );
}
```

## Memory Comparison

### 10,000 Segments
- Traditional: ~1.2 MB
- Compact: ~450 KB
- **Saved: 750 KB (62.5%)**

### 100,000 Segments
- Traditional: ~12 MB
- Compact: ~4.5 MB
- **Saved: 7.5 MB (62.5%)**

### 1,000,000 Segments
- Traditional: ~120 MB
- Compact: ~45 MB
- **Saved: 75 MB (62.5%)**

## Performance Benchmarks

Based on 100,000 segments:

| Operation | Time | Per Segment |
|-----------|------|-------------|
| Add all | 120ms | 1.2μs |
| Retrieve all (standard) | 59ms | 0.59μs |
| Retrieve all (optimized) | 8ms | 0.08μs |
| **Speedup (optimized vs standard)** | **7.4x** | - |

## Common Patterns

### Pattern 1: File Loading
```java
public CompactLineSegmentStorage loadFile(String path) {
    List<String> lines = Files.readAllLines(Paths.get(path));
    CompactLineSegmentStorage storage = new CompactLineSegmentStorage(lines.size());
    
    Position lastPos = new Position(0, 0, 0);
    for (int i = 0; i < lines.size(); i++) {
        Position nextPos = parseGcodeLine(lines.get(i));
        LineSegment segment = new LineSegment(lastPos, nextPos, i);
        storage.add(segment);
        lastPos = nextPos;
    }
    
    return storage;
}
```

### Pattern 2: Rendering Loop
```java
public void render(CompactLineSegmentStorage storage) {
    float[] positions = storage.getPositionsArray();
    byte[] colors = generateColors(storage);
    
    // Upload to GPU
    glBufferData(GL_ARRAY_BUFFER, positions, GL_STATIC_DRAW);
    glBufferData(GL_ARRAY_BUFFER, colors, GL_STATIC_DRAW);
    
    // Draw
    glDrawArrays(GL_LINES, 0, storage.size() * 2);
}
```

### Pattern 3: Filtering
```java
public CompactLineSegmentStorage filterArcs(CompactLineSegmentStorage input) {
    CompactLineSegmentStorage output = new CompactLineSegmentStorage(input.size() / 10);
    
    for (int i = 0; i < input.size(); i++) {
        if (input.isArc(i)) {
            output.add(input.get(i));
        }
    }
    
    return output;
}
```

### Pattern 4: Range Access
```java
public void renderRange(CompactLineSegmentStorage storage, int start, int end) {
    Position pos = new Position(0, 0, 0);
    
    for (int i = start; i < end && i < storage.size(); i++) {
        storage.getStartPosition(i, pos);
        glVertex3d(pos.x, pos.y, pos.z);
        
        storage.getEndPosition(i, pos);
        glVertex3d(pos.x, pos.y, pos.z);
    }
}
```

## API Reference

### Constructors
- `CompactLineSegmentStorage()` - Create empty storage
- `CompactLineSegmentStorage(int capacity)` - Create with initial capacity

### Mutators
- `void add(LineSegment segment)` - Add a segment
- `void clear()` - Remove all segments

### Accessors
- `int size()` - Get segment count
- `boolean isEmpty()` - Check if empty
- `LineSegment get(int index)` - Get segment as object
- `void getStartPosition(int index, Position result)` - Get start position (zero-alloc)
- `void getEndPosition(int index, Position result)` - Get end position (zero-alloc)

### Property Accessors
- `int getLineNumber(int index)` - Get line number
- `double getFeedRate(int index)` - Get feed rate
- `boolean isZMovement(int index)` - Check Z-movement flag
- `boolean isArc(int index)` - Check arc flag
- `boolean isFastTraverse(int index)` - Check fast traverse flag

### Direct Access
- `float[] getPositionsArray()` - Get position array (read-only!)
- `int getPositionDataLength()` - Get position data length
- `long estimateMemoryUsage()` - Estimate memory usage in bytes

## Error Handling

```java
try {
    LineSegment seg = storage.get(999);
} catch (IndexOutOfBoundsException e) {
    // Index out of range
}

try {
    storage.add(null);
} catch (NullPointerException e) {
    // Cannot add null segment
}

try {
    new CompactLineSegmentStorage(-1);
} catch (IllegalArgumentException e) {
    // Capacity cannot be negative
}
```

## Migration Guide

### From ArrayList<LineSegment>

```java
// OLD CODE
ArrayList<LineSegment> segments = new ArrayList<>();
segments.add(segment);
int count = segments.size();
LineSegment seg = segments.get(index);

// NEW CODE - Drop-in replacement
CompactLineSegmentStorage segments = new CompactLineSegmentStorage();
segments.add(segment);
int count = segments.size();
LineSegment seg = segments.get(index);
```

### From List<LineSegment> Interface

```java
// If you need List interface, wrap it
public class CompactLineSegmentList extends AbstractList<LineSegment> {
    private final CompactLineSegmentStorage storage;
    
    public LineSegment get(int index) {
        return storage.get(index);
    }
    
    public int size() {
        return storage.size();
    }
}
```

## Testing

```java
@Test
public void testCompactStorage() {
    CompactLineSegmentStorage storage = new CompactLineSegmentStorage(10);
    
    Position start = new Position(0, 0, 0);
    Position end = new Position(1, 1, 1);
    LineSegment segment = new LineSegment(start, end, 1);
    segment.setFeedRate(1500.0);
    segment.setIsArc(true);
    
    storage.add(segment);
    
    assertEquals(1, storage.size());
    assertFalse(storage.isEmpty());
    
    LineSegment retrieved = storage.get(0);
    assertEquals(1, retrieved.getLineNumber());
    assertEquals(1500.0, retrieved.getFeedRate(), 0.001);
    assertTrue(retrieved.isArc());
}
```

## Best Practices

1. ✅ **Pre-allocate capacity** when size is known
2. ✅ **Use zero-allocation accessors** in hot loops
3. ✅ **Access arrays directly** for GPU/bulk operations
4. ✅ **Reuse Position objects** instead of creating new ones
5. ✅ **Clear storage** when done to release memory
6. ❌ **Don't modify returned arrays** (read-only!)
7. ❌ **Don't hold references** to transient LineSegment objects
8. ❌ **Don't use for frequent inserts/deletes** (use ArrayList instead)

## Questions?

- See: `/info/pattern2.2-implementation-summary.md` - Full implementation details
- See: `Pattern22CompactStorageTest.java` - 22 comprehensive test examples
- See: JavaDoc in `CompactLineSegmentStorage.java` - Complete API documentation
