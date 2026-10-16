package dev.dreamerslab.webrtc

import com.google.appinventor.components.runtime.util.YailDictionary
import com.google.appinventor.components.runtime.util.YailList
import org.webrtc.PeerConnection

internal object IceServers {
    val googleStun: YailList = YailList.makeList(listOf(YailDictionary.makeDictionary("urls", "stun:stun.l.google.com:19302")))

    fun parse(servers: YailList): List<PeerConnection.IceServer> = servers.toArray().map(::server)

    private fun server(entry: Any?): PeerConnection.IceServer {
        require(entry is YailDictionary) { "Each ICE server is a dictionary with urls, and username and credential for TURN" }
        val urls = when (val value = entry["urls"]) {
            is YailList -> value.toStringArray().toList()
            is String -> listOf(value)
            else -> throw IllegalArgumentException("An ICE server has no urls")
        }
        return PeerConnection.IceServer.builder(urls)
            .setUsername(entry["username"]?.toString().orEmpty())
            .setPassword(entry["credential"]?.toString().orEmpty())
            .createIceServer()
    }
}
