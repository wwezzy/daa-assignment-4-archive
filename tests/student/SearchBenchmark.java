import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Run after compiling Java/source. Timings cover search and result allocation. */
public final class SearchBenchmark {
    private static List<Integer> search(int algorithm, String text, String pattern) {
        return switch (algorithm) {
            case 0 -> Algorithms.naivePositions(text, pattern);
            case 1 -> Algorithms.kmpPositions(text, pattern);
            case 2 -> Algorithms.rabinKarpPositions(text, pattern);
            default -> throw new IllegalArgumentException("Unknown algorithm");
        };
    }

    private static String randomText(Random random, int length) {
        StringBuilder text = new StringBuilder(length);
        for (int i = 0; i < length; i++) text.append("ABCD".charAt(random.nextInt(4)));
        return text.toString();
    }

    private static List<Integer> reference(String text, String pattern) {
        List<Integer> expected = new ArrayList<>();
        for (int start = text.indexOf(pattern); start >= 0; start = text.indexOf(pattern, start + 1)) {
            expected.add(start);
        }
        return expected;
    }

    public static void main(String[] args) {
        final int warmups = 15;
        final int trials = 5;
        String[] names = {"naive", "kmp", "rk"};
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int n : new int[]{500, 1500, 3000}) {
            int m = n / 5;
            long seed = 20261108L + n;
            Random random = new Random(seed);
            String generated = randomText(random, n);
            for (String kind : new String[]{"random", "repetitive"}) {
                String text = kind.equals("random") ? generated : "a".repeat(n);
                String pattern = kind.equals("random")
                        ? text.substring(n / 2, n / 2 + m) : "a".repeat(m);
                List<Integer> expected = reference(text, pattern);
                for (int round = 0; round < warmups; round++) {
                    for (int algorithm = 0; algorithm < 3; algorithm++) {
                        if (!search(algorithm, text, pattern).equals(expected)) {
                            throw new AssertionError("Warmup result mismatch");
                        }
                    }
                }
                List<List<Double>> times = List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
                for (int trial = 0; trial < trials; trial++) {
                    for (int offset = 0; offset < 3; offset++) {
                        int algorithm = (trial + offset) % 3;
                        long start = System.nanoTime();
                        List<Integer> actual = search(algorithm, text, pattern);
                        long elapsed = System.nanoTime() - start;
                        if (!actual.equals(expected)) throw new AssertionError("Measured result mismatch");
                        times.get(algorithm).add(elapsed / 1_000_000.0);
                    }
                }
                for (int algorithm = 0; algorithm < 3; algorithm++) {
                    List<Double> sorted = new ArrayList<>(times.get(algorithm));
                    Collections.sort(sorted);
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("case", kind);
                    row.put("n", n);
                    row.put("m", m);
                    row.put("seed", seed);
                    row.put("algorithm", names[algorithm]);
                    row.put("matches", expected.size());
                    row.put("times_ms", times.get(algorithm));
                    row.put("median_ms", sorted.get(trials / 2));
                    row.put("correct", true);
                    rows.add(row);
                }
            }
        }
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("java", System.getProperty("java.runtime.version"));
        output.put("vm", System.getProperty("java.vm.name"));
        output.put("os", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        output.put("processor", System.getenv().getOrDefault("PROCESSOR_IDENTIFIER", "unknown"));
        output.put("logical_processors", Runtime.getRuntime().availableProcessors());
        output.put("max_heap_bytes", Runtime.getRuntime().maxMemory());
        output.put("warmups_per_algorithm_case", warmups);
        output.put("trials", trials);
        output.put("timer", "System.nanoTime");
        output.put("rows", rows);
        System.out.println(Json.write(output));
    }
}
