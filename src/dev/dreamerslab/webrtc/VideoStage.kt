package dev.dreamerslab.webrtc

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup

internal class VideoStage(context: Context) : ViewGroup(context) {
    private val margin = (16 * resources.displayMetrics.density).toInt()
    private val insets = mutableSetOf<View>()

    fun add(view: View, inset: Boolean) {
        if (inset) insets += view
        addView(view)
    }

    override fun onViewRemoved(child: View) {
        insets -= child
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
            getDefaultSize(suggestedMinimumHeight, heightMeasureSpec),
        )
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            val frame = frame(child)
            child.measure(
                MeasureSpec.makeMeasureSpec(frame.width(), MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(frame.height(), MeasureSpec.AT_MOST),
            )
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            val frame = frame(child)
            val x = frame.left + (frame.width() - child.measuredWidth) / 2
            val y = frame.top + (frame.height() - child.measuredHeight) / 2
            child.layout(x, y, x + child.measuredWidth, y + child.measuredHeight)
        }
    }

    private fun frame(child: View): Rect {
        if (child !in insets || childCount == 1) return Rect(0, 0, measuredWidth, measuredHeight)
        val width = minOf(measuredWidth, measuredHeight) / 3
        val height = width * 4 / 3
        return Rect(measuredWidth - margin - width, measuredHeight - margin - height, measuredWidth - margin, measuredHeight - margin)
    }

    companion object {
        fun place(view: View, arrangement: ViewGroup, inset: Boolean) {
            remove(view)
            val stage = (0 until arrangement.childCount).map(arrangement::getChildAt).filterIsInstance<VideoStage>().firstOrNull()
                ?: VideoStage(arrangement.context).also {
                    arrangement.addView(it, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                }
            stage.add(view, inset)
        }

        fun remove(view: View) {
            val stage = view.parent as? VideoStage ?: return
            stage.removeView(view)
            if (stage.childCount == 0) (stage.parent as? ViewGroup)?.removeView(stage)
        }
    }
}
