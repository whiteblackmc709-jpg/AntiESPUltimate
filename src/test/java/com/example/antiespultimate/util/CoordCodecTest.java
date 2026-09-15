package com.example.antiespultimate.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoordCodecTest {

    @Test
    void roundTripPositive() {
        long p = CoordCodec.pack(100, 64, 200);
        assertEquals(100, CoordCodec.unpackX(p));
        assertEquals(64, CoordCodec.unpackY(p));
        assertEquals(200, CoordCodec.unpackZ(p));
    }

    @Test
    void roundTripNegative() {
        long p = CoordCodec.pack(-5000, -64, -12345);
        assertEquals(-5000, CoordCodec.unpackX(p));
        assertEquals(-64, CoordCodec.unpackY(p));
        assertEquals(-12345, CoordCodec.unpackZ(p));
    }

    @Test
    void roundTripZero() {
        long p = CoordCodec.pack(0, 0, 0);
        assertEquals(0, CoordCodec.unpackX(p));
        assertEquals(0, CoordCodec.unpackY(p));
        assertEquals(0, CoordCodec.unpackZ(p));
    }

    @Test
    void roundTripWorldBorderish() {
        // Paper's default world border is +/-29,999,984 -- well inside the
        // 26-bit signed range this codec supports.
        long p = CoordCodec.pack(29_999_000, 319, -29_999_000);
        assertEquals(29_999_000, CoordCodec.unpackX(p));
        assertEquals(319, CoordCodec.unpackY(p));
        assertEquals(-29_999_000, CoordCodec.unpackZ(p));
    }

    @Test
    void roundTripYBuildLimits() {
        // Vanilla/Paper 1.21.4 build height range is [-64, 320].
        long low = CoordCodec.pack(0, -64, 0);
        long high = CoordCodec.pack(0, 320, 0);
        assertEquals(-64, CoordCodec.unpackY(low));
        assertEquals(320, CoordCodec.unpackY(high));
    }

    @Test
    void distinctCoordsProduceDistinctKeys() {
        long a = CoordCodec.pack(1, 2, 3);
        long b = CoordCodec.pack(3, 2, 1);
        assertEquals(false, a == b);
    }
}
