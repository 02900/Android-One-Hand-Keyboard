package com.onehandkeyboard.update

import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException

/**
 * Finds the update server on the local network without knowing its IP: sends a UDP broadcast
 * question and `tools/serve_updates.py` answers with its HTTP port. The answer's source address
 * is the PC's current IP, so it keeps working when the router gives the PC another one.
 */
class UpdateDiscovery(
    private val port: Int = PORT,
    private val targets: () -> List<InetAddress> = ::lanBroadcasts,
    private val accepts: (InetAddress) -> Boolean = { it.isSiteLocalAddress },
) {
    /** Base URL (`http://192.168.0.20:8000`), or null if nobody answered in time. */
    fun find(timeoutMs: Int = TIMEOUT_MS): String? =
        try {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                val question = QUESTION.toByteArray()
                targets().forEach { target -> send(socket, DatagramPacket(question, question.size, target, port)) }
                awaitAnswer(socket, System.currentTimeMillis() + timeoutMs)
            }
        } catch (_: IOException) {
            null
        }

    private fun send(
        socket: DatagramSocket,
        packet: DatagramPacket,
    ) {
        try {
            socket.send(packet)
        } catch (_: IOException) {
            // That interface can't broadcast; the others may.
        }
    }

    private fun awaitAnswer(
        socket: DatagramSocket,
        deadline: Long,
    ): String? {
        var server: String? = null
        try {
            while (server == null && System.currentTimeMillis() < deadline) {
                socket.soTimeout = (deadline - System.currentTimeMillis()).toInt().coerceAtLeast(1)
                val packet = DatagramPacket(ByteArray(BUFFER_BYTES), BUFFER_BYTES)
                socket.receive(packet)
                server = serverFrom(packet)
            }
        } catch (_: SocketTimeoutException) {
            // Nobody (else) answered.
        }
        return server
    }

    private fun serverFrom(packet: DatagramPacket): String? {
        val httpPort = parseAnswer(String(packet.data, 0, packet.length))
        val host = packet.address
        return if (httpPort != null && accepts(host)) "http://${host.hostAddress}:$httpPort" else null
    }

    companion object {
        const val PORT = 47800
        const val QUESTION = "OHK-UPDATES?\n"
        private const val ANSWER_PREFIX = "OHK-UPDATES "
        private const val TIMEOUT_MS = 1500
        private const val BUFFER_BYTES = 256
        private const val MAX_PORT = 65535

        /** "OHK-UPDATES 8000" → 8000; anything else → null. */
        fun parseAnswer(text: String): Int? =
            text
                .trim()
                .takeIf { it.startsWith(ANSWER_PREFIX) }
                ?.removePrefix(ANSWER_PREFIX)
                ?.toIntOrNull()
                ?.takeIf { it in 1..MAX_PORT }

        /** Broadcast address of every LAN interface (Wi-Fi), plus the generic one. */
        fun lanBroadcasts(): List<InetAddress> {
            val subnets =
                try {
                    NetworkInterface
                        .getNetworkInterfaces()
                        ?.toList()
                        .orEmpty()
                        .filter { it.isUp && !it.isLoopback }
                        .flatMap { it.interfaceAddresses }
                        .filter { it.address.isSiteLocalAddress }
                        .mapNotNull { it.broadcast }
                } catch (_: IOException) {
                    emptyList()
                }
            return (subnets + InetAddress.getByName("255.255.255.255")).distinct()
        }
    }
}
