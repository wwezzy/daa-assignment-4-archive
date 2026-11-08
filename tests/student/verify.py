"""Reproducible checks with small independent oracles and known large answers.

Run from any directory with Python 3.12+ and JDK 17+ on PATH.
"""
from collections import Counter
from datetime import datetime, timezone
from functools import lru_cache
import gzip
import hashlib
import itertools
import json
import os
from pathlib import Path
import random
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'tests/student/results'
sys.path.insert(0, str(ROOT / 'tools'))
from run import build, execute
from public_validate import check

SEED = 20261108


def occurrences(text, pattern):
    return [i for i in range(len(text) + 1) if text.startswith(pattern, i)]


@lru_cache(None)
def subsequences(text):
    return {''.join(chars) for count in range(len(text) + 1)
            for chars in itertools.combinations(text, count)}


@lru_cache(None)
def merge_oracle(sizes):
    if len(sizes) <= 1:
        return 0
    return min(sizes[i] + sizes[i + 1] + merge_oracle(
        sizes[:i] + (sizes[i] + sizes[i + 1],) + sizes[i + 2:])
        for i in range(len(sizes) - 1))


def expected(request):
    task = request['task']
    if task == 'lcs':
        common = subsequences(request['a']) & subsequences(request['b'])
        sequence = max(common, key=lambda s: (len(s), s))
        return {'length': len(sequence), 'sequence': sequence}
    if task == 'merge':
        return {'cost': merge_oracle(tuple(request['sizes']))}
    if task == 'tree':
        n = request['n']
        best = 0
        for mask in range(1 << n):
            if any(mask >> u & 1 and mask >> v & 1 for u, v in request['edges']):
                continue
            best = max(best, sum(w for u, w in enumerate(request['weights']) if mask >> u & 1))
        return {'weight': best}
    if task in ('naive', 'kmp', 'rk'):
        return {'positions': occurrences(request['text'], request['pattern'])}
    if task == 'suffix':
        text = request['text']
        sa = sorted(range(len(text)), key=lambda i: text[i:])
        lcp = [0] * len(text)
        for i in range(1, len(text)):
            left, right = sa[i - 1], sa[i]
            while left + lcp[i] < len(text) and right + lcp[i] < len(text):
                if text[left + lcp[i]] != text[right + lcp[i]]:
                    break
                lcp[i] += 1
        longest = max(lcp, default=0)
        start = sa[lcp.index(longest)] if longest else 0
        return {'sa': sa, 'lcp': lcp, 'repeated': text[start:start + longest]}
    if task == 'aho':
        return {'counts': [[len(occurrences(text, pattern)) for pattern in request['patterns']]
                           for text in request['texts']]}
    raise ValueError(task)


def save_lines(name, rows, compressed=False):
    path = OUT / name
    opener = gzip.open if compressed else open
    with opener(path, 'wt', encoding='utf-8', newline='\n') as stream:
        for row in rows:
            stream.write(json.dumps(row, separators=(',', ':')) + '\n')


def validate_group(command, cases, name, compressed=False):
    requests = [case['input'] for case in cases]
    start = time.perf_counter()
    answers = execute(command, requests)
    elapsed = time.perf_counter() - start
    for index, (case, answer) in enumerate(zip(cases, answers)):
        if not check(case['input'], answer, case['output']):
            raise AssertionError(f'{name} case {index}, {case.get("name")}: {answer}')
    suffix = '.jsonl.gz' if compressed else '.jsonl'
    save_lines(name + '-inputs' + suffix, requests, compressed)
    save_lines(name + '-expected' + suffix, [case['output'] for case in cases], compressed)
    save_lines(name + '-actual' + suffix, answers, compressed)
    counts = dict(Counter(request['task'] for request in requests))
    print(f'PASS {name}: {len(cases)} requests ({elapsed:.3f} s)', flush=True)
    return {'requests': len(cases), 'tasks': counts, 'seconds': elapsed}


