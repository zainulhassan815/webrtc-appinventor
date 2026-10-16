import glob
import html
import json
import os
import re
import sys
import zipfile

DOCS = os.path.dirname(os.path.abspath(__file__))
IMAGES = 'images/blocks'
COMPONENT = 'PeerConnection'
INSTANCE = f'{COMPONENT}1'

SECTIONS = [
    ('Camera and microphone', ['StartLocalMedia', 'StopLocalMedia', 'LocalMediaStarted', 'SwitchCamera', 'CameraSwitched',
                               'MicrophoneEnabled', 'CameraEnabled', 'Speakerphone', 'VideoWidth', 'VideoHeight',
                               'VideoFrameRate']),
    ('Video on screen', ['ShowLocalVideo', 'ShowRemoteVideo', 'RemoteTrackAdded', 'MirrorLocalVideo', 'VideoScaling']),
    ('Connecting', ['CreateOffer', 'CreateAnswer', 'SetRemoteDescription', 'AddIceCandidate', 'LocalDescriptionSet',
                    'IceCandidateFound', 'ConnectionStateChanged', 'ConnectionState', 'IceServers', 'Close']),
    ('Data channels', ['CreateDataChannel', 'Send', 'DataChannelOpened', 'DataChannelClosed', 'MessageReceived']),
    ('Errors', ['Error']),
]


def sentence(text):
    text = text.strip()
    return text if text.endswith(('.', '!', '?')) else text + '.'


def plain(text):
    return html.unescape(re.sub(r'<[^>]+>', '', text)).strip()


def anchor(title):
    return re.sub(r'[^\w\- ]', '', title.lower()).replace(' ', '-')


class Reference:

    def __init__(self, component):
        self.component = component
        self.images = []
        self.entries = {}
        for kind in ('methods', 'events'):
            for entry in component[kind]:
                self.entries[entry['name']] = self.block(entry)
        for prop in component['blockProperties']:
            self.entries[prop['name']] = self.property(prop)

    def image(self, name, alt):
        self.images.append(name)
        return f'![{alt}]({IMAGES}/{name}.png)'

    def block(self, entry):
        return f'{self.image(f"{INSTANCE}-{entry["name"]}", entry["name"])}\n\n**{entry["name"]}:** {sentence(entry["description"])}'

    def property(self, prop):
        shots = [self.image(f'{INSTANCE}-get{prop["name"]}', f'get {prop["name"]}')]
        if prop['rw'] == 'read-write':
            shots.append(self.image(f'{INSTANCE}-set{prop["name"]}', f'set {prop["name"]}'))
        return ' '.join(shots) + f'\n\n**{prop["name"]}** ({prop["rw"]}): {sentence(prop["description"])}'

    def dropdowns(self):
        found = {}
        for entry in self.component['events'] + self.component['methods'] + self.component['blockProperties']:
            for owner in [entry] + entry.get('params', []):
                data = owner.get('helper', {}).get('data')
                if data:
                    found[data['key']] = data
        return [f'{self.image(f"helper-{key}", key)}\n\n**{key}:** ' + ', '.join(o['name'] for o in data['options'])
                for key, data in sorted(found.items())]

    def markdown(self):
        listed = [name for _, names in SECTIONS for name in names]
        unlisted = sorted(set(self.entries) - set(listed))
        unknown = sorted(set(listed) - set(self.entries))
        if unlisted or unknown:
            sys.exit(f'Blocks without a section: {unlisted}. Sections naming no block: {unknown}')
        lines = [
            '# Blocks',
            '',
            f'Every block of the WebRTC extension. {sentence(plain(self.component["helpString"]))} Each image '
            'contains its block, so you can drag it straight into your workspace.',
            '',
        ]
        lines += [f'- [{title}](#{anchor(title)})' for title, _ in SECTIONS] + ['- [Dropdowns](#dropdowns)']
        for title, names in SECTIONS:
            lines += ['', f'## {title}', '', '\n\n'.join(self.entries[name] for name in names)]
        lines += ['', '## Dropdowns', '', '\n\n'.join(self.dropdowns()), '']
        return '\n'.join(lines)


def main(aix_path):
    with zipfile.ZipFile(aix_path) as aix:
        components = next(json.loads(aix.read(n)) for n in aix.namelist() if n.endswith('/components.json'))
    reference = Reference(next(c for c in components if c['name'] == COMPONENT))
    text = reference.markdown()
    missing = [n for n in reference.images if not os.path.exists(os.path.join(DOCS, IMAGES, f'{n}.png'))]
    if missing:
        sys.exit(f'Missing block images: {", ".join(missing)}')
    with open(os.path.join(DOCS, 'blocks.md'), 'w') as f:
        f.write(text)


if __name__ == '__main__':
    out = sys.argv[1] if len(sys.argv) > 1 else 'out'
    main(glob.glob(os.path.join(out, '*.aix'))[0])
