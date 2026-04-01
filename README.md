# 📡 PowerBeam Connect

A modern Android application for scanning and connecting to Ubiquiti PowerBeam and other WiFi networks with support for point-to-point wireless connections.

## ✨ Features

- **🔍 WiFi Network Scanning** - Real-time scan of available networks with signal strength indicators
- **⭐ PowerBeam Detection** - Automatic detection and highlighting of 5GHz PowerBeam networks
- **📶 Signal Strength Display** - Visual emoji-based signal quality indicators
- **🔐 Multiple Security Protocols** - Support for WPA3, WPA2, WPA, and WEP networks
- **🎨 Dark Theme UI** - Eye-friendly interface with modern Material Design
- **⚡ Fast Connection** - One-tap connection with intuitive password dialog
- **🛡️ Smart Permissions** - Android 6.0+ compatible with proper runtime permissions

## 🚀 Requirements

- Android 8.0 (API 26) or higher
- WiFi capable device
- Location permission (required for WiFi scanning on Android 6.0+)
- WiFi state and change permissions

## 📦 Installation

### Build from Source

1. Clone the repository
```bash
git clone https://github.com/rroot4546-a11y/PowerBeamConnect.git
cd PowerBeamConnect
```

2. Build the APK
```bash
./gradlew assembleDebug
```

3. Install on device
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Build Release APK
```bash
./gradlew assembleRelease
```

## 🔧 How to Use

1. **Open the app** - Launch PowerBeam Connect
2. **Grant Permissions** - Allow WiFi and location permissions when prompted
3. **Scan Networks** - Click "🔄 Scan Again" to discover available networks
4. **Select Network** - Tap on any network to connect
5. **Enter Password** - Input the network password (if secured)
6. **Connect** - The app will attempt to establish a connection

## 🎯 Why Connection Fails?

If you see "Connection failed" on PowerBeam networks, here are common causes:

| Issue | Solution |
|-------|----------|
| **Wrong Password** | Verify credentials with network administrator |
| **MAC Filtering** | Check if device MAC is whitelisted on the tower |
| **Weak Signal** | Move closer to antenna or adjust device position for line-of-sight |
| **Full Capacity** | Tower may be at maximum client limit. Try again later |
| **Security Mismatch** | Ensure app detects correct security protocol (WPA2/WPA3) |
| **Missing Permissions** | Grant WiFi and location permissions in Settings |

## 🏗️ Architecture

### Components

- **MainActivity** - Main UI and user interaction handling
- **WifiScanManager** - Manages WiFi scanning and result processing
- **NetworkConnectionManager** - Handles network connection and callback logic
- **WifiAdapter** - RecyclerView adapter for displaying network list

### Dependencies

- **androidx.appcompat** - Android compatibility library
- **androidx.recyclerview** - List view component
- **androidx.cardview** - Card layout component
- **com.google.android.material** - Material Design components
- **kotlin coroutines** - Asynchronous task handling

## 🎨 UI Customization

### Colors
Edit `app/src/main/res/values/colors.xml`:

```xml
<color name="primary">#e94560</color>              <!-- Primary brand color -->
<color name="card_bg">#16213e</color>              <!-- Regular network card -->
<color name="powerbeam_bg">#1a3a5c</color>         <!-- PowerBeam highlight -->
```

### Strings
Edit `app/src/main/res/values/strings.xml` for localization.

## 📱 Screenshots

| Scanning | Connected | Network List |
|----------|-----------|--------------|
| ![scan](assets/scan.png) | ![connected](assets/connected.png) | ![list](assets/list.png) |

## 🔒 Security

- No network credentials are stored locally
- Passwords are only used for immediate connection
- All connections use standard Android network APIs
- No data collection or telemetry

## 🐛 Troubleshooting

### WiFi Scanning Not Working
- Ensure WiFi is enabled on the device
- Check that location permissions are granted
- Restart the app

### Connection Timeout
- Verify password is correct
- Check signal strength (move closer to antenna)
- Ensure device is within range of the network

### "Network Unavailable" Error
- This usually means invalid credentials or incompatible security protocol
- Try WPA2 networks first, then WPA3
- Contact network administrator for proper credentials

## 📝 Version History

### v1.1 (Current)
- Complete refactor with separated managers
- Improved error handling and logging
- Better WiFi scan timeout management
- Enhanced UI with PowerBeam badge
- Support for multiple security protocols
- Kotlin coroutines integration ready

### v1.0
- Initial release
- Basic WiFi scanning
- Simple connection interface

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Built for connecting to Ubiquiti PowerBeam networks
- Inspired by the need for reliable point-to-point connectivity
- Thanks to the Android developer community

## 📧 Support

For issues, questions, or suggestions:
- Open an issue on GitHub
- Contact the developer via the repository

---

**Made with ❤️ for reliable wireless connectivity**

Last updated: 2026-04-01 | Version: 1.1
