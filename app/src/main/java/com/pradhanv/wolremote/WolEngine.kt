package com.pradhanv.wolremote

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Inet6Address

object WolEngine {

    /**
     * Builds a standard magic packet: 6x 0xFF followed by the MAC repeated 16 times.
     * If a SecureOn password is set, it is appended (6 bytes) as AMD magic-packet w/ password.
     */
    private fun buildPacket(mac: String, secureOn: String?): ByteArray {
        val macBytes = parseMac(mac) ?: throw IllegalArgumentException("Invalid MAC address: $mac")
        val payload = ByteArray(6 + macBytes.size * 16 + if (secureOn != null) 6 else 0)
        for (i in 0 until 6) payload[i] = 0xFF.toByte()
        var idx = 6
        repeat(16) {
            macBytes.forEach { b -> payload[idx++] = b }
        }
        secureOn?.let {
            val pw = parseMac(it) ?: throw IllegalArgumentException("Invalid SecureOn password")
            pw.forEach { b -> payload[idx++] = b }
        }
        return payload
    }

    fun parseMac(mac: String): ByteArray? {
        val cleaned = mac.replace(":", "").replace("-", "").replace(".", "")
        if (cleaned.length != 12 || cleaned.any { it !in "0123456789abcdefABCDEF" }) return null
        return ByteArray(6) { i ->
            ((Character.digit(cleaned[i * 2], 16) shl 4)
                or Character.digit(cleaned[i * 2 + 1], 16)).toByte()
        }
    }

    sealed class WakeResult {
        data class Success(val via: String, val detail: String) : WakeResult()
        data class Failure(val message: String) : WakeResult()
    }

    /**
     * Sends the magic packet.
     *
     * IPv4 mode: UDP broadcast to [ip]:[port] (use your router port-forward to the PC's LAN IP,
     * or subnet-directed broadcast). Also tries a directed unicast as fallback.
     *
     * IPv6 mode: sends directly to the PC's global IPv6 address over UDP. No broadcast exists
     * in IPv6; the packet is unicast to the machine's global address, so that address must be
     * stable (static / EUI-64 / DDNS AAAA record). The socket is dual-stack capable and will
     * pick v6 automatically when the target resolves to an Inet6Address.
     */
    suspend fun wake(pc: PcEntry): WakeResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val packetData = buildPacket(pc.mac, pc.secureOn.ifBlank { null })
            val port = pc.port
            val results = mutableListOf<String>()

            // Resolve target - supports hostnames (DDNS), IPv4 dotted quads and full IPv6 literals
            val addresses: List<InetAddress> = try {
                InetAddress.getAllByName(pc.host.trim()).toList()
            } catch (e: Exception) {
                emptyList()
            }

            // 1) Try each resolved address (covers both A and AAAA records of a DDNS name)
            for (addr in addresses) {
                try {
                    DatagramSocket().use { sock ->
                        sock.broadcast = true
                        sock.reuseAddress = true
                        val isV6 = addr is Inet6Address
                        val target = addr.hostAddress
                        if (target == null) return@use
                        DatagramPacket(packetData, packetData.size, addr, port).let { sock.send(it) }
                        results.add(if (isV6) "IPv6 $target:$port" else "IPv4 $target:$port")
                    }
                } catch (_: Exception) { /* try next */ }
            }

            // 2) IPv4 broadcast fallback (255.255.255.255 only works on same LAN;
            //    subnet broadcast like 192.168.1.255 also attempted when host looks like a CIDR-less LAN IP)
            if (!pc.host.contains(":")) { // not an ipv6 literal
                try {
                    DatagramSocket().use { sock ->
                        sock.broadcast = true
                        val bc = InetAddress.getByName("255.255.255.255")
                        DatagramPacket(packetData, packetData.size, bc, port).let { sock.send(it) }
                        results.add("broadcast 255.255.255.255:$port")
                    }
                } catch (_: Exception) { /* ignore */ }
            }

            if (results.isEmpty()) {
                WakeResult.Failure("Could not reach ${pc.host}:$port. Check host/IP, port-forwarding, or the PC's firewall.")
            } else {
                WakeResult.Success(via = "sent", detail = results.joinToString(", "))
            }
        } catch (e: IllegalArgumentException) {
            WakeResult.Failure(e.message ?: "bad input")
        } catch (e: Exception) {
            WakeResult.Failure(e.message ?: e.javaClass.simpleName)
        }
    }
}
