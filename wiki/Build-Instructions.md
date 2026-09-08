# Rhythm Build Instructions

Complete guide to building Rhythm from source code. Whether you're contributing, customizing, or just curious, this guide will get you started.

---

## 📋 Prerequisites

### Required Software

#### 1. **Android Studio**
- **Version**: Ladybug or newer (2024.2+)
- **Download**: [Android Studio](https://developer.android.com/studio)
- **Components**: Android SDK, SDK Tools, Platform Tools

#### 2. **Java Development Kit (JDK)**
- **Version**: JDK 21 or newer (required for Gradle 9.x and AGP 9.x)
- **Download**: [Oracle JDK 21](https://www.oracle.com/java/technologies/downloads/#java21) or [OpenJDK 21](https://adoptium.net/temurin/releases/?version=21)

#### 3. **Git**
- **Version**: 2.30 or newer
- **Download**: [Git](https://git-scm.com/downloads)

### System Requirements

**Minimum:**
- **RAM**: 8 GB
- **Storage**: 10 GB free space
- **OS**: Windows 10/11, macOS 10.14+, or Linux (Ubuntu 18.04+)

**Recommended:**
- **RAM**: 16 GB
- **Storage**: 20 GB free space (SSD recommended)
- **CPU**: Multi-core processor

---

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/cromaguy/Rhythm.git
cd Rhythm
```

### 2. Open in Android Studio

1. Launch **Android Studio**
2. Select **Open an Existing Project**
3. Navigate to the cloned `Rhythm` folder
4. Click **OK**

### 3. Sync Gradle

Android Studio will automatically prompt to sync Gradle. If not:

1. Click **File** → **Sync Project with Gradle Files**
2. Wait for dependencies to download (~5-10 minutes first time)

### 4. Run the App

1. Connect Android device (Android 8.0+) via USB **OR** create an emulator
2. Enable **Developer Options** and **USB Debugging** on your device
3. Click the **Run** button (▶️) in Android Studio
4. Select your device/emulator
5. Wait for build to complete (~3-5 minutes first build)

---

## 🔧 Detailed Build Instructions

### Step 1: Configure JDK in Android Studio

Ensure correct JDK version (21+):

1. **File** → **Project Structure** → **SDK Location**
2. Under **JDK Location**, verify JDK 21 or newer is selected
3. If not, click **Download JDK** and select version 21

### Step 2: Configure Android SDK

Required SDK components:

1. **Tools** → **SDK Manager**
2. **SDK Platforms** tab:
   - ✅ Android API 37 - Compile SDK
   - ✅ Android 8.0 (API 26) - Minimum SDK
3. **SDK Tools** tab:
   - ✅ Android SDK Build-Tools 35.0.0
   - ✅ Android SDK Platform-Tools
   - ✅ Android SDK Command-line Tools
4. Click **Apply** and wait for downloads

### Step 3: Configure Gradle Properties

Create/edit `local.properties` in project root:

```properties
# Android SDK location (auto-generated)
sdk.dir=/path/to/Android/Sdk

# Optional: Gradle JVM arguments for better performance
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError
org.gradle.parallel=true
org.gradle.caching=true
```

### Step 4: Verify Build Configuration

Check `app/build.gradle.kts`:

```kotlin
android {
    namespace = "chromahub.rhythm.app"
    compileSdk = 37
    
    defaultConfig {
        applicationId = "chromahub.rhythm.app"
        minSdk = 26          // Android 8.0
        targetSdk = 37       // Android 15
        versionCode = 544561196
        versionName = "5.4.456.1196 Beta"
    }
    
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
            )
        }
    }
}
```

### Step 5: Build Variants

Rhythm supports multiple build variants:

#### **Debug Build** (Development)
```bash
# Specific flavor (recommended)
./gradlew assembleFdroidDebug
./gradlew assembleGithubDebug
```
- Output: `app/build/outputs/apk/fdroid/debug/` or `app/build/outputs/apk/github/debug/`
- Files are named `Rhythm-{versionName}-{flavorVariant}-{abi}.apk` (e.g., `Rhythm-5.4.456.1196 Beta-fdroidDebug-arm64-v8a.apk`)
- Features: Debugging enabled, logs enabled
- Signing: Debug keystore (auto-generated)

#### **Release Build** (Production)
```bash
# Specific flavor (recommended)
./gradlew assembleFdroidRelease
./gradlew assembleGithubRelease
```
- Output: `app/build/outputs/apk/fdroid/release/` or `app/build/outputs/apk/github/release/`
- Features: Optimized, R8/ProGuard enabled
- Signing: Requires signing configuration

---

## 🔐 Release Build Setup

### Generate Signing Key

```bash
keytool -genkey -v -keystore rhythm-release-key.jks \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -alias rhythm-key
```

**Important**: Store this keystore securely and remember the passwords!

### Configure Signing

Create `.config/keystore.properties` (do NOT commit this file):

```properties
store_file=../rhythm-release-key.jks
store_password=YourStorePassword
key_alias=rhythm-key
key_password=YourKeyPassword
```

The actual `app/build.gradle.kts` expects these specific snake_case property names and uses a `getProperties()` helper:

```kotlin
// In app/build.gradle.kts (top level)
val signingProperties = getProperties(".config/keystore.properties")
val releaseSigning = if (signingProperties != null) {
    signingConfigs.create("release") {
        keyAlias = signingProperties.property("key_alias")
        keyPassword = signingProperties.property("key_password")
        storePassword = signingProperties.property("store_password")
        storeFile = rootProject.file(signingProperties.property("store_file"))
    }
} else {
    signingConfigs.getByName("debug")
}

