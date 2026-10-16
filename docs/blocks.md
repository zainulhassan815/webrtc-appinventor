# PeerConnection

<p>A WebRTC call with another device: audio, video, and data channels for text messages. Send the descriptions and ICE candidates it produces to the other device through any channel, such as Supabase broadcast, and pass theirs back in.</p>

## Events

### LocalMediaStarted()

The microphone and camera asked for in StartLocalMedia are ready.

### RemoteTrackAdded(kind)

The other device's audio or video arrived. Kind is audio or video. Audio plays by itself; show video with ShowRemoteVideo.

| Parameter | Type |
|---|---|
| kind | text |

### CameraSwitched(front)

The camera switched. Front is true when the front camera is now in use.

| Parameter | Type |
|---|---|
| front | boolean |

### LocalDescriptionSet(type, sdp)

Your offer or answer is ready. Send type and sdp to the other device, which passes them to SetRemoteDescription.

| Parameter | Type |
|---|---|
| type | text |
| sdp | text |

### IceCandidateFound(sdpMid, sdpMLineIndex, candidate)

A way to reach this device was found. Send all three values to the other device, which passes them to AddIceCandidate.

| Parameter | Type |
|---|---|
| sdpMid | text |
| sdpMLineIndex | number |
| candidate | text |

### ConnectionStateChanged(state)

The connection's state changed.

| Parameter | Type |
|---|---|
| state | ConnectionState |

### DataChannelOpened(label)

A data channel is ready to send and receive.

| Parameter | Type |
|---|---|
| label | text |

### DataChannelClosed(label)

A data channel closed.

| Parameter | Type |
|---|---|
| label | text |

### MessageReceived(label, text)

Text arrived on a data channel.

| Parameter | Type |
|---|---|
| label | text |
| text | text |

### Error(operation, message)

A block or the connection failed. Without this event, Screen.ErrorOccurred fires instead.

| Parameter | Type |
|---|---|
| operation | text |
| message | text |

## Methods

### StartLocalMedia(audio, video)

Starts the microphone, the camera or both, asking for permission first. LocalMediaStarted fires when they're ready: create the offer or answer after it so the other device receives them. If the user refuses, Screen.PermissionDenied fires.

| Parameter | Type |
|---|---|
| audio | boolean |
| video | boolean |

### StopLocalMedia()

Stops the camera and microphone.

### ShowLocalVideo(arrangement)

Shows your camera's video in an arrangement, filling it. Show it in the same arrangement as the remote video to get a small picture in the corner.

| Parameter | Type |
|---|---|
| arrangement | component |

### ShowRemoteVideo(arrangement)

Shows the other device's video in an arrangement, filling it.

| Parameter | Type |
|---|---|
| arrangement | component |

### SwitchCamera()

Switches between the front and back cameras. CameraSwitched fires when done.

### CreateOffer()

Starts a connection by creating an offer. LocalDescriptionSet fires with it: send it to the other device. Start local media and create data channels first.

### CreateAnswer()

Answers an offer passed to SetRemoteDescription. LocalDescriptionSet fires with the answer: send it back to the other device.

### SetRemoteDescription(type, sdp)

Applies the other device's offer or answer. Type is offer or answer.

| Parameter | Type |
|---|---|
| type | text |
| sdp | text |

### AddIceCandidate(sdpMid, sdpMLineIndex, candidate)

Adds an ICE candidate from the other device. Candidates that arrive before the other device's description are kept until it's set.

| Parameter | Type |
|---|---|
| sdpMid | text |
| sdpMLineIndex | number |
| candidate | text |

### CreateDataChannel(label)

Creates a data channel for text messages. Create it before CreateOffer; the other device gets it too, and DataChannelOpened fires on both when it's ready.

| Parameter | Type |
|---|---|
| label | text |

### Send(label, text)

Sends text over an open data channel.

| Parameter | Type |
|---|---|
| label | text |
| text | text |

### Close()

Ends the connection. The other device's data channels close, and its connection state changes once it notices.

## Properties

### IceServers

The STUN and TURN servers used by the next connection: a list of dictionaries with urls (text or a list), and username and credential for TURN servers. Defaults to Google's public STUN server.

`list`, read-write.

### ConnectionState

The connection's state.

`ConnectionState`, read-only.

### MicrophoneEnabled

Whether your microphone's audio is sent. When false, the other device hears silence.

`boolean`, read-write, designer default `True`.

### CameraEnabled

Whether your camera's video is sent. When false, the other device sees black.

`boolean`, read-write, designer default `True`.

### Speakerphone

Whether call audio plays through the loudspeaker. When false, it plays through the earpiece, or a connected headset.

`boolean`, read-write, designer default `True`.

### MirrorLocalVideo

Whether your own video is shown mirrored while the front camera is in use. The other device always sees it unmirrored.

`boolean`, read-write, designer default `True`.

### VideoScaling

How video fits its view: Fill crops the edges, Fit shows the whole picture.

`VideoScaling`, read-write, designer default `Fill`.

### VideoWidth

The camera's capture width in pixels for the next StartLocalMedia. The camera uses its closest size.

`number`, read-write, designer default `1280`.

### VideoHeight

The camera's capture height in pixels for the next StartLocalMedia.

`number`, read-write, designer default `720`.

### VideoFrameRate

The camera's frames per second for the next StartLocalMedia.

`number`, read-write, designer default `30`.

## Dropdowns

### ConnectionState

| Option | Value | Description |
|---|---|---|
| New | `New` | No connection attempt has started yet. |
| Connecting | `Connecting` | The peers are finding a network path to each other. |
| Connected | `Connected` | Media and data flow between the peers. |
| Disconnected | `Disconnected` | The network path was lost. WebRTC tries to recover it. |
| Failed | `Failed` | The connection couldn't be established or recovered. |
| Closed | `Closed` | The connection was closed. |

### VideoScaling

| Option | Value | Description |
|---|---|---|
| Fill | `Fill` | Fills the view, cropping the video's edges. |
| Fit | `Fit` | Shows the whole video, with bars where its shape differs from the view's. |
