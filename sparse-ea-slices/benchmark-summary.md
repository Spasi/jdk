# EA Optimization Benchmark Summary - GitHub Markdown

Benchmark: `FFMStructAccessTest.copyReinterpretInline16`. This file uses only GitHub-safe Markdown: headings, tables, bullets, and fenced text charts. No embedded HTML, CSS, or SVG.

Latency is always `us/op`, C2 compile time is always seconds, and C2 speed is always `bytes/s`. TTP is the first iteration within 10% of that run's best observed latency.

Legend: `B:*` is baseline, `P:*` is patched. In ASCII charts, shorter bars are better for peak latency, TTP, and C2 time; longer bars are better for C2 speed.

## Scorecard

| Metric | Best | Worst |
|---|---:|---:|
| Peak latency | `B:NoClip+Delay` `8.065 us/op` at `M3` | `B:NoClip` `7,581.102 us/op` at `M1` |
| Time to near-peak | `W3` tie | `B:NoClip` `W20` |
| C2 compile time | `P:default` `1.058 s` | `B:NoClip` `36.794 s` |
| C2 speed | `P:default` `209,157.597 bytes/s` | `B:NoClip` `3,494.572 bytes/s` |

## Main Numbers

| Run | Peak us/op | Peak iter | TTP | Measured avg us/op | C2 s | C2 bytes/s |
|---|---:|---:|---:|---:|---:|---:|
| `B:default` | 287.784 | `W13` | `W6` | 288.210 | 8.495 | 25,335.259 |
| `B:NoEA` | 490.439 | `W4` | `W4` | 491.849 | 1.256 | 172,957.027 |
| `B:NoClip` | 7,581.102 **WORST** | `M1` | `W20` **WORST** | 7,595.440 | 36.794 **WORST** | 3,494.572 **WORST** |
| `B:NoClip+Delay` | 8.065 **BEST** | `M3` | `W14` | 8.070 | 23.230 | 9,470.282 |
| `P:default` | 281.214 | `M2` | `W3` **BEST** | 281.396 | 1.058 **BEST** | 209,157.597 **BEST** |
| `P:NoEA` | 487.301 | `M1` | `W4` | 490.084 | 1.266 | 175,814.158 |
| `P:NoClip` | 13.806 | `W8` | `W3` **BEST** | 13.817 | 2.091 | 109,250.454 |
| `P:NoClip+Delay` | 8.066 | `M3` | `W4` | 8.069 | 2.953 | 77,247.426 |

## Visual Bars

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

C2 compile time, log scale, shorter is better:

