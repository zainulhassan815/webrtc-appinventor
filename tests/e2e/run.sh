#!/usr/bin/env bash
set -euo pipefail

scenario=$(realpath "${1:?usage: run.sh <scenario.yail> [timeout-seconds]}")
timeout=${2:-60}
here=$(cd "$(dirname "$0")" && pwd)
adb=${ADB:-$HOME/Android/sdk/platform-tools/adb}
package=dev.dreamerslab.webrtc.e2e
name=$(basename "$scenario" .yail)
screen=$(mktemp --suffix=.yail)
trap 'rm -f "$screen"' EXIT
cat "$here/report.yail" "$scenario" > "$screen"
apk=$("$here/build-app.sh" "$name" "$here/scenario.json" "$screen")
work=$(dirname "$(dirname "$apk")")

"$adb" uninstall "$package" >/dev/null 2>&1 || true
"$adb" install -g "$apk" >/dev/null
"$adb" logcat -c
"$adb" shell monkey -p "$package" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
label() {
  "$adb" shell uiautomator dump /sdcard/e2e.xml >/dev/null
  "$adb" shell cat /sdcard/e2e.xml | python3 -I "$here/label.py"
}

deadline=$((SECONDS + timeout))
until label | python3 -I "$here/check.py" "${scenario%.yail}.expected" >/dev/null 2>&1; do
  if "$adb" logcat -d | grep -A15 "FATAL EXCEPTION" | grep -q "Process: $package"; then
    "$adb" logcat -d | grep -A20 "FATAL EXCEPTION" >&2
    exit 1
  fi
  [ $SECONDS -lt $deadline ] || break
  sleep 2
done
"$adb" exec-out screencap -p > "$work/screen.png"
label | python3 -I "$here/check.py" "${scenario%.yail}.expected"
