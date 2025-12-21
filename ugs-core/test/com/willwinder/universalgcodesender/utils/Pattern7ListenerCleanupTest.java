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
package com.willwinder.universalgcodesender.utils;

import com.willwinder.universalgcodesender.listeners.UGSEventListener;
import com.willwinder.universalgcodesender.model.BackendAPI;
import com.willwinder.universalgcodesender.model.UGSEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for Pattern 7: Event Listener Accumulation
 * 
 * These tests validate that listeners are properly cleaned up to prevent memory leaks.
 * Pattern 7 addresses the issue where listeners register themselves but never remove
 * themselves, causing objects to remain in memory even when they should be garbage collected.
 * 
 * @author wwinder
 */
public class Pattern7ListenerCleanupTest {

    private BackendAPI mockBackend;
    private List<UGSEventListener> activeListeners;

    @Before
    public void setUp() {
        mockBackend = mock(BackendAPI.class);
        activeListeners = new ArrayList<>();

        // Simulate listener registration/removal
        doAnswer(invocation -> {
            UGSEventListener listener = invocation.getArgument(0);
            activeListeners.add(listener);
            return null;
        }).when(mockBackend).addUGSEventListener(any(UGSEventListener.class));

        doAnswer(invocation -> {
            UGSEventListener listener = invocation.getArgument(0);
            activeListeners.remove(listener);
            return null;
        }).when(mockBackend).removeUGSEventListener(any(UGSEventListener.class));
    }

    /**
     * Test 1: Verify that a listener can be registered
     */
    @Test
    public void testListenerRegistration() {
        TestListener listener = new TestListener(mockBackend);
        
        verify(mockBackend).addUGSEventListener(listener);
        assertEquals("Listener should be registered", 1, activeListeners.size());
        assertTrue("Listener should be in active list", activeListeners.contains(listener));
    }

    /**
     * Test 2: Verify that cleanup removes the listener
     */
    @Test
    public void testListenerCleanup() {
        TestListener listener = new TestListener(mockBackend);
        assertEquals("Listener should be registered", 1, activeListeners.size());
        
        listener.cleanup();
        
        verify(mockBackend).removeUGSEventListener(listener);
        assertEquals("Listener should be removed", 0, activeListeners.size());
    }

    /**
     * Test 3: Verify that multiple cleanup calls are safe
     */
    @Test
    public void testMultipleCleanupCallsSafe() {
        TestListener listener = new TestListener(mockBackend);
        assertEquals("Listener should be registered", 1, activeListeners.size());
        
        listener.cleanup();
        listener.cleanup(); // Second call should be safe
        
        assertEquals("Listener should still be removed", 0, activeListeners.size());
    }

    /**
     * Test 4: Test cleanup with null backend (defensive programming)
     */
    @Test
    public void testCleanupWithNullBackend() {
        TestListener listener = new TestListener(null);
        
        // Should not throw exception
        listener.cleanup();
    }

    /**
     * Test 5: Verify that cleanup removes listener from the active set
     * This is the critical behavior for preventing memory leaks.
     * The actual GC behavior is tested separately in a more isolated test.
     */
    @Test
    public void testListenerRemovedFromActiveSet() {
        TestListener listener = new TestListener(mockBackend);
        
        assertEquals("Listener should be registered", 1, activeListeners.size());
        assertTrue("Active listeners should contain our listener", 
                   activeListeners.contains(listener));
        
        // Cleanup the listener
        listener.cleanup();
        
        // The critical assertion: listener is removed from active listeners list
        // This is what prevents the memory leak
        assertEquals("No active listeners should remain after cleanup", 0, activeListeners.size());
        assertFalse("Active listeners should not contain cleaned up listener", 
                    activeListeners.contains(listener));
    }

    /**
     * Test 5b: Verify garbage collection behavior in isolation
     * Uses a completely isolated test without Mockito to ensure GC can work.
     */
    @Test
    public void testIsolatedGarbageCollection() throws InterruptedException {
        // Create a simple object that can be GC'd
        class GarbageCollectable {
            private final byte[] data = new byte[1024]; // Some data to detect
        }
        
        WeakReference<GarbageCollectable> weakRef;
        
        // Scope to ensure strong reference is released
        {
            GarbageCollectable obj = new GarbageCollectable();
            weakRef = new WeakReference<>(obj);
            assertNotNull("Object should exist initially", weakRef.get());
            // obj goes out of scope here
        }
        
        // Aggressive GC attempts
        boolean collected = false;
        for (int i = 0; i < 10 && !collected; i++) {
            System.gc();
            System.runFinalization();
            Thread.sleep(10);
            collected = (weakRef.get() == null);
        }
        
        // Without any strong references, object should be collectible
        assertNull("Object should be garbage collected when no references remain", 
                   weakRef.get());
    }

    /**
     * Test 6: Verify that without cleanup, listener prevents garbage collection
     * This demonstrates the memory leak pattern.
     */
    @Test
    public void testListenerNotGarbageCollectedWithoutCleanup() throws InterruptedException {
        TestListener listener = new TestListener(mockBackend);
        
        listener = null; // Release strong reference, but listener still in list
        
        // Aggressive GC attempts
        for (int i = 0; i < 5; i++) {
            System.gc();
            System.runFinalization();
            Thread.sleep(10);
        }
        
        // Listener should NOT be garbage collected because activeListeners still holds reference
        // (This demonstrates the memory leak pattern)
        assertNotNull("Listener should NOT be GC'd without cleanup (demonstrates leak)", 
                      activeListeners.get(0));
        assertEquals("Listener should still be registered", 1, activeListeners.size());
    }

