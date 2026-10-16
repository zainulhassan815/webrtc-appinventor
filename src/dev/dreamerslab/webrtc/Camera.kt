package dev.dreamerslab.webrtc

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.PeerConnectionFactory
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoTrack

internal class CaptureFormat(val width: Int, val height: Int, val frameRate: Int)

internal interface CameraListener {
    fun onCameraSwitched(front: Boolean)
    fun onCameraError(message: String)
}

internal class Camera(
    context: Context,
    factory: PeerConnectionFactory,
    private val format: CaptureFormat,
    private val listener: CameraListener,
) {
    private val main = Handler(Looper.getMainLooper())
    private val enumerator = Camera2Enumerator(context)
    private val name = enumerator.deviceNames.run { firstOrNull(enumerator::isFrontFacing) ?: firstOrNull() }
        ?: throw IllegalStateException("This device has no camera")
    private val capturer = enumerator.createCapturer(name, Events())
    private val textures = SurfaceTextureHelper.create("camera", WebRtc.egl.eglBaseContext)
    private val source = factory.createVideoSource(false)
    val track: VideoTrack = factory.createVideoTrack("video", source)
    var front = enumerator.isFrontFacing(name)
        private set

    init {
        capturer.initialize(textures, context, source.capturerObserver)
        start()
    }

    fun start() = capturer.startCapture(format.width, format.height, format.frameRate)

    fun stop() = capturer.stopCapture()

    fun switch() = capturer.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
        override fun onCameraSwitchDone(front: Boolean) {
            main.post {
                this@Camera.front = front
                listener.onCameraSwitched(front)
            }
        }

        override fun onCameraSwitchError(message: String) = report(message)
    })

    fun dispose() {
        capturer.stopCapture()
        capturer.dispose()
        track.dispose()
        source.dispose()
        textures.dispose()
    }

    private fun report(message: String) {
        main.post { listener.onCameraError(message) }
    }

    private inner class Events : CameraVideoCapturer.CameraEventsHandler {
        override fun onCameraError(message: String) = report(message)

        override fun onCameraDisconnected() = report("The camera was disconnected, usually because another app opened it")

        override fun onCameraFreezed(message: String) = report(message)

        override fun onCameraOpening(name: String) = Unit

        override fun onFirstFrameAvailable() = Unit

        override fun onCameraClosed() = Unit
    }
}
