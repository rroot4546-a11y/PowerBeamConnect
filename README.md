# 📡 PowerBeam Connect

Android app for connecting to Ubiquiti PowerBeam and other point-to-point wireless networks.

## Features

- 📡 **Scan WiFi networks** - Shows all available networks with signal strength
- 🎯 **PowerBeam Detection** - Highlights 5GHz PowerBeam networks
- 🔐 **Easy Connection** - One-tap connect with password dialog
- 📊 **Signal Quality** - Visual signal strength indicator (emoji-based)
- 🎨 **Dark Theme** - Easy on the eyes

## Why Connection Fails

If you see "Connection failed" on PowerBeam networks, common causes:

1. **Wrong Password** - Most common. Double-check with network admin.
2. **MAC Filtering** - Some towers only allow registered devices.
3. **Weak Signal** - PowerBeam needs line-of-sight. Move closer or adjust antenna.
4. **Full Capacity** - Tower may be at max clients.

## Build

```bash
./gradlew assembleDebug
```

APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

---

Made with ❤️ for connecting to the internet!