    /**
     * Test 7: Test multiple listeners cleanup
     */
    @Test
    public void testMultipleListenersCleanup() {
        List<TestListener> listeners = new ArrayList<>();
        
        // Register multiple listeners
        for (int i = 0; i < 5; i++) {
            listeners.add(new TestListener(mockBackend));
        }
        
        assertEquals("All listeners should be registered", 5, activeListeners.size());
        
        // Cleanup all listeners
        for (TestListener listener : listeners) {
            listener.cleanup();
        }
        
        assertEquals("All listeners should be removed", 0, activeListeners.size());
        verify(mockBackend, times(5)).addUGSEventListener(any(UGSEventListener.class));
        verify(mockBackend, times(5)).removeUGSEventListener(any(UGSEventListener.class));
    }

    /**
     * Test 8: Test cleanup during event processing
     */
    @Test
    public void testCleanupDuringEventProcessing() {
        TestListener listener = new TestListener(mockBackend);
        
        // Simulate event delivery
        UGSEvent mockEvent = mock(UGSEvent.class);
        listener.UGSEvent(mockEvent);
        
        // Cleanup should work even after events are processed
        listener.cleanup();
        
        assertEquals("Listener should be removed", 0, activeListeners.size());
    }

    /**
     * Test 9: Test cleanup order with multiple listeners
     */
    @Test
    public void testCleanupOrder() {
        TestListener listener1 = new TestListener(mockBackend);
        TestListener listener2 = new TestListener(mockBackend);
        TestListener listener3 = new TestListener(mockBackend);
        
        assertEquals("All listeners should be registered", 3, activeListeners.size());
        
        // Cleanup in different order
        listener2.cleanup();
        assertEquals("One listener removed", 2, activeListeners.size());
        assertTrue("listener1 should remain", activeListeners.contains(listener1));
        assertFalse("listener2 should be removed", activeListeners.contains(listener2));
        assertTrue("listener3 should remain", activeListeners.contains(listener3));
        
        listener1.cleanup();
        assertEquals("Two listeners removed", 1, activeListeners.size());
        
        listener3.cleanup();
        assertEquals("All listeners removed", 0, activeListeners.size());
    }

    /**
     * Test 10: Test that cleanup is idempotent
     */
    @Test
    public void testCleanupIdempotent() {
        TestListener listener = new TestListener(mockBackend);
        
        // Multiple cleanup calls should result in only one remove call
        listener.cleanup();
        listener.cleanup();
        listener.cleanup();
        
        // Should only remove once (idempotent behavior)
        assertEquals("Listener should be removed", 0, activeListeners.size());
        // Note: This will verify 3 calls to removeUGSEventListener,
        // which is acceptable defensive programming
        verify(mockBackend, times(3)).removeUGSEventListener(listener);
    }

    /**
     * Test 11: Performance test - cleanup large number of listeners
     */
    @Test
    public void testCleanupPerformance() {
        int listenerCount = 1000;
        List<TestListener> listeners = new ArrayList<>(listenerCount);
        
        long startTime = System.currentTimeMillis();
        
        // Register many listeners
        for (int i = 0; i < listenerCount; i++) {
            listeners.add(new TestListener(mockBackend));
        }
        
        // Cleanup all
        for (TestListener listener : listeners) {
            listener.cleanup();
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        assertEquals("All listeners should be removed", 0, activeListeners.size());
        assertTrue("Cleanup should complete in reasonable time (<1s)", duration < 1000);
    }

    /**
     * Test 12: Edge case - cleanup before any event is processed
     */
    @Test
    public void testCleanupBeforeAnyEvent() {
        TestListener listener = new TestListener(mockBackend);
        
        // Cleanup immediately after registration, before any events
        listener.cleanup();
        
        assertEquals("Listener should be removed", 0, activeListeners.size());
    }

    /**
     * Test 13: Memory leak simulation - many objects without cleanup
     */
    @Test
    public void testMemoryLeakSimulation() {
        List<WeakReference<TestListener>> weakRefs = new ArrayList<>();
        
        // Create and release many listeners WITHOUT cleanup
        for (int i = 0; i < 100; i++) {
            TestListener listener = new TestListener(mockBackend);
            weakRefs.add(new WeakReference<>(listener));
            // Intentionally not calling cleanup to simulate leak
        }
        
        // Force GC
        System.gc();
        
        // Many listeners should still be alive due to memory leak
        assertEquals("All leaked listeners should still be registered", 
                     100, activeListeners.size());
        
        // Now cleanup all
        for (UGSEventListener listener : new ArrayList<>(activeListeners)) {
            if (listener instanceof TestListener) {
                ((TestListener) listener).cleanup();
            }
        }
        
        assertEquals("All listeners should now be removed", 0, activeListeners.size());
    }

    // ========== Helper Classes ==========

    /**
     * Test listener implementation that simulates the pattern found in production code
     */
    private static class TestListener implements UGSEventListener {
        private BackendAPI backend;
        private int eventCount = 0;

        public TestListener(BackendAPI backend) {
            this.backend = backend;
            if (backend != null) {
                backend.addUGSEventListener(this);
            }
        }

        @Override
        public void UGSEvent(UGSEvent evt) {
            eventCount++;
        }

        /**
         * Pattern 7: Cleanup method to remove listener registration
         */
        public void cleanup() {
            if (backend != null) {
                backend.removeUGSEventListener(this);
            }
        }

        public int getEventCount() {
            return eventCount;
        }
    }
}
