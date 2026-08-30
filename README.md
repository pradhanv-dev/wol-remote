# WoL Remote (Android)

Wake-on-LAN app for Android. Wake your PC from anywhere.

## ⚠️ Critical Limitation: Force Shutdown Does NOT Work

**WoL ONLY works if PC is in:**
- ✅ **Sleep** (S3) — NIC stays powered, recommended
- ✅ **Hibernate** (S4) — NIC stays powered
- ✅ **Soft Shutdown** (S5) — OS shut down normally, NIC still listening
- ❌ **Force Shutdown** — NIC loses power, WoL impossible
- ❌ **Hard power-off** (crash, power button held, unplugged)

**This is hardware-level, not fixable by the app.** The NIC must be powered to listen for packets.

### Workarounds for Force Shutdown

**Option 1: Use Sleep instead of Shutdown (Recommended)**
- Press Win+X → **Sleep** instead of Shut Down
- PC consumes ~10-15W vs ~2W in shutdown, but WoL works reliably
- Best for most users

**Option 2: Smart Power Plug** (for complete power-off + remote wake)
- Add a networked smart plug (TP-Link Kasa, Shelly, LIFX, etc.)
- Plug between wall outlet and PC power supply
- Send command to app → plug power-cycles → PC boots
- ⚠️ PC does a hard reboot (not graceful shutdown)
- Future: this app can integrate Smart Plug control

**Option 3: Low-Power Always-On Device** (Advanced)
- Raspberry Pi or Arduino with relay connected to PC power button
- Device stays powered, listens for commands
- App sends command → device presses power button → PC wakes
- Complex setup, overkill for most users

---

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

---

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
