import glob
import os
import sys
import zipfile

USAGE = 'usage: package.py <out-dir> <supabase.aix>'
SAMPLE = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'video-call')
PROJECT_FOLDERS = ('youngandroidproject', 'src', 'assets')


def package(aixes, aia_path):
    with zipfile.ZipFile(aia_path, 'w', zipfile.ZIP_DEFLATED) as aia:
        for folder in PROJECT_FOLDERS:
            for root, _, files in os.walk(os.path.join(SAMPLE, folder)):
                for name in files:
                    path = os.path.join(root, name)
                    aia.write(path, os.path.relpath(path, SAMPLE))
        for aix_path in aixes:
            with zipfile.ZipFile(aix_path) as aix:
                for name in aix.namelist():
                    if not name.endswith('/'):
                        aia.writestr(f'assets/external_comps/{name}', aix.read(name))


if __name__ == '__main__':
    if len(sys.argv) != 3:
        sys.exit(USAGE)
    out, supabase = sys.argv[1:]
    package([glob.glob(os.path.join(out, 'dev.dreamerslab.webrtc.aix'))[0], supabase], os.path.join(out, 'VideoCall.aia'))