// Applied to build types:
release {
    signingConfig = releaseSigning
    isMinifyEnabled = true
    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
}
```

### Build Signed Release APK

```bash
./gradlew assembleGithubRelease
# or
./gradlew assembleFdroidRelease
```

Output: Flavor-specific directory — `app/build/outputs/apk/github/release/` or `app/build/outputs/apk/fdroid/release/`
APK naming: `Rhythm-{versionName}-{flavorVariant}-{abi}.apk`

---

## 🛠️ Command Line Build

### Using Gradle Wrapper (Recommended)

**Windows:**
```cmd
.\gradlew.bat clean assembleGithubDebug
```

**macOS/Linux:**
```bash
./gradlew clean assembleGithubDebug
```

### Common Gradle Tasks

```bash
# Clean build artifacts
./gradlew clean

# Build debug APK (specific flavor)
./gradlew assembleGithubDebug
./gradlew assembleFdroidDebug

# Build release APK (specific flavor)
./gradlew assembleGithubRelease
./gradlew assembleFdroidRelease

# All flavors at once
./gradlew assembleDebug
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run lint checks
./gradlew lint

# Install debug APK on connected device
./gradlew installGithubDebug
./gradlew installFdroidDebug
```

---

## 🧪 Building for Testing

### Enable Developer Options

1. **Settings** → **About Phone**
2. Tap **Build Number** 7 times
3. Enter PIN/password if prompted
4. **Developer Options** now available

### USB Debugging

1. **Settings** → **Developer Options**
2. Enable **USB Debugging**
3. Connect device via USB
4. Accept authorization dialog on device

### Verify Device Connection

```bash
adb devices
```

Expected output:
```
List of devices attached
ABC123456789    device
```

### Install APK Manually

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 🎯 Build Optimization

### Speed Up Builds

#### 1. **Increase Heap Size**

Edit `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx6144m -XX:MaxMetaspaceSize=1024m
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
```

#### 2. **Enable Gradle Build Cache**

```properties
android.enableBuildCache=true
android.buildCacheDir=~/.android/build-cache
```

#### 3. **Use Configuration Cache**

```bash
./gradlew --configuration-cache assembleDebug
```

### Troubleshoot Slow Builds

```bash
# Profile build performance
./gradlew assembleDebug --profile

