# Sparse Escape Analysis Memory Slices in C2

This branch contains a prototype that fixes a HotSpot scalability problem
which appears when many allocations are scalar-replaceable in a large method.

The patch passes jtreg validation (headless tests).

The [FFMStructAccessTest::copyReinterpretInline16](sparse-ea-slices/FFMStructAccessTest.java)
method was used to validate the optimization. This is not realistic Java
code, but it is a good stress test for the optimization. In real-world
code, the problem becomes significantly easier to reproduce when running
with `-XX:+DelayAfterInliningCutoff` (currently disabled by default).

# Benchmark Summary

- [sparse-ea-slices/benchmark-summary.html](https://htmlpreview.github.io/?https://github.com/Spasi/jdk/blob/sparse-ea-slices/sparse-ea-slices/benchmark-summary.html) (HTML version)
- [sparse-ea-slices/benchmark-summary.md](sparse-ea-slices/benchmark-summary.md) (markdown version)

# Benchmark Summary (sneak peek)

Peak latency, log scale, shorter is better:

```text
B:default         [##################................]     287.784 us/op
B:NoEA            [#####################.............]     490.439 us/op
B:NoClip          [##################################]   7,581.102 us/op
B:NoClip+Delay    [#.................................]       8.065 us/op
P:default         [##################................]     281.214 us/op
P:NoEA            [#####################.............]     487.301 us/op
P:NoClip          [####..............................]      13.806 us/op
P:NoClip+Delay    [#.................................]       8.066 us/op
```

Time to near-peak, linear scale, shorter is better:

```text
B:default         [#######...........................] W6 
B:NoEA            [###...............................] W4 
B:NoClip          [##################################] W20
B:NoClip+Delay    [######################............] W14
P:default         [#.................................] W3 
P:NoEA            [###...............................] W4 
P:NoClip          [#.................................] W3 
P:NoClip+Delay    [###...............................] W4 
```

C2 speed, linear scale, longer is better:

```text
B:default         [#####.............................]   25,335.259 bytes/s
B:NoEA            [############################......]  172,957.027 bytes/s
B:NoClip          [#.................................]    3,494.572 bytes/s
B:NoClip+Delay    [##................................]    9,470.282 bytes/s
P:default         [##################################]  209,157.597 bytes/s
P:NoEA            [#############################.....]  175,814.158 bytes/s
P:NoClip          [##################................]  109,250.454 bytes/s
P:NoClip+Delay    [#############.....................]   77,247.426 bytes/s
```

# Benchmark results (raw)

- [JDK 27-ea+25 BASELINE](sparse-ea-slices/JDK%2027-ea%2B25%20BASELINE.txt)
- [JDK 27-ea+25 BASELINE -XX:-DoEscapeAnalysis](sparse-ea-slices/JDK%2027-ea%2B25%20BASELINE%20NoEA.txt)
- [JDK 27-ea+25 BASELINE -XX:-ClipInlining](sparse-ea-slices/JDK%2027-ea%2B25%20BASELINE%20NoClip.txt)
- [JDK 27-ea+25 BASELINE -XX:-ClipInlining -XX:+UnlockDiagnosticVMOptions -XX:+DelayAfterInliningCutoff](sparse-ea-slices/JDK%2027-ea%2B25%20BASELINE%20NoClipDelay.txt)

- [JDK 27-ea+25 PATCHED](sparse-ea-slices/JDK%2027-ea%2B25%20PATCHED.txt)
- [JDK 27-ea+25 PATCHED -XX:-DoEscapeAnalysis](sparse-ea-slices/JDK%2027-ea%2B25%20PATCHED%20NoEA.txt)
- [JDK 27-ea+25 PATCHED -XX:-ClipInlining](sparse-ea-slices/JDK%2027-ea%2B25%20PATCHED%20NoClip.txt)
- [JDK 27-ea+25 PATCHED -XX:-ClipInlining -XX:+UnlockDiagnosticVMOptions -XX:+DelayAfterInliningCutoff](sparse-ea-slices/JDK%2027-ea%2B25%20PATCHED%20NoClipDelay.txt)

# Patch description

- [Simple](sparse-ea-slices/README_simple.md) (for readers unfamiliar with HotSpot internals)
- [Advanced](sparse-ea-slices/README_advanced.md) (for Hotspot reviewers)

# Notes

- This patch has not been tested with the Valhalla `lworld` branch and will probably require adjustments to work with it.
- This work is a prototype that touches sensitive HotSpot code. Extensive testing/review/validation is required.
- This work is the product of Generative AI. It cannot be used as-is, as per https://openjdk.org/legal/ai.
  * The initial patch and performance improvement was developed with the help of GPT 5.5 over 2 days.
  * Then followed 2-3 weeks of iteration, testing and fixing issues, again with GPT 5.5.
  * Finally, Claude Fable 5 was used for another 2 days to review everything.
- A lot of testing, benchmarking and validation were done manually.