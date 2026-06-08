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

package compiler.c2.irTests.scalarReplacement;

import jdk.test.lib.Asserts;

import compiler.lib.ir_framework.*;

/*
 * @test
 * @summary Test scalar replacement when EA instance memory slices are represented sparsely.
 * @library /test/lib /
 * @requires vm.compiler2.enabled & vm.opt.final.EliminateAllocations
 * @run driver compiler.c2.irTests.scalarReplacement.SparseSliceScalarReplacementTests
 */
public class SparseSliceScalarReplacementTests {
    // holder/global create non-instance memory traffic around the scalarized boxes.
    private static final Holder holder = new Holder();
    private static final Object marker = new Object();
    private static Object global;

    // Non-scalar-replaced object used to update general memory slices.
    static class Holder {
        int value;
    }

    // Scalar replacement candidates with primitive fields.
    static class IntBox {
        int x;
        int y;
    }

    // Scalar replacement candidates with reference fields.
    static class RefBox {
        Object ref;
        int y;
    }

    public static void main(String[] args) {
        TestFramework.run();
    }

    @Run(test = {"defaultIntAfterBranchMerge",
                 "defaultRefAfterBranchMerge",
                 "storedIntBeforeBranchMerge",
                 "storedRefBeforeBranchMerge",
                 "defaultIntAfterLoopMerge",
                 "defaultIntWithEscapingNeighbor",
                 "defaultRefLiveAcrossCall",
                 "mixedSparseAndUnmarkedMergeMem",
                 "storedIntThroughMultipleMergeMems",
                 "nestedSparseMergeMem",
                 "storedIntBehindNestedMerges",
                 "storedIntBehindLoopMerges"})
    public void runTests() {
        int value = RunInfo.getRandom().nextInt();
        assertResults(value);
        assertResults(0);
        assertResults(42);
        assertResults(-17);
    }

    @DontCompile
    private void assertResults(int value) {
        Asserts.assertEQ(value + 1, defaultIntAfterBranchMerge(true, value));
        Asserts.assertEQ(value + 3, defaultIntAfterBranchMerge(false, value));

        Asserts.assertEQ(value + 18, defaultRefAfterBranchMerge(true, value));
        Asserts.assertEQ(value + 20, defaultRefAfterBranchMerge(false, value));

        Asserts.assertEQ(value * 2 + 2, storedIntBeforeBranchMerge(true, value));
        Asserts.assertEQ(value * 2 + 4, storedIntBeforeBranchMerge(false, value));

        Asserts.assertEQ(value + 19, storedRefBeforeBranchMerge(true, value));
        Asserts.assertEQ(value + 21, storedRefBeforeBranchMerge(false, value));

        int limit = (value & 7) + 1;
        Asserts.assertEQ(limit * (limit + 1) / 2, defaultIntAfterLoopMerge(value));

        Asserts.assertEQ(value + 1, defaultIntWithEscapingNeighbor(true, value));
        Asserts.assertEQ(value + 3, defaultIntWithEscapingNeighbor(false, value));

        Asserts.assertEQ(value + 18 + opaque(value), defaultRefLiveAcrossCall(true, value));
        Asserts.assertEQ(value + 20 + opaque(value), defaultRefLiveAcrossCall(false, value));

        Asserts.assertEQ(value + 1, mixedSparseAndUnmarkedMergeMem(true, value));
        Asserts.assertEQ(value + 3, mixedSparseAndUnmarkedMergeMem(false, value));

        Asserts.assertEQ(value * 2, storedIntThroughMultipleMergeMems(true, true, value));
        Asserts.assertEQ(value * 2, storedIntThroughMultipleMergeMems(true, false, value));
        Asserts.assertEQ(value * 2, storedIntThroughMultipleMergeMems(false, true, value));
        Asserts.assertEQ(value * 2, storedIntThroughMultipleMergeMems(false, false, value));

        Asserts.assertEQ(value + 1, nestedSparseMergeMem(true, true, value));
        Asserts.assertEQ(value + 3, nestedSparseMergeMem(true, false, value));
        Asserts.assertEQ(value + 5, nestedSparseMergeMem(false, true, value));
        Asserts.assertEQ(value + 5, nestedSparseMergeMem(false, false, value));

        Asserts.assertEQ(value, storedIntBehindNestedMerges(true, true, value));
        Asserts.assertEQ(value, storedIntBehindNestedMerges(true, false, value));
        Asserts.assertEQ(value, storedIntBehindNestedMerges(false, true, value));
        Asserts.assertEQ(value, storedIntBehindNestedMerges(false, false, value));

        Asserts.assertEQ(value, storedIntBehindLoopMerges(value));
    }

