#!/usr/bin/env bash
set -euo pipefail

name=${1:?usage: build-app.sh <name> <form.json> <screen.yail>}
form=$(realpath "${2:?usage: build-app.sh <name> <form.json> <screen.yail>}")
screen=$(realpath "${3:?usage: build-app.sh <name> <form.json> <screen.yail>}")
here=$(cd "$(dirname "$0")" && pwd)
root=$(cd "$here/../.." && pwd)
work=$root/.e2e/$name

(cd "$root" && rush build --no-color >/dev/null)
aix=$(ls "$root"/out/*.aix)
rm -rf "$work" && mkdir -p "$work/project"
python3 -I "$here/app.py" "$work/project" "$aix" "$form" "$screen"
"$here/build-apk.sh" "$work"