def small_cases():
    cases = json.loads((ROOT / 'tests/student/lcs.json').read_text(encoding='utf-8'))

    def add(request, reason):
        cases.append({'input': request, 'output': expected(request), 'name': reason})

    words = [''.join(chars) for n in range(5) for chars in itertools.product('AB', repeat=n)]
    for a in words:
        for b in words:
            add({'task': 'lcs', 'a': a, 'b': b}, 'exhaustive-binary-lcs')
    for n in range(6):
        for sizes in itertools.product((0, 1, 3), repeat=n):
            add({'task': 'merge', 'sizes': list(sizes)}, 'enumerate-adjacent-merge-sequences')
    for sizes in ([1, 100, 1], [10**9] * 8, [2, 3, 4], [0] * 8):
        add({'task': 'merge', 'sizes': sizes}, 'merge-adjacency-zero-and-long-cost')
    rng = random.Random(SEED)
    for _ in range(180):
        n = rng.randrange(12)
        edges = [[i, rng.randrange(i)] for i in range(1, n)]
        permutation = list(range(n))
        rng.shuffle(permutation)
        edges = [[permutation[u], permutation[v]] for u, v in edges]
        add({'task': 'tree', 'n': n, 'weights': [rng.randrange(-10, 15) for _ in range(n)],
             'edges': edges}, 'subset-oracle-and-random-root')
    for weights in ([], [-5], [0], [9], [-5] * 10, [0] * 10, [10**9] * 9):
        add({'task': 'tree', 'n': len(weights), 'weights': weights,
             'edges': [[i - 1, i] for i in range(1, len(weights))]}, 'tree-boundary-and-long-sum')
    patterns = [''.join(chars) for n in range(4) for chars in itertools.product('AB', repeat=n)]
    for text in words:
        for pattern in patterns:
            for task in ('naive', 'kmp', 'rk'):
                add({'task': task, 'text': text, 'pattern': pattern}, 'exhaustive-search-including-overlap')
    alphabet = ''.join(chr(c) for c in range(32, 127))
    for _ in range(100):
        text = ''.join(rng.choices(alphabet, k=rng.randrange(80)))
        pattern = ''.join(rng.choices(alphabet, k=rng.randrange(10)))
        if rng.randrange(2) and text:
            start = rng.randrange(len(text))
            pattern = text[start:start + rng.randrange(10)]
        for task in ('naive', 'kmp', 'rk'):
            add({'task': task, 'text': text, 'pattern': pattern}, 'all-printable-ascii-search')
    for task in ('naive', 'kmp', 'rk'):
        for text, pattern in [('abdj', 'ab'), ('dj', 'ab'), ('~ ! ab ~', 'ab'),
                              ('aaaaa', 'aaa'), (' A A ', ' A '), ('A', 'a')]:
            add({'task': task, 'text': text, 'pattern': pattern}, 'collision-normalization-case-and-spaces')
    suffix_words = [''.join(chars) for n in range(7) for chars in itertools.product('ab', repeat=n)]
    for text in suffix_words + ['banana', 'abcdef', 'ababcdcd', alphabet, alphabet[::-1]]:
        add({'task': 'suffix', 'text': text}, 'direct-sort-and-lcp-oracle')
    for _ in range(150):
        add({'task': 'suffix', 'text': ''.join(rng.choices(alphabet, k=rng.randrange(100)))},
            'ascii-suffix-oracle')
    for patterns, texts in [([], ['', 'abc']), (['a'], []), (['he', 'she', 'he'], ['sheshe', '', 'sheshe']),
                            (['a', 'aa', 'aaa', 'a'], ['aaaa', 'baaa', 'aaaa']),
                            ([' ', '~', ' A', 'A'], ['~ A A', '', '~ A A'])]:
        add({'task': 'aho', 'patterns': patterns, 'texts': texts}, 'aho-boundary-duplicates-failure-reset')
    for _ in range(160):
        dictionary = [''.join(rng.choices('ab ~', k=rng.randrange(1, 8))) for _ in range(rng.randrange(12))]
        if dictionary:
            dictionary += [dictionary[0]]
        texts = [''.join(rng.choices('ab ~', k=rng.randrange(50))) for _ in range(rng.randrange(6))]
        add({'task': 'aho', 'patterns': dictionary, 'texts': texts}, 'dictionary-direct-count-oracle')
    return cases


