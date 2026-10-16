#!/usr/bin/env bash
set -euo pipefail

work=$(realpath "${1:?usage: build-apk.sh <work-dir with project/>}")
here=$(cd "$(dirname "$0")" && pwd)
root=$(cd "$here/../.." && pwd)
appinventor=$("${RUSH_CLI:-$root/../rush-cli}/tool/e2e/buildserver.sh")
java11=${JAVA11_HOME:-$(ls -d "$HOME"/.sdkman/candidates/java/11* | head -1)}

rm -rf "$work/out" "$work/tmp" "$work/dex" "$work/project.aia"
mkdir -p "$work/out" "$work/tmp/lib64" "$work/dex"
(cd "$work/project" && zip -q -r -D "$work/project.aia" .)
cp "$appinventor/lib/android/tools/linux/lib64/libc++.so" "$work/tmp/lib64/"
if ! "$java11/bin/java" -Djava.io.tmpdir="$work/tmp" -Dfile.encoding=UTF-8 -Djava.awt.headless=true \
  -cp "$appinventor/buildserver/build/run/lib/*" com.google.appinventor.buildserver.Main \
  --inputZipFile "$work/project.aia" --userName e2e --outputDir "$work/out" --dexCacheDir "$work/dex" \
  > "$work/build.log" 2>&1; then
  grep -E 'ERROR|error:' "$work/build.log" | head -5 >&2
  echo "APK build failed, see $work/build.log" >&2
  exit 1
fi
ls "$work"/out/*.apk