# Scan report: build/reports/profile/
```

---

## ❌ Common Build Errors

### Error 1: SDK Not Found

**Error:**
```
SDK location not found. Define location with sdk.dir in the local.properties file
```

**Solution:**
Create `local.properties`:
```properties
sdk.dir=/path/to/Android/Sdk
```

### Error 2: JDK Version Mismatch

**Error:**
```
Unsupported class file major version 61
```

**Solution:**
Ensure JDK 21 or newer is configured in Android Studio (File → Project Structure → SDK Location).

### Error 3: Out of Memory

**Error:**
```
OutOfMemoryError: Java heap space
```

**Solution:**
Increase heap size in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx8192m
```

### Error 4: Dependencies Not Found

**Error:**
```
Could not resolve all dependencies
```

**Solution:**
1. Check internet connection
2. Sync Gradle: **File** → **Sync Project with Gradle Files**
3. Invalidate caches: **File** → **Invalidate Caches / Restart**

### Error 5: Build Tools Version

**Error:**
```
Failed to find Build Tools revision 35.0.0
```

**Solution:**
Install missing build tools via SDK Manager.

### Error 6: NDK Not Configured (if needed)

**Error:**
```
NDK not configured
```

**Solution:**
Download NDK via **SDK Manager** → **SDK Tools** → **NDK (Side by side)**

---

## 📦 Build Outputs

### Debug APK
- **Path**: `app/build/outputs/apk/{flavor}/debug/` (e.g., `.../fdroid/debug/`)
- **File**: `Rhythm-{versionName}-{flavor}Debug-{abi}.apk`
- **Size**: ~50-60 MB
- **Signing**: Debug keystore
- **Debuggable**: Yes
- **Optimized**: No

### Release APK
- **Path**: `app/build/outputs/apk/{flavor}/release/` (e.g., `.../fdroid/release/`)
- **File**: `Rhythm-{versionName}-{flavor}Release-{abi}.apk`
- **Size**: ~30-40 MB (R8/ProGuard optimized)
- **Signing**: Release keystore
- **Debuggable**: No
- **Optimized**: Yes (R8/ProGuard)

### Build Reports
- **Lint**: `app/build/reports/lint-results.html`
- **Test**: `app/build/reports/tests/`
- **Profile**: `build/reports/profile/`

---

## 🔍 Verify Build Integrity

### Check APK Signature

```bash
keytool -printcert -jarfile app-release.apk
```

### Inspect APK Contents

```bash
unzip -l app-release.apk
```

### APK Analyzer (Android Studio)

1. **Build** → **Analyze APK**
2. Select APK file
3. Review size, resources, DEX files

---

## 🌐 CI/CD Build

### GitHub Actions Example

```yaml
name: Build APK

on:
  push:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'adopt'
      
      - name: Grant execute permission for gradlew
        run: chmod +x gradlew
      
      - name: Build with Gradle
        run: ./gradlew assembleDebug
      
      - name: Upload APK
        uses: actions/upload-artifact@v3
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/app-debug.apk
```

---

## 🧹 Clean Build

### Full Clean

```bash
./gradlew clean
rm -rf .gradle
rm -rf build
rm -rf app/build
```

### Invalidate Caches (Android Studio)

**File** → **Invalidate Caches** → Check all options → **Invalidate and Restart**

---

## 📚 Additional Resources

- [Android Studio User Guide](https://developer.android.com/studio/intro)
- [Configure Your Build](https://developer.android.com/studio/build)
- [Build Variants](https://developer.android.com/studio/build/build-variants)
- [Sign Your App](https://developer.android.com/studio/publish/app-signing)
- [Gradle Documentation](https://docs.gradle.org/)

---

## 💬 Need Help?

- **Telegram**: [Rhythm Support](https://t.me/RhythmSupport)
- **Discord**: [Rhythm Community](https://discord.gg/XjPyUYPQYc)
- **GitHub Issues**: [Report Build Problems](https://github.com/cromaguy/Rhythm/issues)
- **Contributing Guide**: [https://github.com/cromaguy/Rhythm/wiki/Contributing](https://github.com/cromaguy/Rhythm/wiki/Contributing)

---

**Happy Building!** 🎉 If you encounter issues not covered here, please [open an issue](https://github.com/cromaguy/Rhythm/issues) so we can improve this guide.
