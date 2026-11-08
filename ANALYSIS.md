# Assignment 4 - Analysis

Kuat Allazhar

Design and Analysis of Algorithms

Java 17

## Environment and commands

I use the supplied JSON Lines adapter and create fresh algorithm state for each request. I work with the published printable ASCII alphabet and zero-based indices. I store merge costs, tree totals and dictionary counts in `long`.

My recorded run is dated 8 November 2026. I used Microsoft OpenJDK 17.0.20.1+1-LTS, Python 3.12.10 and Windows 11 amd64. My machine has an Intel Core i9-13900H, 20 logical processors and 16,780,570,624 bytes of RAM. I used a 384 MiB maximum Java heap for the launcher and benchmark.

I ran the following commands from the repository root with JDK 17 on PATH.

```text
python tools/run.py --setup
python tools/run.py --check
python tests/student/verify.py
```

On my Windows installation I selected the JDK for the current PowerShell session before running the checks.

```powershell
$env:JAVA_HOME = 'C:\Users\alnbu\.jdks\ms-17.0.20.1'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

The launcher compiles `Java/source/*.java` with `javac --release 17`. My test runner uses the same build function, then compiles `tests/student/SearchBenchmark.java` against `Java/build` and runs `java -Xmx384m -cp Java/build SearchBenchmark`. An example request can be run with `python tools/run.py --input examples/lcs.jsonl`.

## Algorithm explanations

### LCS

For lengths n and m, `dp[i][j]` stores the longest common subsequence length of the first i characters of a and first j characters of b. The zero row and column are zero. Equal last characters extend the diagonal answer by one. Otherwise, I take the maximum after dropping the last character from either prefix. Increasing row and column indices make all dependencies available.

Appending a matching last character to an optimal diagonal subsequence gives a feasible answer. Removing the final character from any common answer leaves a sequence fitting both shorter prefixes, so diagonal + 1 is also an upper bound when the last characters match. When they differ, a common alignment cannot use both final positions. Dropping at least one covers every optimum. Induction over prefix lengths establishes the table.

I reconstruct the sequence from `(n,m)`. On a match I append the character and move diagonally. On a mismatch I move to a neighbour retaining the optimum, preferring the row above on ties. I reverse the collected characters at the end. Either `A` or `B` is valid for `AB` / `BA`. Empty input gives length 0 and an empty sequence.

Time and auxiliary memory are O((n+1)(m+1)), including boundaries. Reconstruction takes O(n+m) time and O(length+1) output/builder space. The reversed-block case at lengths 1000 and 1000 has optimum 500. Gap and tie cases check the witness as well as its length.

### Ordered adjacent-block merging

`dp[left][right]` is the minimum cost to merge that closed interval into one block. A singleton costs zero. For every possible final split k, the candidate is `dp[left][k] + dp[k+1][right] + sum(left..right)`. Prefix sums provide the interval sum in constant time. I process intervals by increasing length and retain the first optimal split.

Every legal final merge joins two adjacent intervals partitioning the target interval. Its split occurs among the candidates. If either child plan were suboptimal, replacing it by a cheaper plan would improve the whole plan. Induction on interval length proves the recurrence.

I reconstruct the left child's plan, the right child's plan and then append `[left,k,right]`. Both child blocks exist when I append their parent merge. For n>0 there are n-1 operations. Empty and single-block inputs have cost zero and an empty plan. I use recursion for this reconstruction, where n<=200.

Time is O(n^3), auxiliary memory O(n^2), and plan output O(n). Empty input takes O(1). `[1,100,1]` checks adjacency. The 200 equal billion-sized blocks have cost 1,544,000,000,000, requiring `long`. Zero blocks exercise tied optimal splits.

### Maximum-weight independent set on a tree

I root the tree at vertex 0 and build a parent-before-child order using an array. Processing it backwards makes all child states available. `take[u]` includes u and adds `skip[v]` for each child v. `skip[u]` excludes u and adds `max(take[v],skip[v])`. Leaves start with their own weight for take and zero for skip.

Selecting u forces every child to be skipped. Skipping u permits each child subtree to choose its better state independently, because different child subtrees have no connecting edges. These choices cover all independent sets. Induction from leaves proves the states and `max(take[0],skip[0])` is the optimum. Skipping all vertices permits an optimum of zero when all weights are negative.

I reconstruct the selected set in parent order. I choose a vertex only if its parent was skipped and take strictly exceeds skip. I skip it on a tie. A selected parent forces its child to use skip. Each vertex occurs once in my traversal. For the empty tree I return zero and an empty set.

Adjacency, ordering, DP and reconstruction take O(n+1) time and auxiliary memory, with O(n+1) output space. Arrays support 100,000-vertex chains without recursive traversal. A positive 100,000-vertex star has optimum 99,999,000,000,000. Small random trees permute vertex labels and compare against exhaustive subsets to check root independence.

### Naive exact search

I try every start from 0 through n-m. A comparison loop maintains that all earlier pattern characters match at that start. A position is reported only after all m characters match. Trying every start includes overlaps and the final valid window, proving completeness and soundness.

The empty pattern returns all n+1 boundaries. A longer pattern produces no candidate starts. Comparisons preserve case, spaces and punctuation. Time is O(n*m+n+1), auxiliary memory O(1), and output memory O(z+1). For `aaaaa` / `aaa`, starts 0,1,2 must all be returned.

### KMP

`pi[i]` is the longest proper prefix of the pattern that is also a suffix of its prefix ending at i. During prefix construction, a mismatch follows `pi[matched-1]` until a shorter border can be extended or becomes empty. Increasing i ensures that fallback entries are available.

During scanning, matched is the length of the pattern prefix matching a suffix of the text processed so far. Every shorter feasible match is a border of that prefix, so following pi preserves all possible occurrences. A full match reports its start and falls back to `pi[m-1]`, retaining overlaps. Empty patterns are handled before indexing the pattern.

Each scan position advances once. Fallback decreases matched, while extensions increase it at most once per scanned character. Prefix construction and scanning take O(n+m+z+1) time. Auxiliary memory is O(m+1), output memory O(z+1). The 200,000-character overlap and long near-match tests exercise the full domain and repeated fallback.

### Exact Rabin-Karp

The polynomial hash uses base 31, modulus 101 and ASCII codes, with the highest power at the left. I compute the pattern and initial window hashes. Each rolling update removes the outgoing character, multiplies by 31 and adds the incoming character. Adding the modulus after subtraction normalizes negative remainders. Induction over starts proves the rolling value equals the window's polynomial hash.

Unequal hashes reject a window. Equal hashes trigger character verification, which ensures exact results despite collisions. In `abdj` / `ab`, both `ab` and `dj` hash to 75, but only start 0 is reported. The same example causes a negative intermediate rolling remainder. Empty patterns and longer patterns are handled before window construction.

For 1<=m<=n, time is O(n+m+c*m), with c equal-hash candidates, including true matches. A bound covering all inputs is O(n*m+n+m+1). Auxiliary memory is O(1) and output memory O(z+1). The deliberately small modulus makes collisions testable and can increase verification work.

### Suffix array and Kasai LCP

Initial ranks describe one-character prefixes with ASCII codes. At width k, a suffix is represented by `(rank[start],rank[start+k])`. A missing second half ranks below every character. I stably counting-sort the second key and then the first key. Equal pairs receive the same new rank. Each round doubles the represented prefix length, stopping early if all ranks differ.

Two doubled prefixes compare exactly as their two ranked halves compare lexicographically. Stable radix sorting therefore preserves the invariant that ranks describe lexicographic prefix order. Once represented prefixes cover every suffix, the resulting permutation is suffix order. Sorting uses indices and ranks without materializing suffix strings. Exactly the n nonempty suffixes are returned.

Kasai builds the inverse suffix array and compares each suffix with its sorted predecessor. Advancing one text position retains a common-prefix lower bound reduced by at most one. The retained bound avoids repeated comparisons. The first sorted suffix has LCP zero. Total extensions and reductions are linear, establishing O(n) time.

All suffixes beginning with a fixed substring form a contiguous lexicographic interval. Adjacent suffixes in that interval share the substring too. Consequently the largest adjacent LCP gives the longest repeated substring. I return one maximum found during Kasai's scan. Overlaps are allowed. Empty input returns empty arrays and an empty repeated string.

Each radix round takes O(n+128) time, with O(log(n+1)) rounds, giving O(n log(n+1)) time for nonempty text. Kasai and extracting the repeated string take O(n). Auxiliary and output memory are O(n+1). The equal-character and periodic tests both have 50,000 characters and exact independently known sa/lcp arrays.

### Aho-Corasick

I give each trie state a 95-entry ASCII transition row. I let duplicate dictionary entries share a terminal state and keep their original positions in the terminal array. I allocate the failure array and BFS queue after the actual number of states is known.

BFS processes increasing trie depth. A failure link represents the longest proper suffix that is a trie prefix. Missing transitions inherit the already completed transition of the failure state. After consuming a text character, the automaton state therefore represents the longest trie-prefix suffix of the processed text.

I start each text at root with a fresh visits array and increment the reached state after every character. I then propagate visits to failure parents in reverse BFS order. A terminal pattern matches whenever it is a suffix of the current state's represented prefix. This propagation accumulates exactly those visits, covering suffix patterns and overlaps. I read terminal states in original dictionary order to return duplicate counts in separate columns.

Empty dictionaries yield one empty row per text, empty texts yield zeros and an empty text list yields no rows. State/count arrays are local to each request or text. Counts are aggregated without copying inherited output lists or emitting individual occurrence records.

Let L be total pattern length, S actual trie states, p dictionary size and q text count. Construction takes O(L+95*S+p) time and O(95*S+p) auxiliary memory. Each text takes O(|text|+S+p) time with O(S+p) working/count-row memory. Retained output uses O(q*(p+1)+1) memory. Tests include a depth-100,000 trie, dense suffix matches, 10,000 duplicate entries, a million output counts and many independent texts against a large trie.

## Own tests and recorded results

I generate my own cases with `tests/student/verify.py` and seed 20261108. For small reference answers I enumerate subsequences, complete adjacent-merge sequences and vertex subsets. I sort short suffix strings directly and compare their LCPs character by character. For search and dictionary cases I check every possible start.

I use the supplied semantic validator to check schemas and witnesses against these independent optima. It replays merge plans chronologically, checks distinct nonadjacent selected vertices and their weight sum, and checks that LCS witnesses fit both strings. It checks suffix sa/lcp arrays together with the repeated substring. I include eight additional checks that accept alternative optimal witnesses and reject malformed witnesses.

`tests/student/results/summary.json` contains UTC timestamps, task counts, seed and source SHA-256 hashes. Input, expected and actual JSONL records are retained alongside it, with gzip for large/repeated batches. Standalone launcher logs are saved too.

|Check|Actual result|
|---|---|
|Setup ping|Passed|
|Public launcher|44 cases and repeated mixed batch passed|
|Own small cases|3,683 passed|
|Small task breakdown|968 LCS, 368 merge, 187 tree, 571 each naive/KMP/RK, 282 suffix, 165 Aho|
|Shuffled repeated process|7,454 requests passed in 0.205 s|
|Large-domain batch|22 cases passed in 0.962 s|
|Witness-validator checks|8 passed|
|Search comparison|18 rows with every result checked|

Batch elapsed times include launching Java and JSON transport. Benchmark intervals below use a separate scope.

|Algorithm|Fixture or generator|Expected property and defect checked|
|---|---|---|
|LCS|`lcs.json`, binary words, reversed/identical maximum strings|Correct sequence, empty boundaries, gaps, repeats and ties|
|merge|All size vectors over 0,1,3 up to length 5, `[1,100,1]`|Adjacent-only optimum and chronological valid plan|
|merge|200 equal billion-sized blocks and 200 zeros|Long cost 1,544,000,000,000 and zero-cost ties|
|tree|Random labelled trees and exhaustive subsets|Exact optimum, root independence, no adjacent/duplicate selected vertices|
|tree|100,000-vertex positive/negative chains and star|Stack safety, empty negative solution and 64-bit sums|
|searches|Binary/ASCII inputs, `abdj` / `ab`|Overlaps, empty pattern, case, spaces, collisions and rolling normalization|
|KMP|200,000 text / 100,000 pattern|All overlapping matches and efficient long fallback|
|suffix|Direct small oracle and 50,000 equal/periodic characters|Exact order, LCP, overlap and rank sorting at scale|
|Aho|Duplicates, suffix patterns and repeated texts|Failure propagation and independent text state|
|Aho|Deep trie, dense suffixes, million count output|Aggregation at full input/output limits|

For equal merge blocks, an optimal binary merge tree has leaf depths d=floor(log2 n) and d+1. Total depth is n*d+2*(n-2^d), giving the stated cost at n=200. A positive equal-weight chain selects ceil(n/2) vertices and a star selects all leaves. Equal-character suffixes sort from shortest to longest and have adjacent LCP 0,1,...,n-1. These formulas supply large expected answers without reusing the submitted algorithms.

## Search comparison

I give all three searches identical strings for each case. I use sizes `(n,m)=(500,100),(1500,300),(3000,600)`, within their common domain. For random cases I use `java.util.Random(20261108+n)` to generate n letters independently from `ABCD` and take the m-character pattern starting at n/2, guaranteeing a match. For repetitive cases I use n copies of `a` and m copies of `a`, with n-m+1 matches.

I check every result against Java `String.indexOf`, advancing starts by one to retain overlaps. I use this built-in function in the benchmark oracle. I run 15 warm-up calls and 5 measured calls per algorithm/case and rotate the measured algorithm order by trial. I time the core search with `System.nanoTime`, including prefix/hash preprocessing and result allocation. Compilation, generation, JSON transport, validation and printing are outside the interval. GC pauses inside a call remain included. I use the same JVM and `-Xmx384m` setting throughout. JIT compilation may continue after warm-up.

The raw milliseconds and medians below come from `tests/student/results/benchmark.json`.

|Case|n|m|Seed|Algorithm|Run 1|Run 2|Run 3|Run 4|Run 5|Median (ms)|
|---|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|
|random|500|100|20261608|naive|0.0142|0.0134|0.0124|0.0124|0.0149|0.0134|
|random|500|100|20261608|kmp|0.0365|0.0359|0.0341|0.0354|0.0355|0.0355|
|random|500|100|20261608|rk|0.0277|0.0284|0.0262|0.0273|0.0274|0.0274|
|repetitive|500|100|20261608|naive|0.0807|0.0821|0.0715|0.0734|0.0719|0.0734|
|repetitive|500|100|20261608|kmp|0.0591|0.0605|0.0577|0.0559|0.0531|0.0577|
|repetitive|500|100|20261608|rk|0.1071|0.1044|0.1022|0.0970|0.0950|0.1022|
|random|1500|300|20262608|naive|0.0247|0.0266|0.0266|0.0269|0.0257|0.0266|
|random|1500|300|20262608|kmp|0.1017|0.1135|0.1132|0.1112|0.1108|0.1112|
|random|1500|300|20262608|rk|0.0866|0.0852|0.0855|0.0869|0.0838|0.0855|
|repetitive|1500|300|20262608|naive|0.5630|0.5850|0.5691|0.5698|0.5464|0.5691|
|repetitive|1500|300|20262608|kmp|0.0414|0.0364|0.0430|0.0489|0.0422|0.0422|
|repetitive|1500|300|20262608|rk|0.5518|0.5287|0.5675|0.5651|0.5668|0.5651|
|random|3000|600|20264108|naive|0.0126|0.0122|0.0121|0.0172|0.0119|0.0122|
|random|3000|600|20264108|kmp|0.0453|0.0453|0.0451|0.0456|0.0448|0.0453|
|random|3000|600|20264108|rk|0.0479|0.0345|0.0345|0.0347|0.0347|0.0347|
|repetitive|3000|600|20264108|naive|1.8069|1.6441|1.6243|1.2501|1.2419|1.6243|
|repetitive|3000|600|20264108|kmp|0.0262|0.0232|0.0284|0.0305|0.0204|0.0262|
|repetitive|3000|600|20264108|rk|3.1173|2.5865|2.6335|2.6035|2.5161|2.6035|

Naive search has the lowest medians on these random cases because most windows fail early. On the largest repetitive case KMP is much faster, while naive and exact RK repeatedly compare long matching windows. RK verifies every equal-hash candidate. Modulus 101 can add collision checks on other inputs. Small intervals and continuing JIT compilation help explain irregular scaling, including a larger random case having a lower median. These measurements describe this recorded run and do not prove asymptotic bounds or a universal ranking.

## Sources and AI assistance

- [Official Assignment 4 template and specification](https://github.com/RZhaniya/daa-assignment-4-archive).
- [Interactive lesson](https://rzhaniya.github.io/daa-assignment-4-archive/) and the provided Week 8 materials.
- I used OpenAI Codex to help me understand the main ideas behind the algorithms.
