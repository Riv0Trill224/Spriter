package com.riv0trill.spriter.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Every eligible item is used before a new shuffled cycle begins. */
public final class ShuffleBag<T> {
    private final List<T> all;
    private final List<T> remaining = new ArrayList<>();
    private final Random random;

    public ShuffleBag(List<T> items, Random random) {
        all = new ArrayList<>(new LinkedHashSet<>(items));
        this.random = random;
    }

    public T next(Set<T> active) {
        if (all.isEmpty()) return null;
        for (int pass = 0; pass < 2; pass++) {
            for (int i = remaining.size() - 1; i >= 0; i--) {
                T item = remaining.get(i);
                if (!active.contains(item)) {
                    remaining.remove(i);
                    return item;
                }
            }
            // The only remaining items are active; defer them until a later cycle.
            remaining.clear();
            remaining.addAll(all);
            Collections.shuffle(remaining, random);
        }
        return null;
    }
}
