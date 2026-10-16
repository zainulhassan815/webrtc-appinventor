import glob
import json
import os
import sys

USAGE = 'usage: components.py <external_comps-dir>'

if __name__ == '__main__':
    if len(sys.argv) != 2:
        sys.exit(USAGE)
    descriptors = sorted(glob.glob(os.path.join(sys.argv[1], '*', 'components.json')))
    print(json.dumps([component for path in descriptors for component in json.load(open(path))]))
