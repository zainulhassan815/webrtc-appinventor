package dev.dreamerslab.webrtc

import org.webrtc.AudioTrack
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnectionFactory

internal class Microphone(factory: PeerConnectionFactory) {
    private val source = factory.createAudioSource(MediaConstraints())
    val track: AudioTrack = factory.createAudioTrack("audio", source)

    fun dispose() {
        track.dispose()
        source.dispose()
    }
}