    @DontInline
    private static int opaque(int value) {
        return value * 31 + 7;
    }

    // Reads a default int field after a branch MergeMem where only the general
    // int slice is explicit.
    @Test
    @IR(phase = CompilePhase.ITER_GVN_AFTER_ELIMINATION, failOn = {IRNode.ALLOC})
    public static int defaultIntAfterBranchMerge(boolean flag, int value) {
        IntBox box = new IntBox();
        if (flag) {
            holder.value = value;
            box.y = value + 1;
        } else {
            holder.value = value + 2;
            box.y = value + 3;
        }
        return box.x + box.y;
    }

    // Reads a default reference field through the same sparse branch shape.
    @Test
    @IR(phase = CompilePhase.ITER_GVN_AFTER_ELIMINATION, failOn = {IRNode.ALLOC})
    public static int defaultRefAfterBranchMerge(boolean flag, int value) {
        RefBox box = new RefBox();
        if (flag) {
            holder.value = value;
            box.y = value + 1;
        } else {
            holder.value = value + 2;
            box.y = value + 3;
        }
        return (box.ref == null ? 17 : 31) + box.y;
    }

    // Verifies that a store before the branch remains visible when later memory
    // merges keep the instance slice implicit.
    @Test
    @IR(phase = CompilePhase.ITER_GVN_AFTER_ELIMINATION, failOn = {IRNode.ALLOC})
    public static int storedIntBeforeBranchMerge(boolean flag, int value) {
        IntBox box = new IntBox();
        box.x = value;
        if (flag) {
            holder.value = value + 1;
            box.y = value + 2;
        } else {
            holder.value = value + 3;
            box.y = value + 4;
        }
        return box.x + box.y;
    }

    // Reference-field counterpart to storedIntBeforeBranchMerge.
    @Test
    @IR(phase = CompilePhase.ITER_GVN_AFTER_ELIMINATION, failOn = {IRNode.ALLOC})
    public static int storedRefBeforeBranchMerge(boolean flag, int value) {
        RefBox box = new RefBox();
        box.ref = marker;
        if (flag) {
            holder.value = value + 1;
            box.y = value + 2;
        } else {
            holder.value = value + 3;
            box.y = value + 4;
        }
        return (box.ref == marker ? 17 : 31) + box.y;
    }

    // Exercises sparse instance memory through a loop Phi rather than a branch Phi.
    @Test
    @IR(phase = CompilePhase.ITER_GVN_AFTER_ELIMINATION, failOn = {IRNode.ALLOC})
    public static int defaultIntAfterLoopMerge(int value) {
        IntBox box = new IntBox();
        int limit = (value & 7) + 1;
        for (int i = 0; i < limit; i++) {
            holder.value = value + i;
            box.y += i + 1;
        }
        return box.x + box.y;
    }

    // Keeps one neighboring allocation escaping while the non-escaping box uses
    // sparse default recovery.
    @Test
    @IR(phase = CompilePhase.ITER_GVN_AFTER_ELIMINATION, counts = {IRNode.ALLOC, "1"})
    public static int defaultIntWithEscapingNeighbor(boolean flag, int value) {
        IntBox box = new IntBox();
        IntBox escaping = new IntBox();
        global = escaping;
        if (flag) {
            escaping.y = value;
            box.y = value + 1;
        } else {
            escaping.y = value + 2;
            box.y = value + 3;
        }
        return box.x + box.y;
    }

