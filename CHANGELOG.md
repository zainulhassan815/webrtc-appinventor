# Changelog

## 1.0.0 (2026-10-16)

First release, with one component, `PeerConnection`:

- **Calls:** one-to-one audio and video between two phones, or a phone and a browser. `CreateOffer`, `CreateAnswer`, `SetRemoteDescription` and `AddIceCandidate`, with `LocalDescriptionSet` and `IceCandidateFound` handing out the texts to send. Candidates that arrive before the other device's description are kept until it's set.
- **Signaling of your choice:** any channel that carries text works. The sample uses the Supabase extension's broadcast and presence.
- **Camera and microphone:** `StartLocalMedia` asks for permission and starts them. `MicrophoneEnabled`, `CameraEnabled`, `SwitchCamera`, and `VideoWidth`, `VideoHeight` and `VideoFrameRate`.
- **Video on screen:** `ShowLocalVideo` and `ShowRemoteVideo` show video in an arrangement. Put both in the same arrangement and your own video becomes a small picture in the corner. `MirrorLocalVideo` and `VideoScaling` (Fill or Fit).
- **Call audio:** loudspeaker or earpiece with `Speakerphone`. The phone's audio mode and focus are restored when the call ends.
- **Data channels:** `CreateDataChannel`, `Send` and `MessageReceived` for text, with or without audio and video.
- **ICE servers:** Google's public STUN server by default. Add TURN servers through `IceServers`.
- **Lifecycle:** the camera pauses while the app is in the background.
- **Samples:** `VideoCall.aia`, an App Inventor app that calls through Supabase, and [a web client](samples/web-call) that calls it from a browser.

Needs Android 8.0 or newer and an APK build: the AI Companion can't load WebRTC.
