import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Dynamic programming and exact string algorithms for independent requests. */
public final class Algorithms {
    /** Compare two versions of a sequence while preserving the order of shared characters. Implement lcs according to docs/guide/CONTRACTS.md. */
    static Map<String, Object> lcs(Map<String, Object> input) {
        String a = (String) input.get("a");
        String b = (String) input.get("b");
        int n = a.length();
        int m = b.length();

        // dp[i][j] is the LCS length for prefixes of lengths i and j.
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        int length = dp[n][m];
        StringBuilder sequence = new StringBuilder(length);
        int i = n;
        int j = m;
        while (i > 0 && j > 0) {
            char lastA = a.charAt(i - 1);
            if (lastA == b.charAt(j - 1)) {
                sequence.append(lastA);
                i--;
                j--;
            } else if (dp[i - 1][j] >= dp[i][j - 1]) {
                i--;
            } else {
                j--;
            }
        }

        // Backtracking visits the characters from last to first.
        return Map.of("length", length, "sequence", sequence.reverse().toString());
    }
    /** Interval DP, followed by child-first reconstruction of adjacent merges. */
    static Map<String, Object> merge(Map<String, Object> input) {
        List<?> sizes = (List<?>) input.get("sizes");
        int n = sizes.size();
        List<int[]> plan = new ArrayList<>();
        if (n == 0) return Map.of("cost", 0L, "plan", plan);

        long[] prefix = new long[n + 1];
        for (int i = 0; i < n; i++) {
            prefix[i + 1] = prefix[i] + ((Number) sizes.get(i)).longValue();
        }
        long[][] dp = new long[n][n];
        int[][] split = new int[n][n];
        for (int length = 2; length <= n; length++) {
            for (int left = 0; left + length <= n; left++) {
                int right = left + length - 1;
                long sum = prefix[right + 1] - prefix[left];
                dp[left][right] = Long.MAX_VALUE;
                for (int middle = left; middle < right; middle++) {
                    long candidate = dp[left][middle] + dp[middle + 1][right] + sum;
                    if (candidate < dp[left][right]) {
                        dp[left][right] = candidate;
                        split[left][right] = middle;
                    }
                }
            }
        }
        appendMerges(0, n - 1, split, plan);
        return Map.of("cost", dp[0][n - 1], "plan", plan);
    }

    private static void appendMerges(int left, int right, int[][] split, List<int[]> plan) {
        if (left == right) return;
        int middle = split[left][right];
        appendMerges(left, middle, split, plan);
        appendMerges(middle + 1, right, split, plan);
        plan.add(new int[]{left, middle, right});
    }