def scale_cases():
    cases = []

    def add(request, output, name):
        cases.append({'input': request, 'output': output, 'name': name})

    add({'task': 'lcs', 'a': 'A' * 500 + 'B' * 500, 'b': 'B' * 500 + 'A' * 500},
        {'length': 500, 'sequence': 'A' * 500}, 'lcs-reversed-maximum-blocks')
    add({'task': 'lcs', 'a': 'ab' * 500, 'b': 'ab' * 500},
        {'length': 1000, 'sequence': 'ab' * 500}, 'lcs-identical-limit')
    add({'task': 'lcs', 'a': '', 'b': 'A' * 1000}, {'length': 0, 'sequence': ''}, 'lcs-empty-limit')
    # Equal blocks have an optimal balanced merge tree with depths floor/ceil(log2 n).
    n = 200
    depth = n.bit_length() - 1
    merge_cost = (n * depth + 2 * (n - 2**depth)) * 10**9
    add({'task': 'merge', 'sizes': [10**9] * n}, {'cost': merge_cost}, 'merge-full-limit-long-cost')
    add({'task': 'merge', 'sizes': [0] * n}, {'cost': 0}, 'merge-full-limit-ties')
    n = 100000
    chain = [[i - 1, i] for i in range(1, n)]
    add({'task': 'tree', 'n': n, 'weights': [10**9] * n, 'edges': chain},
        {'weight': n // 2 * 10**9}, 'tree-deep-chain')
    add({'task': 'tree', 'n': n, 'weights': [-10**9] * n, 'edges': chain},
        {'weight': 0}, 'tree-negative-deep-chain')
    star = [[0, i] for i in range(1, n)]
    add({'task': 'tree', 'n': n, 'weights': [10**9] * n, 'edges': star},
        {'weight': (n - 1) * 10**9}, 'tree-wide-star-long-sum')
    for task, n, m in [('naive', 3000, 1500), ('kmp', 200000, 100000), ('rk', 10000, 2000)]:
        add({'task': task, 'text': 'a' * n, 'pattern': 'a' * m},
            {'positions': list(range(n - m + 1))}, task + '-dense-limit')
        add({'task': task, 'text': 'a' * n, 'pattern': ''},
            {'positions': list(range(n + 1))}, task + '-empty-pattern-limit')
    add({'task': 'rk', 'text': 'a' * 1000, 'pattern': 'b' * 10000},
        {'positions': []}, 'rk-long-pattern-limit')
    add({'task': 'kmp', 'text': 'a' * 199999 + 'b', 'pattern': 'a' * 99999 + 'b'},
        {'positions': [100000]}, 'kmp-long-fallback-limit')
    n = 50000
    add({'task': 'suffix', 'text': 'a' * n},
        {'sa': list(range(n - 1, -1, -1)), 'lcp': list(range(n)), 'repeated': 'a' * (n - 1)},
        'suffix-equal-limit')
    sa = list(range(n - 2, -1, -2)) + list(range(n - 1, 0, -2))
    lcp = [0] + list(range(2, n, 2)) + [0] + list(range(1, n - 1, 2))
    add({'task': 'suffix', 'text': 'ab' * (n // 2)},
        {'sa': sa, 'lcp': lcp, 'repeated': ('ab' * (n // 2))[:-2]}, 'suffix-periodic-limit')
    add({'task': 'aho', 'patterns': ['a'] * 10000, 'texts': ['a' * 2000] * 100},
        {'counts': [[2000] * 10000 for _ in range(100)]}, 'aho-duplicates-million-counts')
    add({'task': 'aho', 'patterns': ['a' * 100000], 'texts': ['a' * 200000]},
        {'counts': [[100001]]}, 'aho-deep-trie-limit')
    dictionary = ['a' * i for i in range(1, 447)]
    add({'task': 'aho', 'patterns': dictionary, 'texts': ['a' * 200000]},
        {'counts': [[200001 - len(pattern) for pattern in dictionary]]}, 'aho-dense-suffix-matches')
    rng = random.Random(SEED)
    dictionary = [''.join(rng.choices('abcdef', k=100)) for _ in range(1000)]
    add({'task': 'aho', 'patterns': dictionary, 'texts': ['~' * 2000] * 100},
        {'counts': [[0] * 1000 for _ in range(100)]}, 'aho-many-states-and-independent-texts')
    return cases


def witness_checks():
    checks = [
        ({'task': 'lcs', 'a': 'AB', 'b': 'BA'}, {'length': 1, 'sequence': 'A'}, {'length': 1}, True),
        ({'task': 'lcs', 'a': 'AB', 'b': 'BA'}, {'length': 1, 'sequence': 'B'}, {'length': 1}, True),
        ({'task': 'lcs', 'a': 'AB', 'b': 'BA'}, {'length': 1, 'sequence': 'Z'}, {'length': 1}, False),
        ({'task': 'merge', 'sizes': [1, 1, 1]}, {'cost': 5, 'plan': [[1, 1, 2], [0, 0, 2]]}, {'cost': 5}, True),
        ({'task': 'merge', 'sizes': [1, 1, 1]}, {'cost': 5, 'plan': [[0, 0, 2], [1, 1, 2]]}, {'cost': 5}, False),
        ({'task': 'tree', 'n': 2, 'weights': [1, 1], 'edges': [[0, 1]]}, {'weight': 1, 'selected': [0]}, {'weight': 1}, True),
        ({'task': 'tree', 'n': 2, 'weights': [1, 1], 'edges': [[0, 1]]}, {'weight': 1, 'selected': [1]}, {'weight': 1}, True),
        ({'task': 'tree', 'n': 2, 'weights': [1, 1], 'edges': [[0, 1]]}, {'weight': 2, 'selected': [0, 1]}, {'weight': 1}, False),
    ]
    for request, answer, optimum, accepted in checks:
        assert check(request, answer, optimum) == accepted
    return len(checks)


def main():
    os.chdir(ROOT)
    OUT.mkdir(exist_ok=True)
    started = datetime.now(timezone.utc).isoformat()
    command = build('Java')
    public = json.loads((ROOT / 'tests/public.json').read_text(encoding='utf-8'))
    summary = {'seed': SEED, 'started_utc': started}
    summary['public'] = validate_group(command, public, 'public')
    own = small_cases()
    summary['small'] = validate_group(command, own, 'small')
    rng = random.Random(SEED)
    mixed = public + own
    rng.shuffle(mixed)
    summary['mixed'] = validate_group(command, mixed + mixed, 'mixed', True)
    summary['scale'] = validate_group(command, scale_cases(), 'scale', True)
    summary['witness_validator_checks'] = witness_checks()
    subprocess.run(['javac', '--release', '17', '-encoding', 'UTF-8', '-cp', str(ROOT / 'Java/build'),
                    '-d', str(ROOT / 'Java/build'), str(ROOT / 'tests/student/SearchBenchmark.java')],
                   check=True, timeout=60)
    result = subprocess.run(['java', '-Xmx384m', '-cp', str(ROOT / 'Java/build'), 'SearchBenchmark'],
                            text=True, capture_output=True, check=True, timeout=120, encoding='utf-8')
    benchmark = json.loads(result.stdout)
    (OUT / 'benchmark.json').write_text(json.dumps(benchmark, indent=2) + '\n', encoding='utf-8')
    summary['benchmark_rows'] = len(benchmark['rows'])
    files = ['Java/source/Algorithms.java', 'tests/student/verify.py',
             'tests/student/SearchBenchmark.java', 'tests/student/lcs.json']
    summary['sha256'] = {name: hashlib.sha256((ROOT / name).read_bytes()).hexdigest() for name in files}
    summary['finished_utc'] = datetime.now(timezone.utc).isoformat()
    summary['status'] = 'passed'
    (OUT / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n', encoding='utf-8')
    print('PASS witness validation and 18 search comparison rows', flush=True)


if __name__ == '__main__':
    main()
