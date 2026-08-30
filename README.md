# WoL Remote (Android)

Wake-on-LAN app for Android. Wake your PC from anywhere.

## Critical: PC must be in the right power state

**WoL ONLY works if:**
- ✅ PC is in **Sleep** (S3), **Hibernate** (S4), or **Soft Shutdown** (powered-off gracefully via OS)
- ❌ **NOT** after a hard power-off (force shutdown, power button held, unplugged, or crash)
- ❌ **NOT** if the power supply is fully disconnected

**If your PC is "turned off abruptly" or crashes, WoL cannot wake it.** The NIC loses power completely. This is a hardware/firmware limitation, not an app issue.

## PC Setup (Required once)

### Windows BIOS/UEFI
1. Restart and enter BIOS (usually F2, F10, DEL, or ESC during boot — see your motherboard manual).
2. Find **"Wake on LAN"**, **"WoL"**, or **"Power on by PCI-E"** setting.
3. Enable it and save.

### Windows Network Driver
1. Open Device Manager (`devmgmt.msc`).
2. Expand **Network adapters** and right-click your NIC.
3. Click **Properties** → **Advanced** tab.
4. Find **"Wake on Magic Packet"** or **"Wake on Pattern Match"** and set to **Enabled**.
5. Click **Power Management** tab.
6. ✓ Check **"Allow this device to wake the computer"**.
7. Click **OK**.

### Windows Power Settings
1. Open Power Settings (Win+X → Power options).
2. Click **Change what closing the lid does** (or **Change plan settings** → **Change advanced power settings**).
3. Ensure **"Allow wake timers"** or **"Wake timers"** is **Enabled** (or set to "Important wake timers").

### Test locally (same network)
1. Put PC to sleep (Win+X → Sleep).
2. From another device on the same network, send a WoL packet to the PC's LAN IP.
3. PC should wake. If not, go back and verify BIOS + driver settings.

## How to wake from outside your home network

**Option A - IPv4 + port forwarding**
1. Complete the PC setup above first.
2. Give the PC a static LAN IP.
3. On your router: forward a UDP port (default 9, or something random like 40000 for safety) to that LAN IP.
4. In the app: Host = your public IP or a DDNS name (e.g. mypc.duckdns.org), Port = forwarded port.

**Option B - IPv6 (recommended, no port-forward needed)**
1. Complete the PC setup above first.
2. Your PC must have a **global** IPv6 address (most ISPs provide one; check `ipconfig`).
3. Make the address stable: set a static/EUI-64 address in Windows NIC properties, or use an AAAA record in DDNS.
4. Allow inbound UDP port 9 through Windows Firewall (or your PC's firewall).
5. In the app: tick **Prefer IPv6** and enter the PC's full IPv6 address as Host.

**Note:** Some mobile carriers are IPv4-only CGNAT — IPv6 mode often works there where IPv4 cannot.
