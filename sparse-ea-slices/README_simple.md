# Sparse Escape Analysis Memory Slices in C2

This document explains, for readers unfamiliar with HotSpot internals, what the
two commits in this change do, why the previous implementation had a problem,
and how the patch solves it.

1. `C2: Return Type::MEMORY directly from PhiNode::Value() for memory Phis`
2. `C2: Keep escape analysis instance memory slices sparse`

---

## Background concepts

### C2, the JIT compiler

HotSpot runs Java bytecode in an interpreter and, for hot methods, compiles
them to optimized machine code at runtime. **C2** is HotSpot's optimizing
just-in-time (JIT) compiler. Compilation time matters: while C2 is busy, the
program runs slower interpreted/less-optimized code, so a method that takes
seconds to compile delays the application from reaching full speed.

### The "sea of nodes" and Phi nodes

C2 represents a method as a graph of small operations called **nodes** (an
"ideal graph"). A node might be "load this field", "add these two values" or
"this is where two branches of an `if` meet".

When control flow merges (after an `if`/`else`, or at the top of a loop), a
value might be different depending on which path was taken. A **Phi node**
expresses this: "the value here is X if we came from the left branch, Y if we
came from the right". Phis exist for ordinary values (integers, references)
and also for *memory* (see below).

During optimization C2 repeatedly asks every node for its **type** — the most
precise description of the values it can produce ("an int between 0 and 9",
"a non-null String", ...). Sharper types enable more optimizations, so this
question is asked many times for every node.

### How C2 models memory, and MergeMem nodes

To reorder and remove loads and stores safely, C2 needs to know which memory
operations can affect each other. It partitions memory into **slices** (alias
classes): all `Point.x` fields form one slice, all `int[]` elements another,
and so on. A store to `Point.x` can never change an `int[]` element, so
operations on different slices can be optimized independently.

Most nodes only touch one slice. But some points in the graph — method calls,
loop entries, places where debug information is recorded — need to represent
the state of *all* memory at once. A **MergeMem node** does this: it is a
table mapping each memory slice to the chain of memory operations that
produced that slice's current state, plus a "base memory" used as the default
for every slice without an explicit entry.

Memory also flows through Phis: after an `if`/`else` where both branches
store to fields, a **memory Phi** merges the two memory states.

### Escape analysis and scalar replacement

**Escape analysis (EA)** determines, for each object allocation, whether the
object can "escape" the compiled method — be stored in a global, passed to
another method, etc. If an object provably never escapes, no other code can
observe it, and C2 may apply **scalar replacement**: delete the allocation
entirely and keep the object's fields in registers, as if they were local
variables. This removes allocation cost, garbage collection pressure and
memory traffic, and is one of C2's most valuable optimizations.

To do this, EA gives each non-escaping object its own private memory slices
("the `x` field *of this particular Point*" rather than "any `Point.x`").
These per-object slices are called **instance slices**. With them, C2 knows
that nothing else in the program can touch that object's fields, which is
what makes deleting the object safe.

One subtlety: even though the object is deleted, the program must still
behave as if it exists. If the compiled code has to fall back to the
interpreter (a **deoptimization**, e.g. when a rare branch is finally taken),
HotSpot *rematerializes* the object — allocates it for real and fills in its
fields. To do this, the compiler records, at every relevant point, where each
field's current value can be found. Getting these recorded values right is a
hard correctness requirement: a mistake doesn't crash, it silently resurrects
an object with wrong field contents.

---

## The problem

Both commits attack the same scalability problem, which appears when many
allocations are scalar-replaceable in a large method — typically the result
of aggressive inlining, since inlining a constructor or factory method is
what exposes allocations to EA in the first place.

### Eager materialization (the main issue)

The previous EA implementation was *eager*: after deciding which objects
don't escape, it walked every MergeMem in the method and inserted an explicit
entry for **every new instance slice into every MergeMem**, whether or not
anything would ever ask for that slice there. Filling those entries also
forced memory Phis to be split per slice: a Phi merging "all memory" had to
become a separate Phi for each instance slice it carried.

