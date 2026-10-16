# The Video Call sample

`VideoCall.aia`, in every [release](https://github.com/zainulhassan815/webrtc-appinventor/releases/latest),
is a complete calling app: two people enter the same room name, tap **Join**, and see
each other. It uses a `PeerConnection` named `Call`, a `Supabase` component, and a
`SupabaseChannel` named `Signaling` to carry the call setup. Each image below contains
its blocks, so you can drag it into your own project.

## Joining a room

Each device picks a random id when the app starts, and uses it as its presence key on
the channel. `offered` remembers whether this device already started or answered a
call.

![global me](images/sample/global-me.png)
![global offered](images/sample/global-offered.png)

![Screen1.Initialize](images/sample/Screen1-Initialize.png)

**Join** shows both videos in the same arrangement, so your own video becomes a small
picture in the corner, and starts the camera and microphone.

![JoinButton.Click](images/sample/JoinButton-Click.png)

Once they're ready, the device listens for `signal` broadcasts and joins the channel.
When it's subscribed, it announces itself through presence.

![Call.LocalMediaStarted](images/sample/Call-LocalMediaStarted.png)

![Signaling.StatusChanged](images/sample/Signaling-StatusChanged.png)

## Who calls whom

Both devices see each other in the channel's presence. Only the one with the smaller id
creates the offer, so the two never offer at the same time. When the other person
leaves, the call closes and the device waits for someone new.

![Signaling.PresenceChanged](images/sample/Signaling-PresenceChanged.png)

## Exchanging the call setup

Every description and ICE candidate the component produces goes out as a `signal`
broadcast, with this device's id:

![Call.LocalDescriptionSet](images/sample/Call-LocalDescriptionSet.png)

![Call.IceCandidateFound](images/sample/Call-IceCandidateFound.png)

And every `signal` that arrives goes back into the component. An offer gets an answer;
`bye` means the other person hung up.

![Signaling.BroadcastReceived](images/sample/Signaling-BroadcastReceived.png)

![Call.ConnectionStateChanged](images/sample/Call-ConnectionStateChanged.png)

## Call controls

![MuteButton.Click](images/sample/MuteButton-Click.png)

![CameraButton.Click](images/sample/CameraButton-Click.png)

![SwitchButton.Click](images/sample/SwitchButton-Click.png)

![SpeakerButton.Click](images/sample/SpeakerButton-Click.png)

**Hang up** tells the other device, ends the call, stops the camera and leaves the
channel.

![HangUpButton.Click](images/sample/HangUpButton-Click.png)

## Errors and permissions

![Call.Error](images/sample/Call-Error.png)

![Signaling.Error](images/sample/Signaling-Error.png)

![Supabase1.Error](images/sample/Supabase1-Error.png)

![Screen1.PermissionDenied](images/sample/Screen1-PermissionDenied.png)