    /** Iterative parent order supports chains of 100,000 vertices. */
    static Map<String, Object> tree(Map<String, Object> input) {
        int n = ((Number) input.get("n")).intValue();
        if (n == 0) return Map.of("weight", 0L, "selected", List.of());
        List<?> weights = (List<?>) input.get("weights");
        List<?> edges = (List<?>) input.get("edges");
        int[] degree = new int[n];
        for (Object item : edges) {
            List<?> edge = (List<?>) item;
            degree[((Number) edge.get(0)).intValue()]++;
            degree[((Number) edge.get(1)).intValue()]++;
        }
        int[][] adjacent = new int[n][];
        for (int u = 0; u < n; u++) adjacent[u] = new int[degree[u]];
        Arrays.fill(degree, 0);
        for (Object item : edges) {
            List<?> edge = (List<?>) item;
            int u = ((Number) edge.get(0)).intValue();
            int v = ((Number) edge.get(1)).intValue();
            adjacent[u][degree[u]++] = v;
            adjacent[v][degree[v]++] = u;
        }

        int[] parent = new int[n];
        int[] order = new int[n];
        parent[0] = -1;
        int count = 1;
        for (int i = 0; i < count; i++) {
            int u = order[i];
            for (int v : adjacent[u]) {
                if (v == parent[u]) continue;
                parent[v] = u;
                order[count++] = v;
            }
        }

        long[] take = new long[n];
        long[] skip = new long[n];
        for (int i = n - 1; i >= 0; i--) {
            int u = order[i];
            take[u] = ((Number) weights.get(u)).longValue();
            for (int v : adjacent[u]) {
                if (parent[v] != u) continue;
                take[u] += skip[v];
                skip[u] += Math.max(take[v], skip[v]);
            }
        }

        boolean[] chosen = new boolean[n];
        List<Integer> selected = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int u = order[i];
            if ((parent[u] == -1 || !chosen[parent[u]]) && take[u] > skip[u]) {
                chosen[u] = true;
                selected.add(u);
            }
        }
        return Map.of("weight", Math.max(take[0], skip[0]), "selected", selected);
    }

    static Map<String, Object> naive(Map<String, Object> input) {
        return Map.of("positions", naivePositions((String) input.get("text"),
                (String) input.get("pattern")));
    }

    static List<Integer> naivePositions(String text, String pattern) {
        int n = text.length();
        int m = pattern.length();
        if (m == 0) return boundaries(n);
        List<Integer> positions = new ArrayList<>();
        for (int start = 0; start <= n - m; start++) {
            if (matchesAt(text, pattern, start)) positions.add(start);
        }
        return positions;
    }

    private static boolean matchesAt(String text, String pattern, int start) {
        for (int j = 0; j < pattern.length(); j++) {
            if (text.charAt(start + j) != pattern.charAt(j)) return false;
        }
        return true;
    }

    private static List<Integer> boundaries(int n) {
        List<Integer> positions = new ArrayList<>(n + 1);
        for (int i = 0; i <= n; i++) positions.add(i);
        return positions;
    }

    static Map<String, Object> kmp(Map<String, Object> input) {
        return Map.of("positions", kmpPositions((String) input.get("text"),
                (String) input.get("pattern")));
    }

    static List<Integer> kmpPositions(String text, String pattern) {
        int m = pattern.length();
        if (m == 0) return boundaries(text.length());
        int[] pi = new int[m];
        for (int i = 1; i < m; i++) {
            int matched = pi[i - 1];
            while (matched > 0 && pattern.charAt(i) != pattern.charAt(matched)) {
                matched = pi[matched - 1];
            }
            if (pattern.charAt(i) == pattern.charAt(matched)) matched++;
            pi[i] = matched;
        }
        List<Integer> positions = new ArrayList<>();
        int matched = 0;
        for (int i = 0; i < text.length(); i++) {
            while (matched > 0 && text.charAt(i) != pattern.charAt(matched)) {
                matched = pi[matched - 1];
            }
            if (text.charAt(i) == pattern.charAt(matched)) matched++;
            if (matched == m) {
                positions.add(i - m + 1);
                matched = pi[m - 1];
            }
        }
        return positions;
    }

    static Map<String, Object> rk(Map<String, Object> input) {
        return Map.of("positions", rabinKarpPositions((String) input.get("text"),
                (String) input.get("pattern")));
    }

    static List<Integer> rabinKarpPositions(String text, String pattern) {
        int n = text.length();
        int m = pattern.length();
        if (m == 0) return boundaries(n);
        List<Integer> positions = new ArrayList<>();
        if (m > n) return positions;
        final long base = 31;
        final long modulus = 101;
        long highestPower = 1;
        long patternHash = 0;
        long windowHash = 0;
        for (int j = 0; j < m; j++) {
            patternHash = (patternHash * base + pattern.charAt(j)) % modulus;
            windowHash = (windowHash * base + text.charAt(j)) % modulus;
            if (j > 0) highestPower = highestPower * base % modulus;
        }
        for (int start = 0; start <= n - m; start++) {
            if (patternHash == windowHash && matchesAt(text, pattern, start)) {
                positions.add(start);
            }
            if (start < n - m) {
                long removed = text.charAt(start) * highestPower % modulus;
                windowHash = (windowHash - removed + modulus) % modulus;
                windowHash = (windowHash * base + text.charAt(start + m)) % modulus;
            }
        }
        return positions;
    }

    /** Stable counting sorts rank pairs in each doubling round. */
    static Map<String, Object> suffix(Map<String, Object> input) {
        String text = (String) input.get("text");
        int n = text.length();
        if (n == 0) return Map.of("sa", new int[0], "lcp", new int[0], "repeated", "");
        int[] sa = new int[n];
        int[] temporary = new int[n];
        int[] rank = new int[n];
        int[] newRank = new int[n];
        for (int i = 0; i < n; i++) {
            sa[i] = i;
            rank[i] = text.charAt(i);
        }
        int classes = 128;
        int[] counts = new int[Math.max(n, classes) + 1];
        for (int width = 1; width < n; width *= 2) {
            sortByRank(sa, temporary, rank, width, classes, counts);
            sortByRank(temporary, sa, rank, 0, classes, counts);
            int nextClass = 0;
            newRank[sa[0]] = 0;
            for (int i = 1; i < n; i++) {
                int previous = sa[i - 1];
                int current = sa[i];
                int previousSecond = previous + width < n ? rank[previous + width] : -1;
                int currentSecond = current + width < n ? rank[current + width] : -1;
                if (rank[previous] != rank[current] || previousSecond != currentSecond) nextClass++;
                newRank[current] = nextClass;
            }
            int[] swap = rank;
            rank = newRank;
            newRank = swap;
            classes = nextClass + 1;
            if (classes == n) break;
        }

        int[] position = new int[n];
        for (int i = 0; i < n; i++) position[sa[i]] = i;
        int[] lcp = new int[n];
        int common = 0;
        int longest = 0;
        int repeatedStart = 0;
        for (int start = 0; start < n; start++) {
            int place = position[start];
            if (place == 0) {
                common = 0;
                continue;
            }
            int previous = sa[place - 1];
            while (start + common < n && previous + common < n
                    && text.charAt(start + common) == text.charAt(previous + common)) {
                common++;
            }
            lcp[place] = common;
            if (common > longest) {
                longest = common;
                repeatedStart = start;
            }
            if (common > 0) common--;
        }
        return Map.of("sa", sa, "lcp", lcp,
                "repeated", text.substring(repeatedStart, repeatedStart + longest));
    }

    private static void sortByRank(int[] source, int[] target, int[] rank,
                                   int offset, int classes, int[] counts) {
        Arrays.fill(counts, 0, classes + 1, 0);
        for (int start : source) {
            int key = start + offset < rank.length ? rank[start + offset] + 1 : 0;
            counts[key]++;
        }
        int position = 0;
        for (int key = 0; key <= classes; key++) {
            int frequency = counts[key];
            counts[key] = position;
            position += frequency;
        }
        for (int start : source) {
            int key = start + offset < rank.length ? rank[start + offset] + 1 : 0;
            target[counts[key]++] = start;
        }
    }

    /** Dense ASCII transitions, BFS failure links and aggregated state visits. */
    static Map<String, Object> aho(Map<String, Object> input) {
        List<?> patterns = (List<?>) input.get("patterns");
        List<?> texts = (List<?>) input.get("texts");
        int p = patterns.size();
        List<long[]> results = new ArrayList<>();
        if (p == 0 || texts.isEmpty()) {
            for (int i = 0; i < texts.size(); i++) results.add(new long[p]);
            return Map.of("counts", results);
        }
        List<int[]> transitions = new ArrayList<>();
        transitions.add(new int[95]);
        int[] terminal = new int[p];
        for (int i = 0; i < p; i++) {
            String pattern = (String) patterns.get(i);
            int state = 0;
            for (int j = 0; j < pattern.length(); j++) {
                int letter = pattern.charAt(j) - 32;
                int[] row = transitions.get(state);
                if (row[letter] == 0) {
                    row[letter] = transitions.size();
                    transitions.add(new int[95]);
                }
                state = row[letter];
            }
            terminal[i] = state;
        }

        int[][] next = transitions.toArray(new int[0][]);
        int states = next.length;
        int[] failure = new int[states];
        int[] queue = new int[states];
        int head = 0;
        int tail = 0;
        for (int letter = 0; letter < 95; letter++) {
            if (next[0][letter] != 0) queue[tail++] = next[0][letter];
        }
        while (head < tail) {
            int state = queue[head++];
            for (int letter = 0; letter < 95; letter++) {
                int child = next[state][letter];
                if (child != 0) {
                    failure[child] = next[failure[state]][letter];
                    queue[tail++] = child;
                } else {
                    next[state][letter] = next[failure[state]][letter];
                }
            }
        }

        for (Object value : texts) {
            String text = (String) value;
            long[] visits = new long[states];
            int state = 0;
            for (int i = 0; i < text.length(); i++) {
                state = next[state][text.charAt(i) - 32];
                visits[state]++;
            }
            for (int i = tail - 1; i >= 0; i--) {
                int child = queue[i];
                visits[failure[child]] += visits[child];
            }
            long[] counts = new long[p];
            for (int i = 0; i < p; i++) counts[i] = visits[terminal[i]];
            results.add(counts);
        }
        return Map.of("counts", results);
    }
}