The cost is a cross product. A method with *N* scalar-replaceable objects
(each contributing a few instance slices) and *M* memory-merge points gets on
the order of *N × M* new graph entries and Phi splits. In heavily inlined
methods both N and M can be in the hundreds, so the graph blows up with
thousands of nodes that say nothing more than "this slice is the same as the
general one" — which is almost always true, because most merge points lie
nowhere near the few stores that actually touch a given object.

Every later optimization pass then has to walk this inflated graph. The time
is not even spent in EA itself: it shows up in the passes that follow,
especially macro elimination (the pass that performs scalar replacement) and
iterative optimization rounds. Compile times that should be fractions of a
second grow to tens of seconds, while the final machine code is identical —
all of the extra structure exists only to be cleaned up again.

### Redundant type recomputation (the small issue)

Independently, the generic "what is your type?" computation for Phi nodes
merges the types of all inputs. For a *memory* Phi this is wasted work: the
answer is always simply "memory" (the slice information lives in a separate
field, not in the type). With thousands of wide memory Phis from the blow-up
above, recomputing this constant answer on every optimization round became
measurable on its own.

---

## What the patch does

### Commit 1: constant type for memory Phis

`PhiNode::Value()` now returns the constant "memory" type immediately for
live memory Phis instead of merging input types. Dead Phis (on unreachable
control flow) are still detected before the early return. This is a small,
self-contained optimization that is valid independently of the second commit.

### Commit 2: sparse instance slices

The core idea: **stop materializing instance-slice entries that carry no
information, and create the few that matter on demand.**

1. **A missing entry now has a defined meaning.** A MergeMem that EA has
   processed is marked. For a marked MergeMem, a lookup of an instance slice
   that has no explicit entry answers with the matching *general* slice (all
   `Point.x` rather than *this* `Point`'s `x`). That is exactly the state the
   eager code used to copy into the table — so the graph means the same
   thing, it just doesn't spell out every cell of the table anymore. The mark
   is preserved when MergeMems are cloned, simplified, or split through Phis,
   so the interpretation never silently changes.

2. **Slices appear only where they are needed.** The code paths that actually
   consume precise per-object memory (the memory-chain walks EA uses to wire
   loads and stores to the right slice) create an instance-slice entry or a
   per-slice Phi at the moment they discover it is needed, instead of ahead
   of time everywhere.

3. **Deoptimization data stays correct.** This is the delicate part. The
   recorded "where is each field's value" information is computed late, by
   walking memory backwards from each safepoint (a point where deoptimization
   can happen). By that time, EA's rewiring has removed per-object stores
   from the *general* memory chains — so a sparse fallback to the general
   slice could miss a store and record a stale value, which would later
   resurrect an object with wrong field contents. The patch closes this hole
   with a targeted pass: for every non-escaping object that is live at a
   safepoint, it materializes precise instance memory along exactly the
   chains that the later recovery walk will follow, while those chains are
   still intact. The pass is itself demand-driven — it only processes fields
   written by ordinary stores; fields only set in constructors (the common
   case) are reachable through a different, stable mechanism and need no
   work at all. Regression tests verify rematerialized field values in the
   shapes that used to go wrong (stores hidden behind nested merges, loops,
   and array self-copies).

### What it improves

- **Much faster compilation** of methods with many scalar-replaceable
  allocations: graph size after EA stays close to the pre-EA size instead of
  growing with the slices × merge-points product, and all downstream passes
  benefit. On heavily inlined workloads this turns multi-second (in
  pathological cases tens of seconds) C2 compilations into fractions of a
  second, which also means applications reach peak performance sooner.
- **No change to generated code.** The same allocations are scalar-replaced;
  the same optimizations apply. The sparse representation is semantically
  identical to the eager one.
- **No change to observable behavior**, including deoptimization: objects
  rematerialized after a deopt receive the same (correct) field values as
  before.

The change passes the JDK's jtreg test suites (including `tier1_compiler`,
the escape analysis and scalar replacement suites) on both release and
fastdebug builds, and adds new IR-framework and deoptimization regression
tests for the sparse representation.
