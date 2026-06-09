# 🛠️ Building OpenTTY from Source Code

Before building OpenTTY, ensure you have:

- **J2ME SDK Mobile** installed on your target device
- **OpenTTY source code** (the complete repository)
- **Java-enabled mobile device** with adequate storage
- **Basic file management app** to navigate directories

## 🛠️ Build Process

### 📥 Step 1: Download SDK  
1. Download [`SDK.jar`](http://opentty.fun/dl/SDK.jar) to your mobile device
2. Transfer the file to your device's main storage or SD card

### 📁 Step 2: Access Repository
1. Open your file manager and navigate to the OpenTTY repository folder
2. Ensure all Java source files and resources are present

### 🔨 Step 3: Compile with SDK
1. **Launch** [`SDK.jar`](http://opentty.fun/dl/SDK.jar) on your device
2. **Browse** to the OpenTTY repository directory using the SDK's file browser
3. **Press the Build button** (identified by a hammer icon 🛠️)
4. Wait for the compilation process to complete

## 📦 Output Files

After successful compilation, check the `dist/` folder for:

- **`OpenTTY.jar`** - Main executable application file
- **`OpenTTY.jad`** - Java application descriptor file

## 📲 Installation

### 🎯 Method 1: Direct Installation
1. Locate `OpenTTY.jar` in the `dist/` directory
2. **Execute/Open** the JAR file
3. Follow your device's installation prompts
4. Launch OpenTTY from your applications menu

### 🔄 Method 2: JAD Installation
1. Some devices may prefer installation via JAD
2. Open `OpenTTY.jad` to begin installation
3. The system will automatically handle the JAR file

## ✅ Verification

After installation:
- Find "OpenTTY" in your applications list
- Launch the application to access the terminal interface
- You should see the OpenTTY command prompt ready for input

## 🔧 Troubleshooting

### ❌ Build Issues
- Verify all source files are present and accessible
- Check available memory on your device
- Ensure Java permissions are properly set

### ❌ Installation Problems
- Try the alternative installation method (JAD vs JAR)
- Confirm device compatibility with J2ME applications (MIDP-2.0 and CLDC-1.1)
- Check available storage space

### ❌ Launch Failures
- Verify Java ME support on your device
- Check application security permissions
- Reinstall if the application fails to start
