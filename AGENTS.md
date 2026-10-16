# Agent instructions

This is an MIT App Inventor extension built with Rush.

Before changing the extension, read `.claude/skills/rush/SKILL.md`.
It explains how blocks, dropdowns, events, dependencies and resources work,
and the limits App Inventor imposes.

## Workflow

1. Run `rush build` after every change and fix every error it reports.
2. Read `out/<package>.md` to check that the blocks match what was asked.
3. Tell the user what to test in App Inventor: `rush build` can't run the
   extension.

## Rules

- Blocks are public methods of the `@Extension` class, annotated with
  `@SimpleFunction`, `@SimpleProperty` or `@SimpleEvent`. Use primitive
  types.
- Extensions are non-visible. To show a view, add it to an arrangement passed
  to a block.
- Dropdown enums go in a directory named `helpers`.
- Fire events on the main thread, never while `Screen.Initialize` runs.
- Declare every library in `rush.yml`.
- Don't edit `.rush/` or `out/`: Rush generates them.

## Project notes

- `Call` owns one native peer connection. It is main-thread only: every WebRTC
  callback is posted to the main looper, and nothing fires after `close()`.
- WebRTC aborts the app natively without `ACCESS_NETWORK_STATE`. Keep it in the
  manifest.
- The phone has one audio route, so `CallAudio` is shared by every `PeerConnection`.
- The JNI library must stay a shared AAR: native code binds to `org.webrtc` names.
- Kotlin components that are renamed or deleted stay in `components.json` until
  `rush clean` (rush-cli#78).
- The AI Companion can't load the WebRTC AAR. Verify with the device tests in
  `docs/testing.md`, not only `rush build`.
- `samples/web-call/call.js` is the sample's protocol in JavaScript, shared by the web
  client and the call test. Change both sides together.
