/*
 * This file is part of OrionAlpha, a MapleStory Emulator Project.
 * Copyright (C) 2018 Eric Smith <notericsoft@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package util;

/**
 * MapleStory RNG
 * 
 * @author Eric
 */
public class Rand32 {
    private static Rand32 g_rand;
    
    private int s1;
    private int s2;
    private int s3;
    
    /**
     * Construct a new Rand32 RNG.
     * 
     * Initializes the seeds of the RNG with the respective default values.
     * All initial seeds will be modified by the time(0) 
     */
    public Rand32() {
        this((int) (System.currentTimeMillis() / 1000));
    }
    
    public Rand32(int seed) {
        int rand = crtRand(seed);
        
        this.s1 = seed | 0x100000;
        this.s2 = rand | 0x1000;
        this.s3 = crtRand(rand) | 0x10;
    }
    
    /**
     * For use with all global/static Rand32 generated Randoms.
     * This is considered to be our global rand (g_rand).
     * 
     * @return The global Rand32 instance
     */
    public static Rand32 getInstance() {
        if (g_rand == null) {
            g_rand = new Rand32();
        }
        return g_rand;
    }
    
    /**
     * Nexon's old RNG formula used to create a random.
     * 
     * This formula is used when generating a Random in
     * KMS Beta clients and other old versions. In addition,
     * Nexon uses this formula for their Center communication
     * sequences.
     * 
     * @param seed The seed value to create the random.
     * @return The newly created Rand
     */
    public static int crtRand(int seed) {
        return 214013 * seed + 2531011;
    }
    
    /**
     * Generates a new random within a specified range (R) 
     * and beginning at a specified start (N).
     * 
     * @param range The maximum range of the random
     * @param start The minimum random
     * @return A new random
     */
    public static final Long getRand(int range, int start) {
        if (range != 0)
            return getInstance().random() % range + start;
        return getInstance().random();
    }
    
    /**
     * A shortcut to generating a random versus:
     * g_rand->Random()
     * or: GetInstance()->Random()
     * or: new Rand32().Random()
     * 
     * @return A new pseudorandom number
     */
    public static final Long genRandom() {
        return getInstance().random();
    }
    
    /**
     * Uses the available unsigned integer seeds,
     * bitshifts them around, updates the past seeds,
     * updates the current seeds, and returns an 
     * unsigned integer of the newly generated Random.
     * 
     * -->We do not need to unsigned shiftright because
     * we choose to use a standard int64 (long) as our
     * initialized v3~v6 variables. Only in the end do
     * we need to cast back our long to the bits of an
     * unsigned integer. 
     * 
     * -->In addition, we use a Long object for the simple
     * reason that we have the free ability to cast the Long
     * back into a intValue() returned by the object.
     * 
     * @return An unsigned integer Random
     */
    public Long random() {
        int v3;
        int v4;
        int v5;

        v3 = ((((s1 >> 6) & 0x3FFFFFF) ^ (s1 << 12)) & 0x1FFF) ^ ((s1 >> 19) & 0x1FFF) ^ (s1 << 12);
        v4 = ((((s2 >> 23) & 0x1FF) ^ (s2 << 4)) & 0x7F) ^ ((s2 >> 25) & 0x7F) ^ (s2 << 4);
        v5 = ((((s3 << 17) ^ ((s3 >> 8) & 0xFFFFFF)) & 0x1FFFFF) ^ (s3 << 17)) ^ ((s3 >> 11) & 0x1FFFFF);

        s3 = v5;
        s1 = v3;
        s2 = v4;
            
        return (s1 ^ s2 ^ s3) & 0xFFFFFFFFL;
    }
    
    public float randomFloat() {
        int uBits = (int) ((random() & 0x007FFFFF) | 0x3F800000);
        
        return Float.intBitsToFloat(uBits) - 1.0f;
    }

    public static byte randomByte() {
        return (byte) (Math.random() * 255);
    }
}
