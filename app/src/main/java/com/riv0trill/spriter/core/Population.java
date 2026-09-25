package com.riv0trill.spriter.core;
import java.util.Random;
/** Population follows usable area and sprite size; there is no five-sprite ceiling. */
public final class Population {
    public static int choose(int available, float width, float height, float edge, Random random) {
        if (available <= 0 || width <= 0 || height <= 0 || edge <= 0) return 0;
        int capacity = Math.min(available, Math.max(1, (int)(width * height * .38f / (edge * edge))));
        int low = Math.max(1, (int)Math.ceil(capacity * .3));
        return low + random.nextInt(capacity - low + 1);
    }
}
