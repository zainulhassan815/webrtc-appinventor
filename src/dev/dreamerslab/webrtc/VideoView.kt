package dev.dreamerslab.webrtc

import android.content.Context
import android.view.ViewGroup
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

internal class VideoView(private val context: Context, private val inset: Boolean) {
    private var renderer: SurfaceViewRenderer? = null
    private var track: VideoTrack? = null
    private var scaling = RendererCommon.ScalingType.SCALE_ASPECT_FILL
    private var mirror = false

    fun showIn(arrangement: ViewGroup) = VideoStage.place(renderer(), arrangement, inset)

    fun show(track: VideoTrack?) {
        renderer?.let { renderer ->
            this.track?.removeSink(renderer)
            if (track == null) renderer.clearImage() else track.addSink(renderer)
        }
        this.track = track
    }

    fun scale(fill: Boolean) {
        scaling = if (fill) RendererCommon.ScalingType.SCALE_ASPECT_FILL else RendererCommon.ScalingType.SCALE_ASPECT_FIT
        renderer?.setScalingType(scaling)
    }

    fun mirror(mirror: Boolean) {
        this.mirror = mirror
        renderer?.setMirror(mirror)
    }

    fun release() {
        val renderer = renderer ?: return
        track?.removeSink(renderer)
        VideoStage.remove(renderer)
        renderer.release()
        this.renderer = null
    }

    private fun renderer(): SurfaceViewRenderer = renderer ?: SurfaceViewRenderer(context).also { renderer ->
        renderer.init(WebRtc.egl.eglBaseContext, null)
        renderer.setZOrderMediaOverlay(inset)
        renderer.setScalingType(scaling)
        renderer.setMirror(mirror)
        track?.addSink(renderer)
        this.renderer = renderer
    }
}
