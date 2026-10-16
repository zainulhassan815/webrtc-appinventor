import re
import sys

UNORDERED = '!unordered'

expected = [line for line in open(sys.argv[1]).read().splitlines() if line]
actual = [line for line in sys.stdin.read().splitlines() if line]
print('\n'.join(actual))


def ordered(patterns):
    failures = [f'line {i + 1}: expected /{pattern}/, got {actual[i] if i < len(actual) else "nothing"!r}'
                for i, pattern in enumerate(patterns) if i >= len(actual) or not re.fullmatch(pattern, actual[i])]
    if len(actual) > len(patterns):
        failures.append(f'unexpected lines: {actual[len(patterns):]}')
    return failures


def unordered(patterns):
    remaining = list(actual)
    failures = []
    for pattern in patterns:
        match = next((line for line in remaining if re.fullmatch(pattern, line)), None)
        if match is None:
            failures.append(f'no line matches /{pattern}/')
        else:
            remaining.remove(match)
    if remaining:
        failures.append(f'unexpected lines: {remaining}')
    return failures


failures = unordered(expected[1:]) if expected[:1] == [UNORDERED] else ordered(expected)
if failures:
    sys.exit('FAIL\n' + '\n'.join(failures))
print('PASS')