    // Ensures reference defaults remain recoverable across a call that cannot
    // modify the scalar-replaced object.
    @Test
    @IR(phase = CompilePhase.ITER_GVN_AFTER_ELIMINATION, failOn = {IRNode.ALLOC})
    public static int defaultRefLiveAcrossCall(boolean flag, int value) {
        RefBox box = new RefBox();
        if (flag) {
            holder.value = value;
            box.y = value + 1;
        } else {
            holder.value = value + 2;
            box.y = value + 3;
        }
        int extra = opaque(value);
        return (box.ref == null ? 17 : 31) + box.y + extra;
    }

    // Mixes sparse and explicit MergeMem inputs so sparse fallback is not
    // propagated when doing so would change unmarked-input semantics.
    @Test
    @IR(phase = CompilePhase.ITER_GVN_AFTER_ELIMINATION, counts = {IRNode.ALLOC, "1"})
    public static int mixedSparseAndUnmarkedMergeMem(boolean flag, int value) {
        IntBox box = new IntBox();
        IntBox escaping = new IntBox();
        global = escaping;
        if (flag) {
            holder.value = value;
            box.y = value + 1;
        } else {
            escaping.x = value + 11;
            holder.value = value + 2;
            box.y = value + 3;
        }
        return box.x + box.y;
    }

    // Forces value recovery through multiple MergeMems for an explicitly stored field.
    @Test
    @IR(phase = CompilePhase.ITER_GVN_AFTER_ELIMINATION, failOn = {IRNode.ALLOC})
    public static int storedIntThroughMultipleMergeMems(boolean flag1, boolean flag2, int value) {
        IntBox box = new IntBox();
        box.x = value;
        if (flag1) {
            holder.value = value + 1;
        } else {
            holder.value = value + 2;
        }
        int first = box.x;
        if (flag2) {
            holder.value = value + 3;
        } else {
            holder.value = value + 4;
        }
        return first + box.x;
    }

    // Covers nested sparse MergeMems where the fallback must be preserved while
    // the nested base is simplified.
    @Test
    @IR(phase = CompilePhase.ITER_GVN_AFTER_ELIMINATION, failOn = {IRNode.ALLOC})
    public static int nestedSparseMergeMem(boolean flag1, boolean flag2, int value) {
        IntBox box = new IntBox();
        if (flag1) {
            if (flag2) {
                holder.value = value;
                box.y = value + 1;
            } else {
                holder.value = value + 2;
                box.y = value + 3;
            }
        } else {
            holder.value = value + 4;
            box.y = value + 5;
        }
        return box.x + box.y;
    }

    // The stored value is only reachable through two consecutive general-slice
    // memory merges; EA must keep it recoverable for the trap safepoints where
    // box is live.
    @Test
    @IR(phase = CompilePhase.ITER_GVN_AFTER_ELIMINATION, failOn = {IRNode.ALLOC})
    public static int storedIntBehindNestedMerges(boolean flag1, boolean flag2, int value) {
        IntBox box = new IntBox();
        box.x = 1;
        box.x = value;
        if (flag1) {
            holder.value = value + 1;
        } else {
            holder.value = value + 2;
        }
        if (flag2) {
            holder.value = value + 3;
        } else {
            holder.value = value + 4;
        }
        return box.x;
    }

    // Same shape with the stored value hidden behind a loop memory merge.
    @Test
    @IR(phase = CompilePhase.ITER_GVN_AFTER_ELIMINATION, failOn = {IRNode.ALLOC})
    public static int storedIntBehindLoopMerges(int value) {
        IntBox box = new IntBox();
        box.x = 1;
        box.x = value;
        int limit = (value & 7) + 1;
        for (int i = 0; i < limit; i++) {
            holder.value = value + i;
        }
        return box.x;
    }
}
