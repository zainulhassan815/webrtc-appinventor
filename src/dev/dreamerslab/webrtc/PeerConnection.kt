package dev.dreamerslab.webrtc

import android.os.Handler
import android.os.Looper
import com.google.appinventor.components.annotations.Extension
import com.google.appinventor.components.annotations.Options
import com.google.appinventor.components.annotations.SimpleEvent
import com.google.appinventor.components.annotations.SimpleFunction
import com.google.appinventor.components.annotations.SimpleProperty
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent
import com.google.appinventor.components.runtime.ComponentContainer
import com.google.appinventor.components.runtime.EventDispatcher
import com.google.appinventor.components.runtime.OnDestroyListener
import com.google.appinventor.components.runtime.util.ErrorMessages
import com.google.appinventor.components.runtime.util.YailList
import dev.dreamerslab.webrtc.helpers.ConnectionState
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection.PeerConnectionState
import org.webrtc.SessionDescription

@Extension(
    description = "A WebRTC connection to another device, with data channels for text messages. " +
        "Send the descriptions and ICE candidates it produces to the other device through any channel, " +
        "such as Supabase broadcast, and pass theirs back in.",
    icon = "icon.png",
)
class PeerConnection(container: ComponentContainer) : AndroidNonvisibleComponent(container.`$form`()), OnDestroyListener {

    private val context = container.`$context`()
    private val main = Handler(Looper.getMainLooper())
    private var iceServers = IceServers.googleStun
    private var state = ConnectionState.New
    private var call: Call? = null

    init {
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

    @SimpleFunction(description = "Starts a connection by creating an offer. LocalDescriptionSet fires with it: " +
        "send it to the other device. Create data channels first.")
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
        call?.close()
        call = null
        state = ConnectionState.Closed
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

    override fun onDestroy() = Close()

    private fun call(): Call =
        call ?: Call(WebRtc.factory(context), IceServers.parse(iceServers), listener).also { call = it }

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

        override fun onChannelOpened(label: String) = DataChannelOpened(label)

        override fun onChannelClosed(label: String) = DataChannelClosed(label)

        override fun onMessage(label: String, text: String) = MessageReceived(label, text)

        override fun onError(operation: String, message: String) {
            fail(operation, message)
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
