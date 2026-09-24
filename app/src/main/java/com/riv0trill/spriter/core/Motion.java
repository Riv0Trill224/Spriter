package com.riv0trill.spriter.core;

/** Pure time-based physics, independent of Android or the rendering frame rate. */
public final class Motion {
    public float x, y, vx, vy;
    public Motion(float x, float y, float vx, float vy) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy;
    }
    public void advance(float seconds, float maxX, float maxY) {
        x += vx * seconds;
        y += vy * seconds;
        if (maxX <= 0) x = 0;
        else {
            while (x < 0 || x > maxX) {
                if (x < 0) { x = -x; vx = Math.abs(vx); }
                if (x > maxX) { x = 2 * maxX - x; vx = -Math.abs(vx); }
            }
        }
        if (maxY <= 0) y = 0;
        else {
            while (y < 0 || y > maxY) {
                if (y < 0) { y = -y; vy = Math.abs(vy); }
                if (y > maxY) { y = 2 * maxY - y; vy = -Math.abs(vy); }
            }
        }
    }
}
