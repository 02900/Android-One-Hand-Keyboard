package com.onehandkeyboard.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.concurrent.thread

class UpdateDiscoveryTest {
    private val loopback = InetAddress.getLoopbackAddress()

    @Test
    fun `answer gives the http port`() {
        assertEquals(8000, UpdateDiscovery.parseAnswer("OHK-UPDATES 8000\n"))
        assertNull(UpdateDiscovery.parseAnswer("OHK-UPDATES abc"))
        assertNull(UpdateDiscovery.parseAnswer("OHK-UPDATES 70000"))
        assertNull(UpdateDiscovery.parseAnswer("HELLO 8000"))
    }

    @Test
    fun `finds the server from its answer address`() {
        DatagramSocket(0, loopback).use { server ->
            val responder =
                thread {
                    val packet = DatagramPacket(ByteArray(64), 64)
                    server.receive(packet)
                    val question = String(packet.data, 0, packet.length)
                    if (question == UpdateDiscovery.QUESTION) {
                        val answer = "OHK-UPDATES 8123\n".toByteArray()
                        server.send(DatagramPacket(answer, answer.size, packet.socketAddress))
                    }
                }
            val discovery = UpdateDiscovery(server.localPort, { listOf(loopback) }, { true })

            assertEquals("http://127.0.0.1:8123", discovery.find(timeoutMs = 2000))
            responder.join()
        }
    }

    @Test
    fun `ignores answers from outside the local network`() {
        DatagramSocket(0, loopback).use { server ->
            thread {
                val packet = DatagramPacket(ByteArray(64), 64)
                server.receive(packet)
                val answer = "OHK-UPDATES 8123\n".toByteArray()
                server.send(DatagramPacket(answer, answer.size, packet.socketAddress))
            }
            val discovery = UpdateDiscovery(server.localPort, { listOf(loopback) }, { it.isSiteLocalAddress })

            assertNull(discovery.find(timeoutMs = 300))
        }
    }

    @Test
    fun `no server means null after the timeout`() {
        val unused = DatagramSocket(0, loopback).use { it.localPort }
        val discovery = UpdateDiscovery(unused, { listOf(loopback) }, { true })

        assertNull(discovery.find(timeoutMs = 200))
    }
}
