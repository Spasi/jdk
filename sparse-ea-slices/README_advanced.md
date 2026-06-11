# Sparse EA Instance Memory Slices — Reviewer Notes

Companion to `pr.md` (high-level overview). This document targets HotSpot/C2
reviewers and analyzes the two commits in detail:

1. `C2: Return Type::MEMORY directly from PhiNode::Value() for memory Phis`
2. `C2: Keep escape analysis instance memory slices sparse`

---

## TL;DR

`ConnectionGraph::split_unique_types()` used to eagerly materialize an
explicit slice for **every** new known-instance alias index in **every**
MergeMem on the mergemem worklist, plus the per-slice memory Phi splits this
transitively requires. That is an `instance-aliases × merge-points` cross
product of nodes whose value is almost always just the matching general
slice. The patch makes the representation sparse:

- A MergeMem processed by EA is **marked**
  (`_use_general_memory_for_unset_instance_slices`). On a marked node,
  `memory_at(idx)` resolves an *absent* known-instance slice to the matching
  **general** slice (per `Compile::get_general_index()`) instead of the base
  memory. The physical lookup survives as `memory_at_base(idx)`.
- Explicit instance slices and split Phis are created **on demand** by the
  memory walks that need precise instance memory (`find_inst_mem()`,
  `move_inst_mem()`, `PhiNode::Ideal` split-through-MergeMem), not up front.
