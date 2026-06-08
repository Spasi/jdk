/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @summary Deoptimization must rematerialize correct field values when escape
 *          analysis keeps instance memory slices sparse and the stored value is
 *          only reachable through nested general-slice memory merges.
 * @run main/othervm -Xbatch -XX:-TieredCompilation
 *      compiler.escapeAnalysis.TestSparseSliceDeoptValues
 * @run main compiler.escapeAnalysis.TestSparseSliceDeoptValues
 */

package compiler.escapeAnalysis;

public class TestSparseSliceDeoptValues {
    static class T {
        int x;
    }

    static T global = new T();
    static int[] globalArr = new int[4];

    // The store of v to box.x is followed by two branch merges updating only
    // general memory. At the rare trap, box must rematerialize with x == v,
    // not with the captured initializing store.
    static int testTwoMerges(boolean c0, boolean c1, boolean c2, int v, boolean rare) {
        T box = new T();
        box.x = 1;
        T other = global;
        if (c0) { other.x = 5; } else { other.x = 6; }
        box.x = v;
        if (c1) { other.x = 7; } else { other.x = 8; }
        if (c2) { other.x = 9; } else { other.x = 10; }
        if (rare) {
            return box.x + 1000;
        }
        return box.x;
    }

    // Same shape with the store hidden behind a loop memory merge.
    static int testLoopMerge(int v, int n, boolean rare) {
        T box = new T();
        box.x = 1;
        T other = global;
        box.x = v;
        for (int i = 0; i < n; i++) {
            other.x = i;
        }
        if (rare) {
            return box.x + 1000;
        }
        return box.x;
    }

    // The array is copied onto itself, so deoptimization recovers the moved
    // element values by searching the arraycopy's memory state. The open store
    // to a[0] is hidden behind branch merges of the general int[] element
    // slice, and the copy is long enough to reach macro expansion as an
    // ArrayCopy node.
    static int testSelfArrayCopy(boolean c0, boolean c1, int v, boolean rare) {
        int[] a = new int[12];
        int[] other = globalArr;
        if (c0) { other[0] = 5; } else { other[1] = 6; }
        a[0] = v;
        if (c1) { other[2] = 7; } else { other[3] = 8; }
        System.arraycopy(a, 0, a, 1, 11);
        if (rare) {
            return a[1] + 1000;
        }
        return a[1];
    }

    public static void main(String[] args) {
        for (int i = 0; i < 20_000; i++) {
            check(testTwoMerges((i & 1) == 0, (i & 2) == 0, (i & 4) == 0, 42, false), 42);
            check(testLoopMerge(42, (i & 7) + 1, false), 42);
            check(testSelfArrayCopy((i & 1) == 0, (i & 2) == 0, 42, false), 42);
        }
        // Compiled by now (-Xbatch). The rare path deoptimizes and must
        // rematerialize box with the stored value.
        check(testTwoMerges(true, true, true, 77, true), 1077);
        check(testLoopMerge(77, 3, true), 1077);
        check(testSelfArrayCopy(true, true, 77, true), 1077);
    }

    static void check(int got, int expected) {
        if (got != expected) {
            throw new RuntimeException("got " + got + " expected " + expected);
        }
    }
}
