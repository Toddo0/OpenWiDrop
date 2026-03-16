# OpenWiDrop

**OpenWiDrop** is a simple, lightweight HTTP file server for Android. It allows you to share files from your Android device over Wi‑Fi. Just select a folder, and any device on the same network can browse, download, or upload files using a web browser.

---

## ✨ Features

- **Simple & Clean Web Interface** – No complicated setup, just a familiar file browser.
- **Browse Folders & Files** – Navigate through your shared directory with ease.
- **Upload Multiple Files** – Select several files at once using the file picker or **drag & drop** them onto the upload area.
- **Download with One Click** – Click the download button next to any file.
- **QR Code Access** – The app automatically generates a QR code that points to the server URL – scan it with another device to connect instantly.
- **Remembers Last Folder** – Your selected folder is saved and restored the next time you open the app.
- **Android 5.0+ Support** – Works on Lollipop and newer versions.

---

## 📸 Screenshots

<a href="https://ibb.co/yms8Ny05"><img src="https://i.ibb.co/j94L8MhG/Screenshot-20260316-034801-Open-Wi-Drop.jpg" alt="Screenshot-20260316-034801-Open-Wi-Drop" border="0"></a>

---

## 📋 Requirements

- Android 5.0 (API 21) or higher
- Storage permission to read/write files in the selected folder

---

## 📦 Installation

### Download APK
You can download the latest APK from the [Releases](https://github.com/yourusername/openwidrop/releases) page. *(Replace with your actual link)*

### Build from Source

1. Clone this repository:
   ```bash
   git clone https://github.com/yourusername/openwidrop.git
   ```
2. Open the project in **Android Studio**.
3. Build and run on your device (or generate a signed APK).

---

## 🚀 Usage

1. Launch the app.
2. Grant storage permission when prompted (this is required to access your files).
3. Tap **Select Folder** to choose the directory you want to share.  
   *If you skip this step, the app defaults to your **Download** folder.*
4. The app will display a local URL (e.g., `http://192.168.1.10:8080`) and a QR code.
5. On another device connected to the **same Wi‑Fi network**, open that URL in a web browser, or scan the QR code.
6. You will see the contents of the shared folder. From there you can:
   - Click on folders to navigate inside them.
   - Click the download button (📥) next to any file to save it.
   - Upload files by clicking the upload area or dragging & dropping files onto it.
7. To change the port, edit the port number in the app and click **Update**.

---

## 🔐 Permissions

- **READ_EXTERNAL_STORAGE** & **WRITE_EXTERNAL_STORAGE** (Android 10 and below)  
  **MANAGE_EXTERNAL_STORAGE** (Android 11+)  
  These permissions are required to read and write files in the selected folder.  
  On Android 11+, the app will guide you to the system settings where you can grant “All files access”.

- **INTERNET**  
  Needed to start the local HTTP server.

---

## ⚙️ Technical Details

- Built with **[NanoHTTPD](https://github.com/NanoHttpd/nanohttpd)** – a tiny embeddable HTTP server written in Java.
- QR codes are generated on‑the‑fly using `android.graphics.Bitmap` and standard drawing APIs.
- Folder selection uses the Storage Access Framework (`ACTION_OPEN_DOCUMENT_TREE`) on newer Android versions, and falls back to a simple file chooser on older versions.
- No heavy dependencies – the whole app is under 100 KB (excluding NanoHTTPD).

---

## 📄 License

This project is licensed under the **MIT License**. See the [LICENSE](LICENSE) file for details.

---

## 🤝 Contributing

Contributions are welcome!  
If you find a bug or have an idea for an improvement, please open an issue or submit a pull request.

---

**Enjoy sharing files effortlessly!**
