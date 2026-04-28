# Kitchen Sync

**Peer-to-Peer QSR Order Sync with Zero Configuration**

A demo Android app showcasing [Couchbase Lite's MultipeerReplicator](https://docs.couchbase.com/couchbase-lite/current/android/p2psync-multipeer.html) for automatic device discovery and real-time data sync -- no server, no URLs, no manual setup. Devices on the same WiFi network find each other automatically and sync orders in real time.

Built to demonstrate how Couchbase Lite's P2P auto-discovery eliminates the need for server infrastructure in Quick Service Restaurant (QSR) operations.

---

## Screenshots

| Role Selection | Kiosk (Self-Order) | Status Board | Kitchen Display | Store Manager | P2P Mesh |
|:-:|:-:|:-:|:-:|:-:|:-:|
| ![Role Selection](screenshots/01_role_selection.png) | ![Kiosk Menu](screenshots/02_kiosk_menu.png) | ![Status Board](screenshots/03_status_board.png) | ![Kitchen Display](screenshots/04_kitchen.png) | ![Manager Dashboard](screenshots/05_manager.png) | ![P2P Peers](screenshots/06_peers.png) |

---

## What This Demonstrates

- **Automatic Device Discovery** -- Devices find each other via DNS-SD with zero configuration
- **Real-Time P2P Sync** -- Orders sync instantly across all connected devices using MultipeerReplicator
- **Mesh Network** -- Every device syncs with every other device, no single point of failure
- **TLS Security** -- All peer communication is encrypted with self-signed certificates
- **Offline-First** -- Each device has its own local Couchbase Lite database; sync happens when peers are available
- **No Internet Required** -- Works entirely over local WiFi (airplane mode + WiFi ON)

---

## Why MultipeerReplicator? (New API vs Old Approach)

This app uses Couchbase Lite 4.0's **MultipeerReplicator** API, which replaces the older **URLEndpointListener + Replicator** pattern for P2P sync. The difference in developer effort is significant.

### Old Approach: URLEndpointListener + Replicator

With the previous WebSocket-based P2P API, developers had to build and manage every layer of the P2P stack manually:

```
Developer is responsible for:

1. Service Discovery    Write Android NSD (NsdManager) code to register and browse
                        for services on the local network
2. IP + Port Tracking   Resolve discovered services to get IP addresses and ports,
                        maintain a list of known peers
3. URL Construction     Build WebSocket URLs: wss://<ip>:<port>/<database>
4. Listener Setup       Configure and start a URLEndpointListener on one device
                        (the "server") to accept incoming connections
5. Replicator Setup     On every other device (the "clients"), create a Replicator
                        pointed at the listener's URL
6. Topology Design      Choose and implement a topology -- star, ring, mesh --
                        and manually manage which device connects to which
7. TLS Certificate      Generate certs, distribute them to peers, configure trust
    Exchange            validation on both listener and replicator sides
8. Reconnect Logic      Handle disconnections with exponential backoff, detect
                        dead peers, re-resolve addresses on IP changes
9. Connection Tracking  Track active connections, avoid duplicates, clean up
                        stale connections when devices leave
```

**Result:** ~75-100 lines of P2P plumbing code (Couchbase listener/replicator setup + Android NSD discovery + connection management), a rigid **star topology** (one listener, many clients), and a **single point of failure** -- if the listener device goes down, all sync stops.

### New Approach: MultipeerReplicator (What This App Uses)

With MultipeerReplicator, the entire P2P stack collapses into a single API call:

```java
// The ENTIRE P2P setup -- discovery, mesh, TLS, reconnect -- all handled automatically
MultipeerReplicatorConfiguration config = new MultipeerReplicatorConfiguration.Builder()
    .setPeerGroupID("com.kitchensync")       // Shared group name (like a WiFi SSID)
    .setIdentity(identity)                    // TLS cert (self-signed is fine)
    .setAuthenticator(authenticator)          // Trust validation callback
    .setCollections(collections)              // Which data to sync
    .build();

MultipeerReplicator replicator = new MultipeerReplicator(config);
replicator.start();  // Done. Devices auto-discover and form a mesh.
```

**Result:** ~30 lines of code, a **self-organizing mesh** topology, and **zero single points of failure**.

### What Gets Eliminated

| Responsibility | Old (URLEndpointListener) | New (MultipeerReplicator) |
|---|---|---|
| **Device Discovery** | Write NSD browse/register code manually | Automatic via DNS-SD |
| **IP/Port Management** | Resolve, track, and update peer addresses | Handled internally |
| **URL Construction** | Build `wss://ip:port/db` per peer | No URLs -- peer group ID only |
| **Topology** | Star only (1 listener, N clients) | Self-organizing mesh (all peers equal) |
| **Single Point of Failure** | Yes -- listener dies, sync stops | No -- mesh reroutes automatically |
| **TLS Setup** | Manual cert exchange between listener and clients | Mutual TLS built-in (just provide an identity) |
| **Reconnection** | Code exponential backoff and dead-peer detection | Auto-healing with rerouting |
| **Connection Tracking** | Maintain peer list, avoid duplicates, clean up stale | Managed by the replicator |
| **Lines of P2P Code** | ~75-100 | ~30 |

### Key Architectural Difference

```
OLD: Star Topology (fragile)              NEW: Mesh Topology (resilient)

     +--------+                           +--------+
     | Kiosk  |                           | Kiosk  |------+
     +---+----+                           +---+----+      |
         |                                    |           |
         v                                    v           v
    +---------+                          +---------+  +---------+
    |Listener |<----+                    | Kitchen |--| Manager |
    |(Kitchen)|     |                    +---------+  +---------+
    +---------+     |
         ^          |                    Every device syncs with every
         |          |                    other device. If one drops,
    +---------+     |                    the rest continue syncing.
    | Manager |-----+
    +---------+

    If the listener dies,
    ALL sync stops.
```

The MultipeerReplicator handles discovery, topology, security, and fault tolerance internally -- letting developers focus on the application logic instead of distributed systems plumbing.

---

## App Features

| Role | Description |
|------|-------------|
| **Kiosk** | Self-order station -- browse the Taco Bell menu, build a cart, enter customer name, place order |
| **Kitchen** | Real-time Kitchen Display System (KDS) -- order cards with food images, tap through status: New > Preparing > Ready > Picked Up |
| **Store Manager** | Dashboard with network status, order stats (New/Preparing/Ready/Picked Up), today's revenue, and recent orders |
| **Status Board** | Customer-facing order status display with three columns: New (blue), Preparing (orange), and Ready for Pickup (green) |
| **Peers** | Live P2P mesh visualization showing all connected devices, their roles, and code spotlight |

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
git clone https://github.com/abhijeetkb06/KitchenSync.git
cd KitchenSync
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
avdmanager create avd -n "KS_Kiosk_Phone" \
  -k "system-images;android-34;google_apis;arm64-v8a" \
  -d "pixel_4" --force <<< "no"

avdmanager create avd -n "KS_Kitchen_Tablet" \
  -k "system-images;android-34;google_apis;arm64-v8a" \
  -d "Nexus 9" --force <<< "no"

avdmanager create avd -n "KS_Manager_Phone" \
  -k "system-images;android-34;google_apis;arm64-v8a" \
  -d "pixel_4" --force <<< "no"
```

Or create them via **Android Studio > Device Manager > Create Device**.

### 5. Launch, Install, and Start

Use the included demo script to launch all 3 emulators, wait for boot, and start the app:

```bash
./launch_demo.sh
```

This launches `KS_Kiosk_Phone`, `KS_Kitchen_Tablet`, and `KS_Manager_Phone`, waits for them to boot, then opens the app on all 3. Verify they're online:

```bash
adb devices
# Should show:
# emulator-5554  device
# emulator-5556  device
# emulator-5558  device
```

To stop all emulators:
```bash
./kill_emulators.sh
```

### 6. Select Roles and Grant Permissions

On each emulator:
1. Select a role (Kiosk / Kitchen / Store Manager)
2. Tap **"While using the app"** when prompted for location permission
3. The devices will auto-discover each other -- you'll see "N peers connected" in the toolbar

### 7. Test the Flow

1. **Kiosk**: Browse the menu (Tacos, Burritos, Sides, Drinks), add items to cart, enter a customer name, tap "Place Order"
2. **Kitchen**: Watch the order appear in real-time with food images -- tap "Start Preparing" > "Mark Ready" > "Picked Up"
3. **Place another order for the same customer name** -- items are appended to the existing open order
4. **Status Board tab**: See orders flow through three columns -- "New" > "Preparing" > "Ready for Pickup"
5. **Store Manager tab**: View dashboard with live order counts, today's revenue, and network status
6. **Peers tab**: See the live P2P mesh network with all connected devices

---

## CBLite Browser (Database Viewer)

Inspect the Couchbase Lite databases on your running emulators with a Capella-style browser UI:

```bash
./launch_cblite_browser.sh
```

This auto-detects the app package (`applicationId` from `app/build.gradle`) and database name (`DATABASE_NAME` from source code), clones the [cblite-browser](https://github.com/abhijeetkb06/cblite-browser) tool on first run, and opens a live document viewer at `http://localhost:8091`.

**Prerequisites:** `cblite` CLI (`brew tap couchbase/tap && brew install cblite`), Python 3, running emulators with the app installed.

**Features:**
- Capella-style UI with scope/collection tree, document table, and JSON/Metadata modal
- Auto-refreshes from emulators every 3 seconds
- Shows version vectors for each document

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

## Architecture

```
com.kitchensync/
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
+-- KitchenSyncApp.java  # Application class
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
    .setPeerGroupID("com.kitchensync")
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
| `FOREGROUND_SERVICE` | Keep P2P sync alive when app is in background |
| `FOREGROUND_SERVICE_CONNECTED_DEVICE` | Android 14+ foreground service type for P2P |
| `WAKE_LOCK` | Prevent CPU sleep during active sync |
| `NEARBY_WIFI_DEVICES` | Android 13+ WiFi peer discovery |
| `ACCESS_FINE_LOCATION` | Required for WiFi scanning on Android 10-12 |
| `ACCESS_COARSE_LOCATION` | Required for WiFi scanning on Android 10-12 |

---

## Included Scripts

| Script | Description |
|--------|-------------|
| `launch_demo.sh` | Launches 3 emulator AVDs, waits for boot, and starts the app on all |
| `kill_emulators.sh` | Kills all running Android emulators |
| `launch_cblite_browser.sh` | Auto-detects app config, launches the [CBLite Browser](https://github.com/abhijeetkb06/cblite-browser) document viewer |

---

## Useful ADB Commands

```bash
# Clear app data on all emulators (fresh start)
for emu in emulator-5554 emulator-5556 emulator-5558; do
  adb -s $emu shell pm clear com.kitchensync
done

# Relaunch app on all emulators
for emu in emulator-5554 emulator-5556 emulator-5558; do
  adb -s $emu shell am start -n com.kitchensync/.ui.roleselection.RoleSelectionActivity
done

# Kill all emulators
./kill_emulators.sh

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
