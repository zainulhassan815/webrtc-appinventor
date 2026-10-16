# Blocks

Every block of the WebRTC extension. A WebRTC call with another device: audio, video, and data channels for text messages. Send the descriptions and ICE candidates it produces to the other device through any channel, such as Supabase broadcast, and pass theirs back in. Each image contains its block, so you can drag it straight into your workspace.

- [Camera and microphone](#camera-and-microphone)
- [Video on screen](#video-on-screen)
- [Connecting](#connecting)
- [Data channels](#data-channels)
- [Errors](#errors)
- [Dropdowns](#dropdowns)

## Camera and microphone

![StartLocalMedia](images/blocks/PeerConnection1-StartLocalMedia.png)

**StartLocalMedia:** Starts the microphone, the camera or both, asking for permission first. LocalMediaStarted fires when they're ready: create the offer or answer after it so the other device receives them. If the user refuses, Screen.PermissionDenied fires.

![StopLocalMedia](images/blocks/PeerConnection1-StopLocalMedia.png)

**StopLocalMedia:** Stops the camera and microphone.

![LocalMediaStarted](images/blocks/PeerConnection1-LocalMediaStarted.png)

**LocalMediaStarted:** The microphone and camera asked for in StartLocalMedia are ready.

![SwitchCamera](images/blocks/PeerConnection1-SwitchCamera.png)

**SwitchCamera:** Switches between the front and back cameras. CameraSwitched fires when done.

![CameraSwitched](images/blocks/PeerConnection1-CameraSwitched.png)

**CameraSwitched:** The camera switched. Front is true when the front camera is now in use.

![get MicrophoneEnabled](images/blocks/PeerConnection1-getMicrophoneEnabled.png) ![set MicrophoneEnabled](images/blocks/PeerConnection1-setMicrophoneEnabled.png)

**MicrophoneEnabled** (read-write): Whether your microphone's audio is sent. When false, the other device hears silence.

![get CameraEnabled](images/blocks/PeerConnection1-getCameraEnabled.png) ![set CameraEnabled](images/blocks/PeerConnection1-setCameraEnabled.png)

**CameraEnabled** (read-write): Whether your camera's video is sent. When false, the other device sees black.

![get Speakerphone](images/blocks/PeerConnection1-getSpeakerphone.png) ![set Speakerphone](images/blocks/PeerConnection1-setSpeakerphone.png)

**Speakerphone** (read-write): Whether call audio plays through the loudspeaker. When false, it plays through the earpiece, or a connected headset.

![get VideoWidth](images/blocks/PeerConnection1-getVideoWidth.png) ![set VideoWidth](images/blocks/PeerConnection1-setVideoWidth.png)

**VideoWidth** (read-write): The camera's capture width in pixels for the next StartLocalMedia. The camera uses its closest size.

![get VideoHeight](images/blocks/PeerConnection1-getVideoHeight.png) ![set VideoHeight](images/blocks/PeerConnection1-setVideoHeight.png)

**VideoHeight** (read-write): The camera's capture height in pixels for the next StartLocalMedia.

![get VideoFrameRate](images/blocks/PeerConnection1-getVideoFrameRate.png) ![set VideoFrameRate](images/blocks/PeerConnection1-setVideoFrameRate.png)

**VideoFrameRate** (read-write): The camera's frames per second for the next StartLocalMedia.

## Video on screen

![ShowLocalVideo](images/blocks/PeerConnection1-ShowLocalVideo.png)

**ShowLocalVideo:** Shows your camera's video in an arrangement, filling it. Show it in the same arrangement as the remote video to get a small picture in the corner.

![ShowRemoteVideo](images/blocks/PeerConnection1-ShowRemoteVideo.png)

**ShowRemoteVideo:** Shows the other device's video in an arrangement, filling it.

![RemoteTrackAdded](images/blocks/PeerConnection1-RemoteTrackAdded.png)

**RemoteTrackAdded:** The other device's audio or video arrived. Kind is audio or video. Audio plays by itself; show video with ShowRemoteVideo.

![get MirrorLocalVideo](images/blocks/PeerConnection1-getMirrorLocalVideo.png) ![set MirrorLocalVideo](images/blocks/PeerConnection1-setMirrorLocalVideo.png)

**MirrorLocalVideo** (read-write): Whether your own video is shown mirrored while the front camera is in use. The other device always sees it unmirrored.

![get VideoScaling](images/blocks/PeerConnection1-getVideoScaling.png) ![set VideoScaling](images/blocks/PeerConnection1-setVideoScaling.png)

**VideoScaling** (read-write): How video fits its view: Fill crops the edges, Fit shows the whole picture.

## Connecting

![CreateOffer](images/blocks/PeerConnection1-CreateOffer.png)

**CreateOffer:** Starts a connection by creating an offer. LocalDescriptionSet fires with it: send it to the other device. Start local media and create data channels first.

![CreateAnswer](images/blocks/PeerConnection1-CreateAnswer.png)

**CreateAnswer:** Answers an offer passed to SetRemoteDescription. LocalDescriptionSet fires with the answer: send it back to the other device.

![SetRemoteDescription](images/blocks/PeerConnection1-SetRemoteDescription.png)

**SetRemoteDescription:** Applies the other device's offer or answer. Type is offer or answer.

![AddIceCandidate](images/blocks/PeerConnection1-AddIceCandidate.png)

**AddIceCandidate:** Adds an ICE candidate from the other device. Candidates that arrive before the other device's description are kept until it's set.

![LocalDescriptionSet](images/blocks/PeerConnection1-LocalDescriptionSet.png)

**LocalDescriptionSet:** Your offer or answer is ready. Send type and sdp to the other device, which passes them to SetRemoteDescription.

![IceCandidateFound](images/blocks/PeerConnection1-IceCandidateFound.png)

**IceCandidateFound:** A way to reach this device was found. Send all three values to the other device, which passes them to AddIceCandidate.

![ConnectionStateChanged](images/blocks/PeerConnection1-ConnectionStateChanged.png)

**ConnectionStateChanged:** The connection's state changed.

![get ConnectionState](images/blocks/PeerConnection1-getConnectionState.png)

**ConnectionState** (read-only): The connection's state.

![get IceServers](images/blocks/PeerConnection1-getIceServers.png) ![set IceServers](images/blocks/PeerConnection1-setIceServers.png)

**IceServers** (read-write): The STUN and TURN servers used by the next connection: a list of dictionaries with urls (text or a list), and username and credential for TURN servers. Defaults to Google's public STUN server.

![Close](images/blocks/PeerConnection1-Close.png)

**Close:** Ends the connection. The other device's data channels close, and its connection state changes once it notices.

## Data channels

![CreateDataChannel](images/blocks/PeerConnection1-CreateDataChannel.png)

**CreateDataChannel:** Creates a data channel for text messages. Create it before CreateOffer; the other device gets it too, and DataChannelOpened fires on both when it's ready.

![Send](images/blocks/PeerConnection1-Send.png)

**Send:** Sends text over an open data channel.

![DataChannelOpened](images/blocks/PeerConnection1-DataChannelOpened.png)

**DataChannelOpened:** A data channel is ready to send and receive.

![DataChannelClosed](images/blocks/PeerConnection1-DataChannelClosed.png)

**DataChannelClosed:** A data channel closed.

![MessageReceived](images/blocks/PeerConnection1-MessageReceived.png)

**MessageReceived:** Text arrived on a data channel.

## Errors

![Error](images/blocks/PeerConnection1-Error.png)

**Error:** A block or the connection failed. Without this event, Screen.ErrorOccurred fires instead.

## Dropdowns

![ConnectionState](images/blocks/helper-ConnectionState.png)

**ConnectionState:** New, Connecting, Connected, Disconnected, Failed, Closed

![VideoScaling](images/blocks/helper-VideoScaling.png)

**VideoScaling:** Fill, Fit
