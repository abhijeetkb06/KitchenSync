# Kitchen Sync

**Peer-to-Peer QSR Order Sync with Zero Configuration**

A demo Android app showcasing [Couchbase Lite's MultipeerReplicator](https://docs.couchbase.com/couchbase-lite/current/android/p2psync-multipeer.html) for automatic device discovery and real-time data sync -- no server, no URLs, no manual setup. Devices on the same WiFi network find each other automatically and sync orders in real time.

Built to demonstrate how Couchbase Lite's P2P auto-discovery eliminates the need for server infrastructure in Quick Service Restaurant (QSR) operations.

---

## Screenshots

| Role Selection | Kiosk (Self-Order) | Kitchen Display | Store Manager | P2P Mesh |
|:-:|:-:|:-:|:-:|:-:|
| ![Role Selection](screenshots/role_selection.png) | ![Kiosk View](screenshots/kiosk_view.png) | ![Kitchen View](screenshots/kitchen_view.png) | ![Manager View](screenshots/manager_view.png) | ![Peers View](screenshots/peers_view.png) |

---

## What This Demonstrates

- **Automatic Device Discovery** -- Devices find each other via DNS-SD (Bonjour) with zero configuration
- **Real-Time P2P Sync** -- Orders sync instantly across all connected devices using MultipeerReplicator
- **Mesh Network** -- Every device syncs with every other device, no single point of failure
- **TLS Security** -- All peer communication is encrypted with self-signed certificates
- **Offline-First** -- Each device has its own local Couchbase Lite database; sync happens when peers are available
- **No Internet Required** -- Works entirely over local WiFi (airplane mode + WiFi ON)

---

## App Features

| Role | Description |
|------|-------------|
| **Kiosk** | Self-order station -- browse the Taco Bell menu, build a cart, enter customer name, place order |
| **Kitchen** | Real-time Kitchen Display System (KDS) -- order cards with food emojis, tap through status: New > Preparing > Ready > Picked Up |
| **Store Manager** | Dashboard with network status, order stats (New/Preparing/Ready/Picked Up), today's revenue, recent orders, and code spotlight |
| **Status Board** | Customer-facing order status display with "Preparing" and "Ready for Pickup" columns |
| **Peers** | Live P2P mesh visualization showing all connected devices and their roles |

All views are accessible from any device via the 5-tab bottom navigation (Kiosk / Status / Kitchen / Manager / Peers), regardless of which role was selected at launch.

### QSR Order Flow

```
Customer at Kiosk          Kitchen Make Line         Order Status Board        Store Manager
     |                          |                          |                        |
     |  1. Place Order          |                          |                        |
     |------------------------->|  2. Order appears         |                        |
     |                          |     automatically         |                        |
     |                          |------------------------->|  "Preparing"            |
     |                          |  3. Mark Ready            |                        |
     |                          |------------------------->|  "Ready for Pickup"     |
     |  4. Pick up order        |                          |                        |
     |                          |  5. Mark Picked Up        |                        |
     |                          |                          |----------------------->|  Revenue updated
```

**Same-customer orders are automatically appended** -- if the customer name matches an existing open order, new items are added to it instead of creating a duplicate.

---

## Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK 17** (required for Gradle 8.12)
- **Android Emulator v36.5.10+** (supports shared virtual WiFi for P2P between emulators)
- **Android SDK 34** with `google_apis` system image for `arm64-v8a`

---

## Quick Start (Step by Step)

### 1. Clone and Build

```bash
git clone https://github.com/abhijeetkb06/YumKitchenSync.git
cd YumKitchenSync
```

Verify JDK 17 is configured. Check `gradle.properties` and update the path if needed:
```properties
org.gradle.java.home=/Library/Java/JavaVirtualMachines/jdk-17.0.5.jdk/Contents/Home
```

Build the APK:
```bash
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### 2. Update Android Emulator

P2P discovery between emulators requires **Android Emulator v36.5.10+** with shared virtual WiFi support.

1. Open **Android Studio > Settings > Languages & Frameworks > Android SDK > SDK Tools**
2. Check **Android Emulator** and ensure it's **v36.5.10** or later
3. Click **Apply** to update if needed

### 3. Install System Image

Ensure the Android 14 system image with Google APIs is installed:

```bash
sdkmanager "system-images;android-34;google_apis;arm64-v8a"
```

Or install via Android Studio SDK Manager under **SDK Platforms > Android 14 > Google APIs ARM 64 v8a System Image**.

### 4. Create 3 AVDs (Android Virtual Devices)

Create three emulators representing different QSR devices:

```bash
# Using cmdline-tools (adjust path to your SDK)
AVDMANAGER=$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager

# Kiosk Phone (Pixel 5)
$AVDMANAGER create avd -n "KitchenSync_Kiosk" \
  -k "system-images;android-34;google_apis;arm64-v8a" \
  -d "pixel_5" --force <<< "no"

# Kitchen Tablet (Pixel C)
$AVDMANAGER create avd -n "KitchenSync_Kitchen_Tablet" \
  -k "system-images;android-34;google_apis;arm64-v8a" \
  -d "pixel_c" --force <<< "no"

# Manager Phone (Pixel 5)
$AVDMANAGER create avd -n "KitchenSync_Manager" \
  -k "system-images;android-34;google_apis;arm64-v8a" \
  -d "pixel_5" --force <<< "no"
```

Or create them via **Android Studio > Device Manager > Create Device**.

### 5. Launch All 3 Emulators

```bash
emulator -avd KitchenSync_Kiosk -no-snapshot-load -no-audio &
sleep 5
emulator -avd KitchenSync_Kitchen_Tablet -no-snapshot-load -no-audio &
sleep 5
emulator -avd KitchenSync_Manager -no-snapshot-load -no-audio &
```

Wait for all 3 to boot. Verify they're online:
```bash
adb devices
# Should show:
# emulator-5554  device
# emulator-5556  device
# emulator-5558  device
```

### 6. Install the App on All 3 Emulators

```bash
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5558 install -r app/build/outputs/apk/debug/app-debug.apk
```

### 7. Launch the App

```bash
for emu in emulator-5554 emulator-5556 emulator-5558; do
  adb -s $emu shell am start -n com.yumkitchen.sync/.ui.roleselection.RoleSelectionActivity
done
```

### 8. Select Roles and Grant Permissions

On each emulator:
1. Select a role (Kiosk / Kitchen / Store Manager)
2. Tap **"While using the app"** when prompted for location permission
3. The devices will auto-discover each other -- you'll see "N peers connected" in the toolbar

### 9. Test the Flow

1. **Kiosk**: Browse the menu (Tacos, Burritos, Sides, Drinks), add items to cart, enter a customer name, tap "Place Order"
2. **Kitchen**: Watch the order appear in real-time with food emojis -- tap "Start Preparing" > "Mark Ready" > "Picked Up"
3. **Place another order for the same customer name** -- items are appended to the existing open order
4. **Status Board tab**: See orders split into "Preparing" and "Ready for Pickup" columns
5. **Store Manager tab**: View dashboard with live order counts, today's revenue, and network status
6. **Peers tab**: See the live P2P mesh network with all connected devices

---

## Proving It Works Without Internet (Airplane Mode Demo)

To prove the app uses only local WiFi and not the internet:

### Option A: From the Emulator UI
1. On each emulator, **swipe down twice** from the top to open Quick Settings
2. Tap the **Airplane mode** tile to turn it ON
3. Tap the **Internet/WiFi** tile and turn WiFi back ON
4. The notification will say **"Wi-Fi on in airplane mode"**
5. WiFi shows connected to **"AndroidWifi"** -- but airplane mode blocks all internet
6. Place orders and watch them sync in real-time -- all over local WiFi

### Option B: Via ADB Commands
```bash
# Enable airplane mode + re-enable WiFi on all emulators
for emu in emulator-5554 emulator-5556 emulator-5558; do
  adb -s $emu shell cmd connectivity airplane-mode enable
  sleep 1
  adb -s $emu shell svc wifi enable
done

# Verify: all should show Airplane=1, WiFi connected to AndroidWifi
for emu in emulator-5554 emulator-5556 emulator-5558; do
  echo "=== $emu ==="
  adb -s $emu shell settings get global airplane_mode_on
  adb -s $emu shell cmd wifi status | grep -E "Wifi is |connected"
done

# Restore internet after demo
for emu in emulator-5554 emulator-5556 emulator-5558; do
  adb -s $emu shell cmd connectivity airplane-mode disable
done
```

### Key Talking Points
- **No server URL anywhere in the code** -- no `ws://`, no cloud endpoint, no Sync Gateway
- **MultipeerReplicator** only takes: a local database, a TLS identity, and a peer group name
- **DNS-SD (Bonjour)** discovers peers via multicast on the local WiFi subnet
- **TLS mutual authentication** secures all peer connections
- Orders sync in real-time even with airplane mode ON -- only local WiFi is needed

---

## Architecture

```
com.yumkitchen.sync/
+-- data/
|   +-- model/          # Order, OrderItem, MenuItem, DeviceRole
|   +-- repository/     # OrderRepository, MenuRepository (LiveQuery)
|   +-- CouchbaseManager.java   # Core: DB + MultipeerReplicator setup
|   +-- PeerEventBus.java       # Thread-safe observer for P2P events
+-- ui/
|   +-- roleselection/  # RoleSelectionActivity (launcher)
|   +-- main/           # MainActivity (5-tab bottom nav host)
|   +-- waiter/         # WaiterMenuFragment (Kiosk), MenuItemAdapter, CartAdapter
|   +-- kitchen/        # KitchenDisplayFragment, OrderCardAdapter
|   +-- statusboard/    # OrderStatusBoardFragment, StatusBoardAdapter
|   +-- discovery/      # PeerDiscoveryFragment, PeerMeshView, PeerListAdapter
|   +-- manager/        # ManagerDashboardFragment, OrderSummaryAdapter
+-- util/               # Constants, PermissionHelper, TimeUtils
+-- YumKitchenApp.java  # Application class
```

### Key P2P Components

**CouchbaseManager** is the core singleton that manages:
- Couchbase Lite database initialization
- TLS identity creation (self-signed cert with CLIENT_AUTH + SERVER_AUTH, unique per app launch)
- MultipeerReplicator configuration and lifecycle
- Event listeners for peer discovery, replication status, and document sync

```java
// The MultipeerReplicator setup -- this is all it takes
MultipeerReplicatorConfiguration config = new MultipeerReplicatorConfiguration.Builder()
    .setPeerGroupID("com.yumkitchen.sync")
    .setIdentity(identity)
    .setAuthenticator(new MultipeerCertificateAuthenticator((peer, certs) -> true))
    .setCollections(collections)
    .build();

MultipeerReplicator replicator = new MultipeerReplicator(config);
replicator.start();  // That's it -- devices auto-discover and sync
```

---

## Tech Stack

| Component | Version |
|-----------|---------|
| **Couchbase Lite Enterprise** | 4.0.3 |
| **Language** | Java |
| **Min SDK** | 29 (Android 10) |
| **Target SDK** | 34 (Android 14) |
| **AGP** | 8.7.3 |
| **Gradle** | 8.12 |
| **UI** | Android XML Views + Material Design Components |

---

## Permissions

| Permission | Purpose |
|-----------|---------|
| `INTERNET` | Network communication |
| `ACCESS_NETWORK_STATE` | Detect network connectivity |
| `ACCESS_WIFI_STATE` | WiFi network info for discovery |
| `CHANGE_WIFI_MULTICAST_STATE` | Enable multicast for DNS-SD discovery |
| `NEARBY_WIFI_DEVICES` | Android 13+ WiFi peer discovery |
| `ACCESS_FINE_LOCATION` | Required for WiFi scanning on Android 10-12 |
| `ACCESS_COARSE_LOCATION` | Required for WiFi scanning on Android 10-12 |

---

## Useful ADB Commands

```bash
# Clear app data on all emulators (fresh start)
for emu in emulator-5554 emulator-5556 emulator-5558; do
  adb -s $emu shell pm clear com.yumkitchen.sync
done

# Relaunch app on all emulators
for emu in emulator-5554 emulator-5556 emulator-5558; do
  adb -s $emu shell am start -n com.yumkitchen.sync/.ui.roleselection.RoleSelectionActivity
done

# Kill all emulators
pkill -9 -f "emulator.*avd"

# View Couchbase P2P logs
adb -s emulator-5554 logcat -s CouchbaseManager CouchbaseLite/REPLICATOR CouchbaseLite/NETWORK
```

---

## Couchbase Lite P2P Resources

- [MultipeerReplicator Documentation](https://docs.couchbase.com/couchbase-lite/current/android/p2psync-multipeer.html)
- [Couchbase Lite Android SDK](https://docs.couchbase.com/couchbase-lite/current/android/quickstart.html)
- [P2P Sync Examples (GitHub)](https://github.com/couchbaselabs/couchbase-lite-peer-to-peer-sync-examples)

---

## License

This is a demo application for Couchbase customer evaluation purposes. Couchbase Lite Enterprise Edition requires a valid license for production use.
