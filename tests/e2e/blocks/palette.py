#!/usr/bin/env python3
import json
import os
import sys
from xml.sax.saxutils import escape

USAGE = 'usage: palette.py <components.json> <out-dir>'


def instance(component):
    return f'{component["name"]}1'


def block(block_id, kind, mutation, fields):
    field_xml = ''.join(f'<field name="{k}">{escape(v)}</field>' for k, v in fields.items())
    return f'<block type="{kind}" id="{block_id}"><mutation {mutation}></mutation>{field_xml}</block>'


def common(component):
    return f'component_type="{component["name"]}" is_generic="false" instance_name="{instance(component)}"'


def blocks(component):
    name = instance(component)
    selector = {'COMPONENT_SELECTOR': name}
    for event in component['events']:
        yield block(f'{name}.{event["name"]}', 'component_event',
                    f'{common(component)} event_name="{event["name"]}"', selector)
    for method in component['methods']:
        yield block(f'{name}.{method["name"]}', 'component_method',
                    f'{common(component)} method_name="{method["name"]}"', selector)
    for prop in component['blockProperties']:
        modes = {'read-write': ['get', 'set'], 'read-only': ['get'], 'write-only': ['set']}[prop['rw']]
        for mode in modes:
            yield block(f'{name}.{mode}{prop["name"]}', 'component_set_get',
                        f'{common(component)} set_or_get="{mode}" property_name="{prop["name"]}"',
                        {**selector, 'PROP': prop['name']})


def helpers(components):
    found = {}
    for component in components:
        for entry in component['events'] + component['methods'] + component['blockProperties']:
            for owner in [entry] + entry.get('params', []):
                data = owner.get('helper', {}).get('data')
                if data:
                    found[data['key']] = data['defaultOpt']
    for key, default in sorted(found.items()):
        yield block(f'helper.{key}', 'helpers_dropdown', f'key="{key}"', {'OPTION': default})


def form(components):
    return {
        'authURL': ['ai2.appinventor.mit.edu'], 'YaVersion': '237', 'Source': 'Form',
        'Properties': {
            '$Name': 'Screen1', '$Type': 'Form', '$Version': '32', 'AppName': 'Palette', 'Title': 'Palette',
            'Uuid': '0',
            '$Components': [{'$Name': instance(c), '$Type': c['name'], '$Version': str(c['version']),
                             'Uuid': str(i + 1)} for i, c in enumerate(components)],
        },
    }


def main(components_path, out_dir):
    components = json.load(open(components_path))
    os.makedirs(out_dir, exist_ok=True)
    all_blocks = [b for c in components for b in blocks(c)] + list(helpers(components))
    with open(os.path.join(out_dir, 'Screen1.scm'), 'w') as f:
        f.write(f'#|\n$JSON\n{json.dumps(form(components))}\n|#\n')
    for b in all_blocks:
        block_id = b.split('id="', 1)[1].split('"', 1)[0]
        with open(os.path.join(out_dir, f'{block_id}.bky'), 'w') as f:
            f.write('<xml xmlns="https://developers.google.com/blockly/xml">' + b +
                    '<yacodeblocks ya-version="237" language-version="39"></yacodeblocks></xml>')
        print(block_id)


if __name__ == '__main__':
    if len(sys.argv) != 3:
        sys.exit(USAGE)
    main(*sys.argv[1:])
