# DAA Assignment 4

Kuat Allazhar

Dynamic Programming and String Processing

Java 17

I use Java 17 for all eight assignment commands. Each command reads a JSON request and returns its result through the supplied JSON Lines adapter.

|Command|Implementation|
|---|---|
|`lcs`|Prefix DP and subsequence reconstruction|
|`merge`|Interval DP and a chronological adjacent-merge plan|
|`tree`|Iterative take/skip DP with selected vertices|
|`naive`|Direct character comparisons|
|`kmp`|Prefix function and overlapping matches|
|`rk`|Base 31/mod 101 rolling hash with exact verification|
|`suffix`|Counting-sort prefix doubling and Kasai LCP|
|`aho`|Failure-link automaton with aggregated counts|

Use Python 3.12+ and JDK 17+ on PATH. Run these commands from the repository root.

```text
python tools/run.py --setup
python tools/run.py --check
python tests/student/verify.py
```

Run an example with `python tools/run.py --input examples/lcs.jsonl`. `submission.json` selects Java. The launcher handles compilation and JSON input/output.

I check small inputs against independent reference answers and use larger cases with known results to test the input limits. I keep the inputs, expected and actual outputs, source hashes and measured times under `tests/student/results`. The recorded run passed all 44 public cases, 3,683 own small cases, 7,454 mixed requests and 22 large cases.

I explain the algorithms, correctness arguments, complexity and search comparison in [ANALYSIS.md](ANALYSIS.md). I describe the test generators in [tests/student/README.md](tests/student/README.md).

I started from the [official template](https://github.com/RZhaniya/daa-assignment-4-archive) and followed its [English specification](docs/Assignment_4_EN.md) and [contracts](docs/guide/CONTRACTS.md). My source references are listed in the analysis.
