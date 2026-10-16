#!/usr/bin/env python3
import json
import os
import sys
import zipfile

USAGE = 'usage: app.py <project-dir> <aix> <form.json> <screen.yail>'
PACKAGE = 'dev.dreamerslab.webrtc.e2e.Screen1'
RUNTIME = 'com.google.appinventor.components.runtime'
BUILT_IN_VERSIONS = {
    'Button': '8', 'File': '4', 'HorizontalArrangement': '4', 'Image': '8', 'Label': '6', 'ListView': '11',
    'PasswordTextBox': '8', 'TextBox': '15', 'VerticalArrangement': '4', 'Web': '9',
}


def extension_components(aix, target):
    with zipfile.ZipFile(aix) as zf:
        zf.extractall(target)
        descriptor = next(name for name in zf.namelist() if name.endswith('/components.json'))
        return {c['name']: c for c in json.loads(zf.read(descriptor))}


def walk(components, container='Screen1'):
    for component in components:
        yield container, component
        yield from walk(component.get('children', []), component['name'])


def designer_value(value):
    if isinstance(value, bool):
        return 'True' if value else 'False'
    if isinstance(value, str) and value.startswith('@'):
        return value[1:]
    return str(value)


def yail_value(value):
    if isinstance(value, bool):
        return ('#t' if value else '#f'), 'boolean'
    if isinstance(value, (int, float)):
        return str(value), 'number'
    if value.startswith('@'):
        return f'(get-component {value[1:]})', 'component'
    return json.dumps(value), 'text'


def scm(components, extensions, uuids):
    def instance(component):
        name, type_name = component['name'], component['type']
        version = extensions[type_name]['version'] if type_name in extensions else BUILT_IN_VERSIONS[type_name]
        return {
            '$Name': name, '$Type': type_name, '$Version': str(version), 'Uuid': str(next(uuids)),
            **{key: designer_value(value) for key, value in component.get('properties', {}).items()},
            **({'$Components': [instance(c) for c in component['children']]} if component.get('children') else {}),
        }
    return {
        'authURL': ['ai2.appinventor.mit.edu'], 'YaVersion': '237', 'Source': 'Form',
        'Properties': {
            '$Name': 'Screen1', '$Type': 'Form', '$Version': '32', 'AppName': 'WebrtcE2E',
            'Title': 'WebRTC', 'Uuid': '0', '$Components': [instance(c) for c in components],
        },
    }


def yail(components, extensions, screen):
    def add(container, component):
        name, type_name = component['name'], component['type']
        full_type = extensions[type_name]['type'] if type_name in extensions else f'{RUNTIME}.{type_name}'
        properties = ' '.join(
            f"(set-and-coerce-property! '{name} '{key} {literal} '{kind})"
            for key, (literal, kind) in ((k, yail_value(v)) for k, v in component.get('properties', {}).items()))
        return f'(add-component {container} {full_type} {name} {properties})'
    adds = '\n'.join(add(container, component) for container, component in walk(components))
    return f'''#|
$Source $Yail
|#

(define-form {PACKAGE} Screen1 #f)
(require <com.google.youngandroid.runtime>)
(do-after-form-creation (set-and-coerce-property! 'Screen1 'AppName "WebrtcE2E" 'text))
{adds}
{screen}
(init-runtime)
'''


def main(project, aix, form, screen):
    with open(form) as f:
        components = json.load(f)
    extensions = extension_components(aix, os.path.join(project, 'assets', 'external_comps'))
    source = os.path.join(project, 'src', *PACKAGE.split('.')[:-1])
    os.makedirs(source, exist_ok=True)
    os.makedirs(os.path.join(project, 'youngandroidproject'), exist_ok=True)
    with open(os.path.join(project, 'youngandroidproject', 'project.properties'), 'w') as f:
        f.write(f'main={PACKAGE}\nname=WebrtcE2E\nassets=../assets\nsource=../src\nbuild=../build\n'
                'versioncode=1\nversionname=1.0\nuseslocation=False\naname=WebrtcE2E\nsizing=Responsive\n'
                'showlistsasjson=True\ntheme=AppTheme.Light.DarkActionBar\n')
    with open(os.path.join(source, 'Screen1.scm'), 'w') as f:
        f.write(f'#|\n$JSON\n{json.dumps(scm(components, extensions, iter(range(1, 1000))))}\n|#\n')
    with open(os.path.join(source, 'Screen1.bky'), 'w') as f:
        f.write('<xml xmlns="http://www.w3.org/1999/xhtml"></xml>\n')
    with open(screen) as f, open(os.path.join(source, 'Screen1.yail'), 'w') as out:
        out.write(yail(components, extensions, f.read()))


if __name__ == '__main__':
    if len(sys.argv) != 5:
        sys.exit(USAGE)
    main(*sys.argv[1:])
