# WoL Remote (Android)

Wake-on-LAN app for Android. Wake your PC from anywhere.

## How to wake from outside your home network

**Option A - IPv4 + port forwarding**
1. On the PC: enable Wake-on-LAN in BIOS/UEFI and in the NIC driver ("Magic Packet" allowed).
2. Give the PC a static LAN IP.
3. On your router: forward a UDP port (default 9, or something random like 40000 for safety) to that LAN IP.
4. In the app: Host = your public IP or a DDNS name (e.g. mypc.duckdns.org), Port = forwarded port.

**Option B - IPv6 (recommended, no port-forward needed)**
1. Your PC must have a **global** IPv6 address (most ISPs provide one; check `ipconfig`).
2. Make the address stable: set a static/EUI-64 address in Windows NIC properties, or use an AAAA record in DDNS.
3. Allow inbound UDP port 9 through Windows Firewall.
4. In the app: tick **Prefer IPv6** and enter the PC's full IPv6 address as Host.

Note: some mobile carriers are IPv4-only CGNAT — IPv6 mode often works there where IPv4 cannot.
