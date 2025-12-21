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
package com.willwinder.ugs.nbp.designer.pattern5;

import com.willwinder.ugs.nbp.designer.io.c2d.model.C2dCircleObject;
import com.willwinder.ugs.nbp.designer.io.c2d.model.C2dCurveObject;
import com.willwinder.ugs.nbp.designer.io.c2d.model.C2dFile;
import com.willwinder.ugs.nbp.designer.io.c2d.model.C2dRectangleObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for Pattern 5: Designer C2d Model Immutable Collections (Designer Module)
 * 
 * @author David (Pattern 5 Implementation)
 */
public class Pattern5DesignerTest {

    @Test(expected = UnsupportedOperationException.class)
    public void testC2dFileCircleObjectsIsImmutable() {
        // Given: A C2dFile with circle objects
        C2dFile file = createC2dFileWithCircles();
        List<C2dCircleObject> circles = file.getCircleObjects();
        
        // When: Attempting to modify the list
        // Then: Should throw UnsupportedOperationException
        circles.add(new C2dCircleObject());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testC2dFileRectangleObjectsIsImmutable() {
        // Given: A C2dFile with rectangle objects
        C2dFile file = createC2dFileWithRectangles();
        List<C2dRectangleObject> rectangles = file.getRectangleObjects();
        
        // When: Attempting to modify the list
        // Then: Should throw UnsupportedOperationException
        rectangles.add(new C2dRectangleObject());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testC2dFileCurveObjectsIsImmutable() {
        // Given: A C2dFile with curve objects
        C2dFile file = createC2dFileWithCurves();
        List<C2dCurveObject> curves = file.getCurveObjects();
        
        // When: Attempting to modify the list
        // Then: Should throw UnsupportedOperationException
        curves.add(new C2dCurveObject());
    }

    @Test
    public void testC2dFileEmptyCollectionsAreSafe() {
        // Given: An empty C2dFile
        C2dFile file = new C2dFile();
        
        // When: Getting collections
        List<C2dCircleObject> circles = file.getCircleObjects();
        List<C2dRectangleObject> rectangles = file.getRectangleObjects();
        List<C2dCurveObject> curves = file.getCurveObjects();
        
        // Then: Should return empty lists, not null
        assertNotNull("Circles should not be null", circles);
        assertNotNull("Rectangles should not be null", rectangles);
        assertNotNull("Curves should not be null", curves);
        assertTrue("Circles should be empty", circles.isEmpty());
        assertTrue("Rectangles should be empty", rectangles.isEmpty());
        assertTrue("Curves should be empty", curves.isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testC2dCurveObjectControlPoints1IsImmutable() {
        // Given: A C2dCurveObject with control points
        C2dCurveObject curve = createC2dCurveWithControlPoints();
        List<Double[]> cp1 = curve.getControlPoints1();
        
        // When: Attempting to modify the list
        // Then: Should throw UnsupportedOperationException
        cp1.add(new Double[]{0.0, 0.0});
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testC2dCurveObjectControlPoints2IsImmutable() {
        // Given: A C2dCurveObject with control points
        C2dCurveObject curve = createC2dCurveWithControlPoints();
        List<Double[]> cp2 = curve.getControlPoints2();
        
        // When: Attempting to modify the list
        // Then: Should throw UnsupportedOperationException
        cp2.add(new Double[]{0.0, 0.0});
    }

    @Test
    public void testC2dCurveObjectEmptyControlPointsAreSafe() {
        // Given: A C2dCurveObject without control points
        C2dCurveObject curve = new C2dCurveObject();
        
        // When: Getting control points
        List<Double[]> cp1 = curve.getControlPoints1();
        List<Double[]> cp2 = curve.getControlPoints2();
        
        // Then: Should return empty lists, not null
        assertNotNull("Control points 1 should not be null", cp1);
        assertNotNull("Control points 2 should not be null", cp2);
        assertTrue("Control points 1 should be empty", cp1.isEmpty());
        assertTrue("Control points 2 should be empty", cp2.isEmpty());
    }

    // ==================== Helper Methods ====================

    private C2dFile createC2dFileWithCircles() {
        C2dFile file = new C2dFile();
        try {
            java.lang.reflect.Field field = C2dFile.class.getDeclaredField("circleObjects");
            field.setAccessible(true);
            List<C2dCircleObject> circles = new ArrayList<>();
            circles.add(new C2dCircleObject());
            field.set(file, circles);
        } catch (Exception e) {
            fail("Failed to setup test: " + e.getMessage());
        }
        return file;
    }

    private C2dFile createC2dFileWithRectangles() {
        C2dFile file = new C2dFile();
        try {
            java.lang.reflect.Field field = C2dFile.class.getDeclaredField("rectangleObjects");
            field.setAccessible(true);
            List<C2dRectangleObject> rectangles = new ArrayList<>();
            rectangles.add(new C2dRectangleObject());
            field.set(file, rectangles);
        } catch (Exception e) {
            fail("Failed to setup test: " + e.getMessage());
        }
        return file;
    }

    private C2dFile createC2dFileWithCurves() {
        C2dFile file = new C2dFile();
        try {
            java.lang.reflect.Field field = C2dFile.class.getDeclaredField("curveObjects");
            field.setAccessible(true);
            List<C2dCurveObject> curves = new ArrayList<>();
            curves.add(new C2dCurveObject());
            field.set(file, curves);
        } catch (Exception e) {
            fail("Failed to setup test: " + e.getMessage());
        }
        return file;
    }

    private C2dCurveObject createC2dCurveWithControlPoints() {
        C2dCurveObject curve = new C2dCurveObject();
        try {
            // Set cp1 field
            java.lang.reflect.Field cp1Field = C2dCurveObject.class.getDeclaredField("cp1");
            cp1Field.setAccessible(true);
            List<Double[]> cp1 = new ArrayList<>();
            cp1.add(new Double[]{0.0, 0.0});
            cp1Field.set(curve, cp1);
            
            // Set cp2 field
            java.lang.reflect.Field cp2Field = C2dCurveObject.class.getDeclaredField("cp2");
            cp2Field.setAccessible(true);
            List<Double[]> cp2 = new ArrayList<>();
            cp2.add(new Double[]{1.0, 1.0});
            cp2Field.set(curve, cp2);
        } catch (Exception e) {
            fail("Failed to setup test: " + e.getMessage());
        }
        return curve;
    }
}
