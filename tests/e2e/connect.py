import json
import sys

USAGE = 'usage: connect.py <Screen.scm> <project-url> <publishable-key>'


def connect(component, url, key):
    if component.get('$Type') == 'Supabase':
        component.update({'ProjectUrl': url, 'PublishableKey': key})
    for child in component.get('$Components', []):
        connect(child, url, key)


def main(scm, url, key):
    text = open(scm).read()
    form = json.loads(text[text.index('{'):text.rindex('}') + 1])
    connect(form['Properties'], url, key)
    open(scm, 'w').write(f'#|\n$JSON\n{json.dumps(form)}\n|#\n')


if __name__ == '__main__':
    if len(sys.argv) != 4:
        sys.exit(USAGE)
    main(*sys.argv[1:])
