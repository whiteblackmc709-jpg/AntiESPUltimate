package com.example.antiespultimate.util;

/**
 * Packs a (x, y, z) block coordinate into a single long so it can be stored
 * in a Set&lt;Long&gt; without the per-entry overhead of a String key.
 *
 * Layout (MSB -> LSB): [26 bits x][12 bits y][26 bits z]
 * 26 bits signed covers roughly +/-33.5M blocks -- far past the +/-30M
 * world border. 12 bits covers y in [-2048, 2047], comfortably past the
 * playable [-64, 320] build limit.
 */
public final class CoordCodec {

    private CoordCodec() {}

    public static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (z & 0x3FFFFFF);
    }

    public static int unpackX(long packed) {
        return signExtend((int) ((packed >> 38) & 0x3FFFFFF), 26);
    }

    public static int unpackY(long packed) {
        return signExtend((int) ((packed >> 26) & 0xFFF), 12);
    }

    public static int unpackZ(long packed) {
        return signExtend((int) (packed & 0x3FFFFFF), 26);
    }

    private static int signExtend(int value, int bits) {
        int shift = 32 - bits;
        return (value << shift) >> shift;
    }
}