```text
B:default         [####################..............]    8.495 s
B:NoEA            [###...............................]    1.256 s
B:NoClip          [##################################]   36.794 s
B:NoClip+Delay    [##############################....]   23.230 s
P:default         [#.................................]    1.058 s
P:NoEA            [###...............................]    1.266 s
P:NoClip          [#######...........................]    2.091 s
P:NoClip+Delay    [###########.......................]    2.953 s
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

## Baseline To Patched Movement

`B` is the baseline point, `P` is the patched point, and `X` means overlap. For these two charts, movement left is better.

C2 compile time, log scale:

```text
default        P===========================B-------------------    8.495s ->   1.058s   8.0x faster
NoEA           --X---------------------------------------------    1.256s ->   1.266s   neutral
NoClip         ---------P=====================================B   36.794s ->   2.091s   17.6x faster
NoClip+Delay   --------------P==========================B------   23.230s ->   2.953s   7.9x faster
```

Time to near-peak, linear scale:

```text
default        P=======B---------------------------------------  W6  -> W3    2.0x sooner
NoEA           ---X--------------------------------------------  W4  -> W4    neutral
NoClip         P==============================================B  W20 -> W3    6.7x sooner
NoClip+Delay   ---P==========================B-----------------  W14 -> W4    3.5x sooner
```

## Baseline vs Patched

| Scenario | Baseline peak | Patched peak | Peak delta | Baseline TTP | Patched TTP | Baseline C2 | Patched C2 | C2 time | C2 speed |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| `default` | 287.784 | 281.214 | -2.3% | `W6` | `W3` | 8.495 | 1.058 | 8.0x faster | 8.3x faster |
| `NoEA` | 490.439 | 487.301 | -0.6% | `W4` | `W4` | 1.256 | 1.266 | neutral | neutral |
| `NoClip` | 7,581.102 | 13.806 | -99.8% | `W20` | `W3` | 36.794 | 2.091 | 17.6x faster | 31.3x faster |
| `NoClip+Delay` | 8.065 | 8.066 | +0.0% | `W14` | `W4` | 23.230 | 2.953 | 7.9x faster | 8.2x faster |

## Warmup Shapes

ASCII sparklines show `W1 W2 W3 W4 W5 peak`. Digits are log-scaled per row: `1` is lowest/best, `8` is highest/worst.

| Run | Warmup shape | TTP | Peak us/op | Measured avg us/op |
|---|---|---:|---:|---:|
| `B:default` | `888761  (W1 W2 W3 W4 W5 peak)` | `W6` | 287.784 | 288.210 |
| `B:NoEA` | `821111  (W1 W2 W3 W4 W5 peak)` | `W4` | 490.439 | 491.849 |
| `B:NoClip` | `888881  (W1 W2 W3 W4 W5 peak)` | `W20` | 7,581.102 | 7,595.440 |
| `B:NoClip+Delay` | `888881  (W1 W2 W3 W4 W5 peak)` | `W14` | 8.065 | 8.070 |
| `P:default` | `821111  (W1 W2 W3 W4 W5 peak)` | `W3` | 281.214 | 281.396 |
| `P:NoEA` | `821111  (W1 W2 W3 W4 W5 peak)` | `W4` | 487.301 | 490.084 |
| `P:NoClip` | `821111  (W1 W2 W3 W4 W5 peak)` | `W3` | 13.806 | 13.817 |
| `P:NoClip+Delay` | `881111  (W1 W2 W3 W4 W5 peak)` | `W4` | 8.066 | 8.069 |

## Latency / Compiler-Time Tradeoff

The SVG report has a scatter plot. This GitHub-safe approximation keeps the same idea as ranked coordinates. Lower peak rank and lower C2-time rank are better.

| Run | Peak rank | C2 time rank | Interpretation |
|---|---:|---:|---|
| `B:default` | 5 | 6 |  |
| `B:NoEA` | 7 | 2 |  |
| `B:NoClip` | 8 | 8 | pathological baseline |
| `B:NoClip+Delay` | 1 | 7 | best runtime peak |
| `P:default` | 4 | 1 | best compile time |
| `P:NoEA` | 6 | 3 |  |
| `P:NoClip` | 3 | 4 |  |
| `P:NoClip+Delay` | 2 | 5 |  |

## Run Legend

| ID | JDK | Scenario | Differing JVM arguments | Source file |
|---|---|---|---|---|
| `B:default` | baseline | `default` | `default EA/inlining policy` | `JDK 27-ea+25 BASELINE.txt` |
| `B:NoEA` | baseline | `NoEA` | `-XX:-DoEscapeAnalysis` | `JDK 27-ea+25 BASELINE NoEA.txt` |
| `B:NoClip` | baseline | `NoClip` | `-XX:-ClipInlining` | `JDK 27-ea+25 BASELINE NoClip.txt` |
| `B:NoClip+Delay` | baseline | `NoClip+Delay` | `-XX:-ClipInlining -XX:+DelayAfterInliningCutoff` | `JDK 27-ea+25 BASELINE NoClipDelay.txt` |
| `P:default` | patched | `default` | `default EA/inlining policy` | `JDK 27-ea+25 PATCHED.txt` |
| `P:NoEA` | patched | `NoEA` | `-XX:-DoEscapeAnalysis` | `JDK 27-ea+25 PATCHED NoEA.txt` |
| `P:NoClip` | patched | `NoClip` | `-XX:-ClipInlining` | `JDK 27-ea+25 PATCHED NoClip.txt` |
| `P:NoClip+Delay` | patched | `NoClip+Delay` | `-XX:-ClipInlining -XX:+DelayAfterInliningCutoff` | `JDK 27-ea+25 PATCHED NoClipDelay.txt` |

## Takeaways

- The patched build preserves the best observed peak within noise: `P:NoClip+Delay` reaches `8.066 us/op`, effectively tied with baseline `NoClip+Delay` at `8.065 us/op`.
- The patched build reaches peak much earlier in the important stress cases: `NoClip+Delay` moves from `W14` to `W4`; `NoClip` moves from `W20` to `W3`.
- The pathological baseline `NoClip` case is fixed: C2 time drops from `36.794 s` to `2.091 s`, and C2 speed improves from `3,494.572` to `109,250.454 bytes/s`.
- With EA disabled, both builds are effectively neutral, which is a useful control case.
