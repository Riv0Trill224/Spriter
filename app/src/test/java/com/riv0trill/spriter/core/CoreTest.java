package com.riv0trill.spriter.core;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public final class CoreTest {
    @Test public void collectionIsExhaustedBeforeRepeating() {
        List<Integer> files = Arrays.asList(1,2,3,4,5,6,7,8,9,10);
        ShuffleBag<Integer> bag = new ShuffleBag<>(files, new Random(4));
        for (int cycle=0; cycle<20; cycle++) {
            Set<Integer> seen = new HashSet<>();
            for (int i=0; i<files.size(); i++) assertTrue(seen.add(bag.next(Collections.emptySet())));
            assertEquals(new HashSet<>(files), seen);
        }
    }
    @Test public void fiveActiveSpritesStayUniqueAcrossManyRotations() {
        ShuffleBag<Integer> bag = new ShuffleBag<>(Arrays.asList(1,2,3,4,5,6,7,8),new Random(7));
        List<Integer> active = new ArrayList<>(); Set<Integer> seen = new HashSet<>();
        for (int i=0; i<5; i++) active.add(bag.next(new HashSet<>(active)));
        seen.addAll(active);
        for (int i=0; i<1000; i++) {
            Integer next = bag.next(new HashSet<>(active));
            assertNotNull(next); assertFalse(active.contains(next));
            active.set(i%5,next); seen.add(next);
            assertEquals(5,new HashSet<>(active).size());
            if (i == 2) assertEquals(8,seen.size());
        }
    }
    @Test public void smallAndEmptyCollectionsAreSafe() {
        ShuffleBag<Integer> empty = new ShuffleBag<>(Collections.emptyList(),new Random(1));
        assertNull(empty.next(Collections.emptySet()));
        ShuffleBag<Integer> one = new ShuffleBag<>(Arrays.asList(1,1),new Random(1));
        assertEquals(Integer.valueOf(1),one.next(Collections.emptySet()));
        assertNull(one.next(Collections.singleton(1)));
    }
    @Test public void invalidImagesDoNotBlockTheRemainingCollection() {
        ShuffleBag<Integer> bag = new ShuffleBag<>(Arrays.asList(1,2,3,4),new Random(1));
        for (int i=0; i<50; i++) {
            int result = bag.next(new HashSet<>(Arrays.asList(1,2,3)));
            assertEquals(4,result);
        }
    }
    @Test public void bouncePreservesOvershootAndChangesDirection() {
        Motion motion = new Motion(95,2,20,-10);
        motion.advance(1,100,100);
        assertEquals(85,motion.x,.001f); assertEquals(8,motion.y,.001f);
        assertEquals(-20,motion.vx,.001f); assertEquals(10,motion.vy,.001f);
    }
    @Test public void movementIsIndependentOfFrameRate() {
        Motion slow = new Motion(20,20,47,31), fast = new Motion(20,20,47,31);
        for (int i=0; i<15*20; i++) slow.advance(1f/15,480,300);
        for (int i=0; i<30*20; i++) fast.advance(1f/30,480,300);
        assertEquals(slow.x,fast.x,.05f); assertEquals(slow.y,fast.y,.05f);
    }
    @Test public void spritesRemainInBoundsIncludingLargeStepsAndResize() {
        Motion m = new Motion(400,300,500,-300);
        for (int i=0; i<1000; i++) {
            m.advance(.1f,37,23);
            assertTrue(m.x >= 0 && m.x <= 37); assertTrue(m.y >= 0 && m.y <= 23);
        }
        m.advance(1,0,-1); assertEquals(0,m.x,0); assertEquals(0,m.y,0);
    }
}
