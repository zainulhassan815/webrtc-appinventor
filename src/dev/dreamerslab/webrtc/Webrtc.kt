package dev.dreamerslab.webrtc

import com.google.appinventor.components.annotations.Extension
import com.google.appinventor.components.annotations.SimpleFunction
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent
import com.google.appinventor.components.runtime.ComponentContainer
import com.google.appinventor.components.runtime.util.YailList

@Extension(
        description = "Extension component for Webrtc. Built with <3 and Rush.",
        icon = "icon.png"
)
class Webrtc(
    private val container: ComponentContainer
) : AndroidNonvisibleComponent(container.`$form`()) {

    @SimpleFunction(description = "Returns the sum of the given list of integers.")
    fun SumAll(listOfInts: YailList): Int {
        return listOfInts.sumOf {
            it.toString().toIntOrNull() ?: 0
        }
    }
}
