package dev.dreamerslab.webrtc

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

internal object CallAudio {
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        .build()
    private val owners = mutableSetOf<Any>()
    private var audio: AudioManager? = null
    private var previousMode = AudioManager.MODE_NORMAL
    private var speakerphone = true

    fun activate(context: Context, owner: Any, active: Boolean) {
        val wasActive = owners.isNotEmpty()
        if (active) owners += owner else owners -= owner
        if (!wasActive && owners.isNotEmpty()) start(context.getSystemService(Context.AUDIO_SERVICE) as AudioManager)
        if (wasActive && owners.isEmpty()) stop()
    }

    fun speakerphone(on: Boolean) {
        speakerphone = on
        audio?.let(::route)
    }

    private fun start(audio: AudioManager) {
        this.audio = audio
        previousMode = audio.mode
        audio.requestAudioFocus(focus)
        audio.mode = AudioManager.MODE_IN_COMMUNICATION
        route(audio)
    }

    private fun stop() {
        val audio = audio ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) audio.clearCommunicationDevice() else setSpeakerphoneOn(audio, false)
        audio.mode = previousMode
        audio.abandonAudioFocusRequest(focus)
        this.audio = null
    }

    private fun route(audio: AudioManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val speaker = audio.availableCommunicationDevices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            if (speakerphone && speaker != null) audio.setCommunicationDevice(speaker) else audio.clearCommunicationDevice()
        } else {
            setSpeakerphoneOn(audio, speakerphone)
        }
    }

    @Suppress("DEPRECATION")
    private fun setSpeakerphoneOn(audio: AudioManager, on: Boolean) {
        audio.isSpeakerphoneOn = on
    }
}
