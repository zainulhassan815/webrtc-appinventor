package dev.dreamerslab.webrtc

import android.os.Handler
import android.os.Looper
import org.webrtc.AddIceObserver
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpSender
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import java.nio.ByteBuffer

internal interface CallListener {
    fun onLocalDescription(description: SessionDescription)
    fun onIceCandidate(candidate: IceCandidate)
    fun onConnectionState(state: PeerConnection.PeerConnectionState)
    fun onRemoteTrack(track: MediaStreamTrack)
    fun onChannelOpened(label: String)
    fun onChannelClosed(label: String)
    fun onMessage(label: String, text: String)
    fun onError(operation: String, message: String)
}

internal class Call(
    factory: PeerConnectionFactory,
    iceServers: List<PeerConnection.IceServer>,
    private val listener: CallListener,
) {
    private val main = Handler(Looper.getMainLooper())
    private val connection = checkNotNull(
        factory.createPeerConnection(PeerConnection.RTCConfiguration(iceServers), ConnectionObserver())
    ) { "WebRTC couldn't create a peer connection" }
    private val channels = mutableMapOf<String, DataChannel>()
    private val openChannels = mutableSetOf<String>()
    private val pendingCandidates = mutableListOf<IceCandidate>()
    private val senders = mutableListOf<RtpSender>()
    private var closed = false

    fun createOffer() = connection.createOffer(Created("CreateOffer"), MediaConstraints())

    fun createAnswer() = connection.createAnswer(Created("CreateAnswer"), MediaConstraints())

    fun setRemoteDescription(description: SessionDescription) =
        connection.setRemoteDescription(Applied("SetRemoteDescription", ::addPendingCandidates), description)

    fun addIceCandidate(candidate: IceCandidate) {
        if (connection.remoteDescription == null) pendingCandidates += candidate else add(candidate)
    }

    fun send(tracks: List<MediaStreamTrack>) {
        tracks.forEach { senders += connection.addTrack(it, listOf("local")) }
    }

    fun stopSending() {
        senders.forEach(connection::removeTrack)
        senders.clear()
    }

    fun createDataChannel(label: String) {
        connection.createDataChannel(label, DataChannel.Init())?.let(::observe)
    }

    fun send(label: String, text: String): Boolean {
        val channel = channels[label]?.takeIf { label in openChannels } ?: return false
        return channel.send(DataChannel.Buffer(ByteBuffer.wrap(text.toByteArray()), false))
    }

    fun close() {
        closed = true
        channels.values.forEach { channel ->
            channel.unregisterObserver()
            channel.close()
            channel.dispose()
        }
        channels.clear()
        connection.dispose()
    }

    private fun add(candidate: IceCandidate) = connection.addIceCandidate(candidate, object : AddIceObserver {
        override fun onAddSuccess() = Unit

        override fun onAddFailure(message: String) = onMain { listener.onError("AddIceCandidate", message) }
    })

    private fun addPendingCandidates() {
        pendingCandidates.forEach(::add)
        pendingCandidates.clear()
    }

    private fun observe(channel: DataChannel) {
        val label = channel.label()
        channel.registerObserver(ChannelObserver(channel, label))
        onMain { channels[label] = channel }
        if (channel.state() == DataChannel.State.OPEN) onMain { opened(label) }
    }

    private fun opened(label: String) {
        if (openChannels.add(label)) listener.onChannelOpened(label)
    }

    private fun closed(label: String) {
        if (openChannels.remove(label)) listener.onChannelClosed(label)
    }

    private fun onMain(action: () -> Unit) {
        main.post { if (!closed) action() }
    }

    private inner class ChannelObserver(private val channel: DataChannel, private val label: String) : DataChannel.Observer {
        override fun onBufferedAmountChange(previousAmount: Long) = Unit

        override fun onStateChange() {
            when (channel.state()) {
                DataChannel.State.OPEN -> onMain { opened(label) }
                DataChannel.State.CLOSED -> onMain { closed(label) }
                else -> Unit
            }
        }

        override fun onMessage(buffer: DataChannel.Buffer) {
            val text = ByteArray(buffer.data.remaining()).also(buffer.data::get).decodeToString()
            onMain { listener.onMessage(label, text) }
        }
    }

    private inner class Created(private val operation: String) : SdpObserver {
        override fun onCreateSuccess(description: SessionDescription) = onMain {
            connection.setLocalDescription(Applied("SetLocalDescription") { listener.onLocalDescription(description) }, description)
        }

        override fun onSetSuccess() = Unit

        override fun onCreateFailure(message: String) = onMain { listener.onError(operation, message) }

        override fun onSetFailure(message: String) = Unit
    }

    private inner class Applied(private val operation: String, private val onSuccess: () -> Unit) : SdpObserver {
        override fun onCreateSuccess(description: SessionDescription) = Unit

        override fun onSetSuccess() = onMain(onSuccess)

        override fun onCreateFailure(message: String) = Unit

        override fun onSetFailure(message: String) = onMain { listener.onError(operation, message) }
    }

    private inner class ConnectionObserver : PeerConnection.Observer {
        override fun onConnectionChange(state: PeerConnection.PeerConnectionState) =
            onMain { listener.onConnectionState(state) }

        override fun onIceCandidate(candidate: IceCandidate) = onMain { listener.onIceCandidate(candidate) }

        override fun onDataChannel(channel: DataChannel) = observe(channel)

        override fun onTrack(transceiver: RtpTransceiver) {
            val track = transceiver.receiver.track() ?: return
            onMain { listener.onRemoteTrack(track) }
        }

        override fun onSignalingChange(state: PeerConnection.SignalingState) = Unit

        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) = Unit

        override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit

        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) = Unit

        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) = Unit

        override fun onAddStream(stream: MediaStream) = Unit

        override fun onRemoveStream(stream: MediaStream) = Unit

        override fun onRenegotiationNeeded() = Unit
    }
}
