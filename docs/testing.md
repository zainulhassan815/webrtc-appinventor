# Testing

The tests build real App Inventor apps with App Inventor's build server and run them on
an Android device.

## Setup

- A checkout of [rush-cli](https://github.com/shreyashsaitwal/rush-cli) next to this
  repository (or `RUSH_CLI`), whose `tool/e2e/buildserver.sh` builds App Inventor's
  build server once. The call test also needs its blocks editor:
  `ant -f blocklyeditor/build.xml BlocklyTestbed` in that App Inventor checkout.
- JDK 11, Node.js, and `npm install` in `tests/e2e`.
- A device on `adb`, unlocked. Set `ADB` to use another adb. The tests grant the camera
  and microphone permissions when they install the app.

## Loopback scenarios

```
tests/e2e/run.sh tests/e2e/scenarios/data.yail
```

A scenario is a screen's YAIL with two components, `Caller` and `Callee`, that call
each other inside one app. It reports lines into a Label, which are matched against the
scenario's `.expected` patterns (`!unordered` on the first line allows any order).

- `data` connects over a data channel only, delivers the offer after the first ICE
  candidate (so candidates must wait for the description), exchanges messages, and
  checks that closing one side closes the other's channel.
- `video` sends the camera and microphone, and switches cameras once connected.
- `pip` shows both videos in one arrangement. Check the screenshot the run saves in
  `.e2e/pip/screen.png` for the inset.

## Calling the sample

```
SUPABASE_URL=… SUPABASE_KEY=… tests/e2e/call.sh phone
```

Builds `VideoCall.aia` with both extensions (`SUPABASE_AIX`, or the Supabase extension
checked out next to this repository), compiles its blocks with App Inventor's own
generator, installs it, joins a room on the phone and calls it from headless Chrome,
which uses a fake camera and [`samples/web-call/call.js`](../samples/web-call/call.js).
It passes when the browser sends and decodes video, the phone shows `Connected`, and
the phone notices the hang-up. `phone` or `browser` picks who sends the offer. Any
Supabase project works: the sample needs no tables.
