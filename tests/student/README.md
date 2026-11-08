# My tests

Run `python tests/student/verify.py` from the repository root with Python 3.12+ and JDK 17+ on PATH. The runner builds Java, checks public cases, generates own cases with seed 20261108 and executes them through the supplied JSON Lines adapter.

I check small cases against reference answers obtained by enumerating subsequences, adjacent-merge sequences and independent vertex subsets. I sort short suffixes directly and count string/dictionary occurrences at every start. I use the public semantic validator to check witnesses against these independently computed optima. I also test that it accepts alternative optima and rejects invalid witnesses.

For large cases I use known answers for reversed/identical strings, equal blocks, chains, stars, repeated text and dictionary counts. I cover the published maximum input sizes and shuffle/repeat mixed requests in one process to check state reset.

`results/summary.json` records UTC timestamps, per-task counts, seed and source hashes. The `*-inputs`, `*-expected` and `*-actual` files retain requests and outputs. Large/mixed JSONL files use gzip. `setup.txt` and `public-checks.txt` contain the standalone launcher logs.

I compare naive, KMP and exact RK with `SearchBenchmark.java` on identical random and repetitive inputs at three sizes. I use 15 warm-up calls and five measured calls per algorithm/case and check every result using a built-in search oracle. I save raw milliseconds, medians and runtime metadata in `results/benchmark.json`. I explain the procedure and observations in `ANALYSIS.md`.
