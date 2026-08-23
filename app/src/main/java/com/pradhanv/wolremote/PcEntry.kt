package com.pradhanv.wolremote

import kotlinx.serialization.Serializable

@Serializable
data class PcEntry(
    val id: Long,
    val name: String,
    /** Hostname, DDNS name, public IPv4, or global IPv6 literal */
    val host: String,
    val mac: String,
    val port: Int = 9,
    /** Optional SecureOn password (hex, 12 chars) */
    val secureOn: String = "",
    /** Optional LAN IP (e.g. 192.168.29.50) - tried in parallel so one tap works at home AND away */
    val hostLan: String = "",
    /** true if host field contains an IPv6 literal or the PC uses IPv6/DDNS-with-AAAA */
    val useIpv6: Boolean = false,
)
