package dev.dreamerslab.webrtc

import android.Manifest
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import com.google.appinventor.components.annotations.DesignerProperty
import com.google.appinventor.components.annotations.Extension
import com.google.appinventor.components.annotations.Options
import com.google.appinventor.components.annotations.SimpleEvent
import com.google.appinventor.components.annotations.SimpleFunction
import com.google.appinventor.components.annotations.SimpleProperty
import com.google.appinventor.components.common.PropertyTypeConstants
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent
import com.google.appinventor.components.runtime.AndroidViewComponent
import com.google.appinventor.components.runtime.ComponentContainer
import com.google.appinventor.components.runtime.EventDispatcher
import com.google.appinventor.components.runtime.OnDestroyListener
import com.google.appinventor.components.runtime.OnPauseListener
import com.google.appinventor.components.runtime.OnResumeListener
import com.google.appinventor.components.runtime.util.BulkPermissionRequest
import com.google.appinventor.components.runtime.util.ErrorMessages
import com.google.appinventor.components.runtime.util.YailList
import dev.dreamerslab.webrtc.helpers.ConnectionState
import dev.dreamerslab.webrtc.helpers.VideoScaling
import org.webrtc.IceCandidate
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection.PeerConnectionState
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack

@Extension(
    description = "A WebRTC call with another device: audio, video, and data channels for text messages. " +
        "Send the descriptions and ICE candidates it produces to the other device through any channel, " +
        "such as Supabase broadcast, and pass theirs back in.",
    icon = "icon.png",
)
class PeerConnection(container: ComponentContainer) : AndroidNonvisibleComponent(container.`$form`()),
    OnPauseListener, OnResumeListener, OnDestroyListener {

    private val context = container.`$context`()
    private val main = Handler(Looper.getMainLooper())
    private var iceServers = IceServers.googleStun
    private var state = ConnectionState.New
    private var call: Call? = null
    private val localVideo = VideoView(form, inset = true)
    private val remoteVideo = VideoView(form, inset = false)
    private var camera: Camera? = null
    private var microphone: Microphone? = null
    private var microphoneEnabled = true
    private var cameraEnabled = true
    private var mirrorLocalVideo = true
    private var scaling = VideoScaling.Fill
    private var videoWidth = 1280
    private var videoHeight = 720
    private var videoFrameRate = 30

    init {
        form.registerForOnPause(this)
        form.registerForOnResume(this)
        form.registerForOnDestroy(this)
    }

    @SimpleProperty(description = "The STUN and TURN servers used by the next connection: a list of dictionaries " +
        "with urls (text or a list), and username and credential for TURN servers. Defaults to Google's public " +
        "STUN server.")
    fun IceServers(): YailList = iceServers

    @SimpleProperty
    fun IceServers(servers: YailList) = attempt("IceServers") {
        IceServers.parse(servers)
        iceServers = servers
    }

    @SimpleProperty(description = "The connection's state.")
    @Options(ConnectionState::class)
    fun ConnectionState(): String = state.toUnderlyingValue()

    @SimpleProperty(description = "Whether your microphone's audio is sent. When false, the other device hears " +
        "silence.")
    fun MicrophoneEnabled() = microphoneEnabled

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    fun MicrophoneEnabled(enabled: Boolean) {
        microphoneEnabled = enabled
        microphone?.track?.setEnabled(enabled)
    }

    @SimpleProperty(description = "Whether your camera's video is sent. When false, the other device sees black.")
    fun CameraEnabled() = cameraEnabled

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    fun CameraEnabled(enabled: Boolean) {
        cameraEnabled = enabled
        camera?.track?.setEnabled(enabled)
    }

    @SimpleProperty(description = "Whether your own video is shown mirrored while the front camera is in use. " +
        "The other device always sees it unmirrored.")
    fun MirrorLocalVideo() = mirrorLocalVideo

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "True")
    @SimpleProperty
    fun MirrorLocalVideo(mirror: Boolean) {
        mirrorLocalVideo = mirror
        updateMirror()
    }

    @SimpleProperty(description = "How video fits its view: Fill crops the edges, Fit shows the whole picture.")
    @Options(VideoScaling::class)
    fun VideoScaling(): String = scaling.toUnderlyingValue()

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_CHOICES, editorArgs = ["Fill", "Fit"], defaultValue = "Fill")
    @SimpleProperty
    fun VideoScaling(@Options(VideoScaling::class) scaling: String) = attempt("VideoScaling") {
        this.scaling = requireNotNull(VideoScaling.fromUnderlyingValue(scaling)) { "Unknown video scaling: $scaling" }
        localVideo.scale(this.scaling == VideoScaling.Fill)
        remoteVideo.scale(this.scaling == VideoScaling.Fill)
    }

    @SimpleProperty(description = "The camera's capture width in pixels for the next StartLocalMedia. The camera " +
        "uses its closest size.")
    fun VideoWidth() = videoWidth

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "1280")
    @SimpleProperty
    fun VideoWidth(width: Int) {
        videoWidth = width
    }

    @SimpleProperty(description = "The camera's capture height in pixels for the next StartLocalMedia.")
    fun VideoHeight() = videoHeight

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "720")
    @SimpleProperty
    fun VideoHeight(height: Int) {
        videoHeight = height
    }

    @SimpleProperty(description = "The camera's frames per second for the next StartLocalMedia.")
    fun VideoFrameRate() = videoFrameRate

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = "30")
    @SimpleProperty
    fun VideoFrameRate(frameRate: Int) {
        videoFrameRate = frameRate
    }

    @SimpleFunction(description = "Starts the microphone, the camera or both, asking for permission first. " +
        "LocalMediaStarted fires when they're ready: create the offer or answer after it so the other device " +
        "receives them. If the user refuses, Screen.PermissionDenied fires.")
    fun StartLocalMedia(audio: Boolean, video: Boolean) = attempt("StartLocalMedia") {
        require(audio || video) { "Start audio, video or both" }
        val permissions = listOfNotNull(
            Manifest.permission.RECORD_AUDIO.takeIf { audio },
            Manifest.permission.CAMERA.takeIf { video },
        )
        form.askPermission(object : BulkPermissionRequest(this@PeerConnection, "StartLocalMedia", *permissions.toTypedArray()) {
            override fun onGranted() = attempt("StartLocalMedia") { startLocalMedia(audio, video) }
        })
    }

    @SimpleFunction(description = "Stops the camera and microphone.")
    fun StopLocalMedia() {
        call?.stopSending()
        localVideo.show(null)
        camera?.dispose()
        camera = null
        microphone?.dispose()
        microphone = null
    }

    @SimpleFunction(description = "Shows your camera's video in an arrangement, filling it. Show it in the same " +
        "arrangement as the remote video to get a small picture in the corner.")
    fun ShowLocalVideo(arrangement: AndroidViewComponent) = attempt("ShowLocalVideo") {
        localVideo.showIn(viewGroup(arrangement))
    }

    @SimpleFunction(description = "Shows the other device's video in an arrangement, filling it.")
    fun ShowRemoteVideo(arrangement: AndroidViewComponent) = attempt("ShowRemoteVideo") {
        remoteVideo.showIn(viewGroup(arrangement))
    }

    @SimpleFunction(description = "Switches between the front and back cameras. CameraSwitched fires when done.")
    fun SwitchCamera() = attempt("SwitchCamera") {
        requireNotNull(camera) { "Start local media with video first" }.switch()
    }

    @SimpleFunction(description = "Starts a connection by creating an offer. LocalDescriptionSet fires with it: " +
        "send it to the other device. Start local media and create data channels first.")
    fun CreateOffer() = attempt("CreateOffer") { call().createOffer() }

    @SimpleFunction(description = "Answers an offer passed to SetRemoteDescription. LocalDescriptionSet fires " +
        "with the answer: send it back to the other device.")
    fun CreateAnswer() = attempt("CreateAnswer") { call().createAnswer() }

    @SimpleFunction(description = "Applies the other device's offer or answer. Type is offer or answer.")
    fun SetRemoteDescription(type: String, sdp: String) = attempt("SetRemoteDescription") {
        call().setRemoteDescription(SessionDescription(descriptionType(type), sdp))
    }

    @SimpleFunction(description = "Adds an ICE candidate from the other device. Candidates that arrive before " +
        "the other device's description are kept until it's set.")
    fun AddIceCandidate(sdpMid: String, sdpMLineIndex: Int, candidate: String) = attempt("AddIceCandidate") {
        call().addIceCandidate(IceCandidate(sdpMid, sdpMLineIndex, candidate))
    }

    @SimpleFunction(description = "Creates a data channel for text messages. Create it before CreateOffer; " +
        "the other device gets it too, and DataChannelOpened fires on both when it's ready.")
    fun CreateDataChannel(label: String) = attempt("CreateDataChannel") { call().createDataChannel(label) }

    @SimpleFunction(description = "Sends text over an open data channel.")
    fun Send(label: String, text: String) = attempt("Send") {
        require(call?.send(label, text) == true) { "No open data channel named \"$label\"" }
    }

    @SimpleFunction(description = "Ends the connection. The other device's data channels close, and its " +
        "connection state changes once it notices.")
    fun Close() {
        remoteVideo.show(null)
        call?.close()
        call = null
        state = ConnectionState.Closed
    }

    @SimpleEvent(description = "The microphone and camera asked for in StartLocalMedia are ready.")
    fun LocalMediaStarted() {
        EventDispatcher.dispatchEvent(this, "LocalMediaStarted")
    }

    @SimpleEvent(description = "The other device's audio or video arrived. Kind is audio or video. Audio plays " +
        "by itself; show video with ShowRemoteVideo.")
    fun RemoteTrackAdded(kind: String) {
        EventDispatcher.dispatchEvent(this, "RemoteTrackAdded", kind)
    }

    @SimpleEvent(description = "The camera switched. Front is true when the front camera is now in use.")
    fun CameraSwitched(front: Boolean) {
        EventDispatcher.dispatchEvent(this, "CameraSwitched", front)
    }

    @SimpleEvent(description = "Your offer or answer is ready. Send type and sdp to the other device, which " +
        "passes them to SetRemoteDescription.")
    fun LocalDescriptionSet(type: String, sdp: String) {
        EventDispatcher.dispatchEvent(this, "LocalDescriptionSet", type, sdp)
    }

    @SimpleEvent(description = "A way to reach this device was found. Send all three values to the other " +
        "device, which passes them to AddIceCandidate.")
    fun IceCandidateFound(sdpMid: String, sdpMLineIndex: Int, candidate: String) {
        EventDispatcher.dispatchEvent(this, "IceCandidateFound", sdpMid, sdpMLineIndex, candidate)
    }

    @SimpleEvent(description = "The connection's state changed.")
    fun ConnectionStateChanged(@Options(ConnectionState::class) state: String) {
        EventDispatcher.dispatchEvent(this, "ConnectionStateChanged", state)
    }

    @SimpleEvent(description = "A data channel is ready to send and receive.")
    fun DataChannelOpened(label: String) {
        EventDispatcher.dispatchEvent(this, "DataChannelOpened", label)
    }

    @SimpleEvent(description = "A data channel closed.")
    fun DataChannelClosed(label: String) {
        EventDispatcher.dispatchEvent(this, "DataChannelClosed", label)
    }

    @SimpleEvent(description = "Text arrived on a data channel.")
    fun MessageReceived(label: String, text: String) {
        EventDispatcher.dispatchEvent(this, "MessageReceived", label, text)
    }

    @SimpleEvent(description = "A block or the connection failed. Without this event, Screen.ErrorOccurred " +
        "fires instead.")
    fun Error(operation: String, message: String) {
        EventDispatcher.dispatchEvent(this, "Error", operation, message)
    }

    override fun onPause() {
        camera?.stop()
    }

    override fun onResume() {
        camera?.start()
    }

    override fun onDestroy() {
        Close()
        StopLocalMedia()
        localVideo.release()
        remoteVideo.release()
    }

    private fun call(): Call = call ?: Call(WebRtc.factory(context), IceServers.parse(iceServers), listener).also {
        it.send(localTracks())
        call = it
    }

    private fun startLocalMedia(audio: Boolean, video: Boolean) {
        StopLocalMedia()
        val factory = WebRtc.factory(context)
        microphone = if (audio) Microphone(factory).also { it.track.setEnabled(microphoneEnabled) } else null
        camera = if (video) {
            Camera(context, factory, CaptureFormat(videoWidth, videoHeight, videoFrameRate), cameraListener)
                .also { it.track.setEnabled(cameraEnabled) }
        } else {
            null
        }
        localVideo.show(camera?.track)
        updateMirror()
        call?.send(localTracks())
        main.post { LocalMediaStarted() }
    }

    private fun localTracks(): List<MediaStreamTrack> = listOfNotNull(microphone?.track, camera?.track)

    private fun updateMirror() = localVideo.mirror(mirrorLocalVideo && camera?.front == true)

    private fun viewGroup(arrangement: AndroidViewComponent): ViewGroup =
        arrangement.view as? ViewGroup ?: throw IllegalArgumentException("Pass a HorizontalArrangement or VerticalArrangement")

    private fun descriptionType(type: String): SessionDescription.Type =
        SessionDescription.Type.entries.firstOrNull { it.canonicalForm() == type }
            ?: throw IllegalArgumentException("Unknown description type \"$type\": use offer or answer")

    private fun attempt(operation: String, action: () -> Unit) {
        try {
            action()
        } catch (e: IllegalArgumentException) {
            fail(operation, e.message.orEmpty())
        } catch (e: IllegalStateException) {
            fail(operation, e.message.orEmpty())
        }
    }

    private fun fail(operation: String, message: String) = main.post {
        if (!EventDispatcher.dispatchEvent(this, "Error", operation, message)) {
            form.ErrorOccurred(this, operation, ErrorMessages.ERROR_EXTENSION_ERROR, message)
        }
    }

    private val listener = object : CallListener {
        override fun onLocalDescription(description: SessionDescription) =
            LocalDescriptionSet(description.type.canonicalForm(), description.description)

        override fun onIceCandidate(candidate: IceCandidate) =
            IceCandidateFound(candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp)

        override fun onConnectionState(state: PeerConnectionState) {
            this@PeerConnection.state = connectionState(state)
            ConnectionStateChanged(this@PeerConnection.state.toUnderlyingValue())
        }

        override fun onRemoteTrack(track: MediaStreamTrack) {
            if (track is VideoTrack) remoteVideo.show(track)
            RemoteTrackAdded(track.kind())
        }

        override fun onChannelOpened(label: String) = DataChannelOpened(label)

        override fun onChannelClosed(label: String) = DataChannelClosed(label)

        override fun onMessage(label: String, text: String) = MessageReceived(label, text)

        override fun onError(operation: String, message: String) {
            fail(operation, message)
        }
    }

    private val cameraListener = object : CameraListener {
        override fun onCameraSwitched(front: Boolean) {
            updateMirror()
            CameraSwitched(front)
        }

        override fun onCameraError(message: String) {
            fail("Camera", message)
        }
    }

    private fun connectionState(state: PeerConnectionState) = when (state) {
        PeerConnectionState.NEW -> ConnectionState.New
        PeerConnectionState.CONNECTING -> ConnectionState.Connecting
        PeerConnectionState.CONNECTED -> ConnectionState.Connected
        PeerConnectionState.DISCONNECTED -> ConnectionState.Disconnected
        PeerConnectionState.FAILED -> ConnectionState.Failed
        PeerConnectionState.CLOSED -> ConnectionState.Closed
    }
}
