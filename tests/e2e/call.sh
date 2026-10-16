#!/usr/bin/env bash
set -euo pipefail

usage='usage: call.sh <phone|browser>, the side that sends the offer, with SUPABASE_URL and SUPABASE_KEY set'
offerer=${1:?$usage}
: "${SUPABASE_URL:?$usage}" "${SUPABASE_KEY:?$usage}"
here=$(cd "$(dirname "$0")" && pwd)
root=$(cd "$here/../.." && pwd)
adb=${ADB:-$HOME/Android/sdk/platform-tools/adb}
package=appinventor.ai_dreamerslab.VideoCall
appinventor=$("${RUSH_CLI:-$root/../rush-cli}/tool/e2e/buildserver.sh")
supabase_aix=${SUPABASE_AIX:-$(ls "$root"/../supabase-appinventor/out/*.aix)}
work=$root/.e2e/video-call
screen=$work/project/src/appinventor/ai_dreamerslab/VideoCall
room=e2e$RANDOM
case $offerer in
  browser) peer=a$RANDOM ;;
  phone) peer=z$RANDOM ;;
  *) echo "$usage" >&2; exit 2 ;;
esac

(cd "$root" && rush build --no-color >/dev/null && python3 -I samples/package.py out "$supabase_aix")
rm -rf "$work" && mkdir -p "$work/project"
unzip -q "$root/out/VideoCall.aia" -d "$work/project"
python3 -I "$here/connect.py" "$screen/Screen1.scm" "$SUPABASE_URL" "$SUPABASE_KEY"
python3 -I "$here/components.py" "$work/project/assets/external_comps" > "$work/components.json"
node "$here/blocks/compile.js" "$appinventor" "$screen/Screen1.scm" "$screen/Screen1.bky" \
  "$work/components.json" "$package.Screen1" "$screen/Screen1.yail"
apk=$("$here/build-apk.sh" "$work")

"$adb" uninstall "$package" >/dev/null 2>&1 || true
"$adb" install -g "$apk" >/dev/null
"$adb" shell monkey -p "$package" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
python3 -I "$here/drive.py" join "$room"
node "$here/peer/peer.js" "$room" "$peer" 30 > "$work/peer.log" 2>&1 &
browser=$!
python3 -I "$here/drive.py" wait 'Call: Connected' 30
sleep 8
"$adb" exec-out screencap -p > "$work/call.png"
status=0
wait "$browser" || status=$?
grep -vE '^(channel|state|track|offering|bye)' "$work/peer.log" | tail -1
python3 -I "$here/drive.py" wait '(Call ended by the other person|The other person left)\..*' 15
exit "$status"