- One new EA pass (`materialize_instance_slices_at_safepoints()`, "Phase
  3.5") force-materializes precise instance memory along exactly the chains
  that scalar replacement value recovery will walk during macro expansion.
  This is a **correctness requirement**, not an optimization: Phase 4 /
  `move_inst_mem()` rewrite general chains to bypass known-instance stores,
  after which the sparse general fallback can no longer recover those stores,
  and `PhaseMacroExpand::value_from_mem()` would record stale field values in
  debug info (wrong rematerialized objects after deopt). The pass is filtered
  to aliases written by *open* (non-Initialize-captured) stores, so the
  common constructor-only case does no work.
- `PhaseMacroExpand` value recovery is hardened for walks that traverse
  general slices (provable store-address matching, conservative bailouts).

Commit 1 is independent: `PhiNode::Value()` returns `Type::MEMORY`
immediately for live memory Phis instead of meeting the input types.

Net effect: post-EA node count stays near pre-EA size, macro elimination and
IGVN no longer churn through redundant slices/Phis, generated code is
unchanged. Validated with tier1_compiler, compiler/escapeAnalysis,
compiler/c2/irTests/scalarReplacement (release + fastdebug), plus new IR and
deopt-rematerialization regression tests.

---

## Commit 1: `PhiNode::Value()` early-out for memory Phis

**Change** (`cfgnode.cpp`): after the existing dead-region check
(`type_or_null(r) == TOP`), return `Type::MEMORY` when `_type ==
Type::MEMORY`.

**Rationale.** For a memory Phi the meet over inputs can only produce
`Type::MEMORY` (inputs are memory nodes typed `MEMORY`) or `TOP` (all inputs
dead). Alias precision lives in `_adr_type`, not in the value lattice. The
generic path still walks all inputs, calls `phase->type()` on each, and runs
the meet — pure overhead on every IGVN visit. Wide memory Phis are revisited
often (every slice store on any path notifies them), so with many of them the
cost is measurable.

**Behavioral delta.** Exactly one case changes: a memory Phi on a *live*
region whose data inputs are all `TOP` now stays `MEMORY` instead of becoming
`TOP`. Such Phis exist only transiently while a region degenerates;
`Identity`/`Ideal` remove them when control collapses. The dead-*region* case
still returns `TOP` before the early-out, so dead-code elimination through
control is unaffected. Trip-counted-loop logic below the early-out only ever
applied to the loop's data Phi (`l->phi() == this`), never a memory Phi.

**Risk:** low. Monotonicity is trivially preserved (constant function).
fastdebug IGVN type verification passes.

---

## Commit 2: sparse instance slices

### Core invariant

For a MergeMem `m` with the sparse mark set:

> An empty slice at known-instance alias `i` denotes the same memory state as
> `m->in(general_index(i))` if that general slice is explicit, else
> `m->base_memory()` (the pre-existing default).

`memory_at()` implements this resolution; `memory_at_base()` is the old
physical behavior. All semantic consumers (GraphKit, gcm, macro expansion,
loop opts, …) go through `memory_at()` unchanged. The only call sites
switched to `memory_at_base()` are those that must *detect absence*:

- `ConnectionGraph::step_through_mergemem()` — `find_inst_mem()` uses
  "result == base_memory()" as its trigger to search the general chain and
  lazily `set_memory_at(ni, …)` the precise slice. `memory_at()` would mask
  absence and defeat lazy materialization.
- `MergeMemNode::set_base_memory()`'s assert (checks the physical default).

### MergeMemNode representation (`memnode.{hpp,cpp}`)

- New `bool _use_general_memory_for_unset_instance_slices`, default false.
  Only EA sets it (Phase 3, see below); IGVN propagates it (see Ideal/Phi
  split). Monotonic false→true.
- `size_of()` override added — required so `Node::clone()` copies the flag.
- GVN interaction: MergeMem `hash()` is `NO_HASH` and `cmp()` is
  identity-only, so the new state can never cause incorrect value-numbering
  commoning. No `hash/cmp` changes needed.
- The `MergeMemNode(Node*)` constructor copies the flag when cloning from
  another MergeMem ("absent slice" must keep its interpretation).
- `memory_at()` fast path: only consults `Compile::current()` when the
  physical slice is empty *and* the flag is set; `general_index(i) != i`
  alone identifies known-instance aliases (only those have a distinct general
  index per `AliasType::Init`), so no `get_adr_type()`/`isa_oopptr()` calls
  on the lookup path.

`MergeMemNode::Ideal()` changes, in order:

1. **Base flattening** (`old_mbase` case): when the stacked base MergeMem is
   sparse, the outer node inherits the mark. The outer general slot receives
   the nested general slice through the normal per-slice loop, so the
   fallback target stays correct.
2. The per-slice assert `old_mem == memory_at(i)` is relaxed to
   `old_in == empty_mem || old_in == memory_at(i)`: for an empty slice the
   semantic value may legitimately be the general slice, not `old_base`.
3. **Nested sparse MergeMem on a slice** (`old_mmem` case): if the nested
   node is sparse and has no explicit entry at instance alias `i`, the outer
   node keeps the slice empty (`new_mem = new_base`) and inherits the mark,
   instead of calling `old_mmem->memory_at(i)` — which would *materialize*
   the general slice into the outer instance slot and re-densify the graph
   one alias at a time as Ideal runs.
4. `set_base_memory()` assert switched to `memory_at_base()` (see above).

Note for review: Ideal mutates the flag even on no-`progress` returns. The
mutation is monotonic and only widens the set of slices interpreted via the
fallback, matching the semantic value those slices already had through the
nested node; it does not invalidate prior `memory_at()` results.

`MemNode::optimize_memory_chain()` / static `step_through_mergemem()`:

- Fast path in `step_through_mergemem()`: if the resolved slice is not a
  MergeMem and either the slice is explicit or the base is not a MergeMem,
  return it without `phase->transform(mmem)`. Avoids recursive MergeMem
  idealization on the common lookup; stacked-MergeMem rollup still happens
  when the conditions don't hold (and via normal IGVN scheduling otherwise).
- `optimize_memory_chain()` could previously assume the walk for a
  known-instance address ends on a Phi of slice BOTTOM/raw or the exact
  alias; with sparse memory it can end on a *general-slice* Phi. Two guards:
  the array-property refinement now checks `isa_aryptr()` before
  `is_aryptr()` (the general slice of an array elem query can be a non-array
  type — this was an assert/crash in fastdebug), and an alias mismatch
  returns `mchain` unchanged instead of asserting, declining the split
  (`split_out_instance` on a general-slice Phi would change meaning).

`InitializeNode::narrow_mem_proj_or_null()` is factored out of
`already_has_narrow_mem_proj_with_adr_type()` so EA can look up an existing
narrow projection without forcing creation.

### Escape analysis (`escape.cpp`)

**Phase 2** additionally records, in a `VectorSet open_instance_aliases`,
every new instance alias written by a non-Load memory node. Captured
initializing stores are naturally excluded: they use raw addresses
(`AliasIdxRaw < new_index_start`). This set drives Phase 3.5.

**Phase 3** now only: sets the sparse mark on each worklist MergeMem, then
runs the pre-existing per-slice loop that re-slices memory nodes whose
address type became precise. The two eager loops are deleted:

- the per-slice `find_inst_mem()` for every `ni` with
  `general_index(ni) == i`, and
- the trailing "find the rest of instances values" loop over **all** new
  alias indexes.

These two loops were the cross product. Their effect is replaced by the
sparse fallback (for slices equal to the general slice) and by lazy/targeted
materialization (for slices that aren't).

**Phase 3.5** (`materialize_instance_slices_at_safepoints()`), runs after the
MergeMem worklist, before `print_method(…_3)` and Phase 4:

- Builds the (instance_id, alias) table restricted to
  `open_instance_aliases`. Empty table (constructor-only stores) ⇒ no work.
- For every candidate `CheckCastPP` with a known-instance type, BFS over its
  value uses through Phi / ConstraintCast / EncodeNarrowPtr / DecodeNarrowPtr
  / **SafePointScalarMerge** (reduced allocation merges reference candidates
  through SafePointScalarMerge, not directly), collecting
  `in(TypeFunc::Memory)` of every JVMS-bearing SafePoint use. ArrayCopy
  nodes are SafePoints, so self-copies are covered here too; the explicit
  `arraycopy_worklist` loop afterwards covers ACs whose `Src/Dest` is an
  AddP over the cast (not a direct use).
- For each collected memory state × live open alias: skip if the slice is
  already explicit in that MergeMem, otherwise `find_inst_mem(mem, ni, …)`.
  The *result is discarded*; the calls are made for their side effects —
  `set_memory_at()` on every MergeMem stepped through and
  `split_memory_phi()` on every wide Phi crossed. These are exactly the
  lookups `scan_mem_chain()`/`value_from_mem()` will repeat at macro time.
- Same `live_nodes >= 0.75 * max_node_limit` bailout policy as Phase 3.

Why this placement is sound: at this point the original memory edges are
still intact (Phase 4 hasn't rewritten `MemNode::Memory` inputs;
`move_inst_mem()` hasn't moved store users), so `find_inst_mem()` still sees
known-instance stores on general chains and can split Phis input-by-input
with correct values.

**The deopt hazard this closes** (and the reason a "shallow" alternative was
rejected): Phase 4 pushes every general-slice Phi it touches onto
`orig_phis` and re-runs `find_inst_mem(…, general_idx, …)` on its inputs,
which *skips* known-instance stores — recursively, to arbitrary Phi depth.
Any instance store reachable only through ≥2 nested general merges at a
safepoint would be stripped from the general chain while the instance slice
was never materialized in the safepoint's MergeMem; `value_from_mem()` then
walked past the store to the captured init value and recorded **stale debug
info** — silently wrong field values after rematerialization. The regression
test `TestSparseSliceDeoptValues` encodes the failing shapes (two nested
branch merges, loop merge, self-arraycopy) and fails against a build with
Phase 3.5 disabled.

**Lazy paths** (replacing eager pre-creation):

- `find_inst_mem_initialize_proj()`: the narrow `NarrowMemProjNode` of an
  Initialize for alias `ni` is created on first demand (previously
  guaranteed pre-created; the old code asserted `get_map() != nullptr`).
  Reuses an existing projection via `narrow_mem_proj_or_null()` and caches
  through `set_map(general_proj, new_proj)`.
- `move_inst_mem()`: a store `n` may now be reachable at `alias_idx` only
  via the sparse fallback of a using MergeMem. Such MergeMems are collected
  during the `DUIterator_Fast` walk and materialized
  (`set_memory_at(alias_idx, n)`) *after* it, since `set_memory_at` would
  append to `n`'s use list mid-iteration.
- `create_split_phi()`: with lazy splitting, several wide Phis on one region
  can each be split for the same alias, so the old fallback — "any new Phi on
  this region with matching alias" — can return a split of a *different*
  original Phi (wrong memory chain). Split Phis now record their origin via
  `set_inst_mem_id(orig_phi->_idx)`, and the region scan requires
  `type() == Type::MEMORY && _idx >= nodes_size() && inst_mem_id() ==
  orig_phi->_idx` plus the alias match, then re-caches with `set_map()`.
  Note `_inst_mem_id` maintenance across `subsume_node()` and
  `PhaseRenumberLive` already exists upstream in `phaseX.cpp` and covers
  memory Phis (`type()->has_memory()`); this patch only extends the field's
  *use* (comment updated in `cfgnode.hpp`). Collision with the data-Phi use
  of `is_same_inst_field()` is excluded by the `Type::MEMORY` check.

### `PhiNode::Ideal` split-through-MergeMem (`cfgnode.cpp`)

The `Phi(...MergeMem...)` → `MergeMem(Phi, Phi:AT1, ...)` transform is
restructured:

- The base Phi clone now resolves self-loops up front (input `== this`, or a
  MergeMem input whose `base_memory() == this`, maps to the clone), instead
  of the separate "distribute all self-loops" fixup pass.
- Per-slice Phis are created only for aliases **explicit in at least one
  MergeMem input** (iterating each input's own slices), rather than for every
  slice of the pair-stream. Inputs are selected with `memory_at(alias)`,
  which transparently yields the general slice for sparse inputs and the
  base for unmarked ones — i.e. the same values the old code read from fully
  materialized inputs.
- The resulting MergeMem inherits the sparse mark **only if every MergeMem
  input is marked**. With any unmarked input, an absent instance slice means
  "base memory" for that input but "general slice" for marked ones; the two
  interpretations differ, so the result conservatively materializes nothing
  and stays unmarked (absent ⇒ base Phi, which is the meet of the inputs'
  defaults — correct for both kinds).

### Macro expansion / scalar replacement (`macro.cpp`)

`scan_mem_chain()` previously asserted that any store on the walk whose alias
doesn't match is raw. With sparse memory the walk legitimately traverses
*general* slices, which contain stores by other objects and — before EA
rewiring or for not-yet-materialized cases — stores to the target object
typed on the general slice. New logic for the non-matching-alias store case:

- If `AllocateNode::Ideal_allocation(store->in(Address))` folds to the target
  allocation at exactly `offset` **and** the address type is an oopptr, the
  store is the sought value (the oopptr requirement keeps raw stores on the
  old skip path: a raw store at a matching offset is not proven to be a
  same-width field write).
- If it folds to the target allocation but at an unprovable offset
  (`OffsetBot/Top`), return `nullptr`: the store *might* alias the field and
  no reliable value exists.
- Otherwise skip it (it cannot affect a non-escaping object), with the assert
  relaxed to allow known-instance scans over general memory.

`nullptr` from `scan_mem_chain()` is propagated conservatively:

- `value_from_mem()` returns `nullptr` ⇒ `scalar_replacement()` gives up on
  the allocation (no SR rather than wrong SR).
- `value_from_mem_phi()` bails out before `find_captured_store()` instead of
  dereferencing.
- The Phi unique-input scan in `value_from_mem()` treats an unknown input as
  disqualifying (`has_unknown_input`): using another input's value would leak
  a branch-local value past the merge.
- The store-match assert in `value_from_mem()` accepts a provably-folding
  store (`is_store_to_instance_field()`) as an alternative to an exact
  instance-typed alias match.

### Performance characteristics

- Phase 3 drops from `O(worklist × new_aliases)` `find_inst_mem()` calls to
  a mark + the pre-existing single slice scan.
- Phase 3.5 is bounded by (live candidates at safepoints) × (their open-store
  aliases) and is memoized through the materialized slices themselves: the
  second query of the same (mem, alias) hits the explicit slice and returns
  immediately. Constructor-only candidates (the common case) skip the pass
  entirely via the `open_instance_aliases` filter.
- `memory_at()` adds two compares to the empty-slice path of unmarked nodes;
  marked nodes add a `general_index` table lookup. The explicit-slice path is
  unchanged.
- The `step_through_mergemem()` fast path removes a `transform()` per memory
  chain step in the common case.

### Testing

- `compiler/c2/irTests/scalarReplacement/SparseSliceScalarReplacementTests.java`
  (new): IR-verified full scalar replacement (`failOn = ALLOC`) for default
  and stored, primitive and reference fields through branch / loop / nested /
  multi-merge shapes, plus mixed sparse/unmarked MergeMem and
  escaping-neighbor variants (`counts = {ALLOC, "1"}`).
- `compiler/escapeAnalysis/TestSparseSliceDeoptValues.java` (new): verifies
  rematerialized field values at an uncommon trap for the shapes that exposed
  the recovery hazard: store behind two nested general merges, behind a loop
  merge, and a self-`System.arraycopy` long enough to survive to macro
  expansion as an ArrayCopyNode. Fails (stale values) if Phase 3.5 is
  disabled.
- jtreg `tier1_compiler`, `compiler/escapeAnalysis`,
  `compiler/c2/irTests/scalarReplacement` pass on release and fastdebug
  (including the `ReduceAllocationMerges`/`DeoptimizeALot` scenarios of
  `AllocationMergesTests`, which exercise SafePointScalarMerge coverage in
  Phase 3.5).

### Suggested review focus

1. The core invariant and every `memory_at()` vs `memory_at_base()` call
   site: the patch deliberately changes only two sites to the physical
   lookup; all other consumers must want semantic resolution.
2. Sparse-mark propagation rules: MergeMem clone, both Ideal paths, and the
   all-inputs-marked condition in the Phi split. Each either preserves the
   interpretation of absent slices or conservatively materializes/unmarks.
3. Completeness of Phase 3.5's safepoint discovery (value-flow node kinds:
   Phi, ConstraintCast, En/DecodeNarrowPtr, SafePointScalarMerge) against
   the ways a candidate can be referenced from JVMS debug info after Phase 1
   rewiring; and the claim that Initialize-captured stores need no
   materialization (raw address ⇒ reachable through the Initialize node,
   `find_captured_store()` path unchanged).
4. The relaxed store matching in `scan_mem_chain()`: the
   oopptr-and-exact-offset condition for accepting, and the
   unprovable-offset `nullptr` for rejecting, with conservative propagation
   in all `value_from_mem*` callers.
