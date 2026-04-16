package com.kitchensync.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.couchbase.lite.Collection;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.DatabaseConfiguration;
import com.couchbase.lite.ListenerToken;
import com.couchbase.lite.LogDomain;
import com.couchbase.lite.LogLevel;
import com.couchbase.lite.MutableDocument;
import com.couchbase.lite.MultipeerCertificateAuthenticator;
import com.couchbase.lite.MultipeerCollectionConfiguration;
import com.couchbase.lite.MultipeerReplicator;
import com.couchbase.lite.MultipeerReplicatorConfiguration;
import com.couchbase.lite.PeerInfo;
import com.couchbase.lite.TLSIdentity;
import com.couchbase.lite.KeyUsage;
import com.couchbase.lite.logging.ConsoleLogSink;
import com.couchbase.lite.logging.LogSinks;

import com.kitchensync.data.model.DeviceRole;
import com.kitchensync.util.Constants;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class CouchbaseManager {
    private static final String TAG = "CouchbaseManager";
    private static final String PREFS_NAME = "kitchensync_prefs";
    private static final String PREF_DEVICE_UUID = "device_uuid";
    private static final long DISCOVERY_BOOST_DELAY_MS = 3000;
    private static final long AUTO_RECOVERY_DELAY_MS = 2000;
    private static final int MAX_BOOST_ATTEMPTS = 2;

    private static CouchbaseManager instance;

    private Context context;
    private Database database;
    private Collection collection;
    private MultipeerReplicator replicator;
    private final List<ListenerToken> listenerTokens = new ArrayList<>();
    private String currentPeerId;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean peerFound = new AtomicBoolean(false);
    private final AtomicBoolean intentionallyStopped = new AtomicBoolean(false);
    private final AtomicBoolean boostInProgress = new AtomicBoolean(false);
    private int boostAttempts = 0;
    private volatile TLSIdentity cachedIdentity;
    private volatile boolean preWarmDone = false;
    private String persistentDeviceUuid;

    private CouchbaseManager() {}

    public static synchronized CouchbaseManager getInstance() {
        if (instance == null) {
            instance = new CouchbaseManager();
        }
        return instance;
    }

    public void init(Context appContext) {
        this.context = appContext.getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        persistentDeviceUuid = prefs.getString(PREF_DEVICE_UUID, null);
        if (persistentDeviceUuid == null) {
            persistentDeviceUuid = UUID.randomUUID().toString().substring(0, 8);
            prefs.edit().putString(PREF_DEVICE_UUID, persistentDeviceUuid).apply();
        }
    }

    /**
     * Pre-warm: open DB + create TLS identity on a background thread.
     * Called from Application.onCreate() so everything is ready before
     * RoleSelectionActivity appears.
     */
    public void preWarm() {
        executor.execute(() -> {
            try {
                long start = System.currentTimeMillis();
                openDatabase();
                cachedIdentity = getOrCreateIdentity();
                preWarmDone = true;
                long elapsed = System.currentTimeMillis() - start;
                Log.i(TAG, "Pre-warm complete in " + elapsed + "ms (DB + TLS identity ready)");
            } catch (Exception e) {
                Log.e(TAG, "Pre-warm failed", e);
                preWarmDone = true;
            }
        });
    }

    public void openDatabase() throws CouchbaseLiteException {
        if (database != null) return;

        LogSinks.get().setConsole(new ConsoleLogSink(
                LogLevel.VERBOSE, LogDomain.REPLICATOR, LogDomain.NETWORK));

        DatabaseConfiguration config = new DatabaseConfiguration();
        database = new Database(Constants.DATABASE_NAME, config);
        collection = database.getDefaultCollection();
        if (collection == null) {
            throw new CouchbaseLiteException("Failed to get default collection");
        }
        Log.i(TAG, "Database opened: " + Constants.DATABASE_NAME);
    }

    /**
     * Start P2P sync immediately (called before role selection).
     * Discovery begins right away so peers find each other while
     * the user is still on the role selection screen.
     */
    public void startPeerSyncEarly() throws Exception {
        if (replicator != null) {
            Log.w(TAG, "Peer sync already running");
            return;
        }

        // Wait for preWarm to finish (avoids creating duplicate TLS identities)
        if (!preWarmDone) {
            Log.i(TAG, "Waiting for preWarm to complete...");
            long waitStart = System.currentTimeMillis();
            while (!preWarmDone && (System.currentTimeMillis() - waitStart) < 10000) {
                Thread.sleep(50);
            }
            Log.i(TAG, "PreWarm wait finished in " + (System.currentTimeMillis() - waitStart) + "ms");
        }

        intentionallyStopped.set(false);
        createAndStartReplicator();
        Log.i(TAG, "MultipeerReplicator started EARLY. PeerId: " + currentPeerId);

        // Schedule discovery boost
        peerFound.set(false);
        boostAttempts = 0;
        scheduleDiscoveryBoost();
    }

    /**
     * Write device document after role selection.
     * P2P sync is already running from startPeerSyncEarly().
     */
    public void registerDeviceRole(String deviceName, DeviceRole role) {
        writeDeviceDocument(deviceName, role);
    }

    /**
     * Legacy method - starts P2P sync with role (used if early start wasn't called).
     */
    public void startPeerSync(String deviceName, DeviceRole role) throws Exception {
        startPeerSyncEarly();
        writeDeviceDocument(deviceName, role);
    }

    /**
     * Manual refresh: tear down current replicator and create a fresh one.
     * Forces a new mDNS broadcast + browse cycle to rediscover peers.
     */
    public void refreshPeerSync() {
        if (intentionallyStopped.get() || collection == null) return;
        Log.i(TAG, "Manual peer sync refresh requested");
        executor.execute(() -> {
            try {
                boostInProgress.set(true);
                tearDownReplicator();
                Thread.sleep(500);
                createAndStartReplicator();
                boostInProgress.set(false);
                peerFound.set(false);
                boostAttempts = 0;
                scheduleDiscoveryBoost();
                Log.i(TAG, "Peer sync refreshed. PeerId: " + currentPeerId);
            } catch (Exception e) {
                boostInProgress.set(false);
                Log.e(TAG, "Peer sync refresh failed", e);
            }
        });
    }

    public void stopPeerSync() {
        intentionallyStopped.set(true);
        tearDownReplicator();
    }

    public void close() {
        stopPeerSync();
        if (database != null) {
            try {
                database.close();
                Log.i(TAG, "Database closed");
            } catch (CouchbaseLiteException e) {
                Log.e(TAG, "Error closing database", e);
            }
            database = null;
            collection = null;
        }
    }

    public Database getDatabase() { return database; }
    public Collection getCollection() { return collection; }
    public String getCurrentPeerId() { return currentPeerId; }
    public MultipeerReplicator getReplicator() { return replicator; }

    public Set<String> getNeighborPeers() {
        Set<String> result = new HashSet<>();
        if (replicator != null) {
            try {
                Set<PeerInfo.PeerId> peers = replicator.getNeighborPeers();
                for (PeerInfo.PeerId peer : peers) {
                    result.add(peer.toString());
                }
            } catch (Exception e) {
                Log.w(TAG, "getNeighborPeers failed (replicator may be stopped)", e);
            }
        }
        return result;
    }

    public void resetDemo() throws CouchbaseLiteException {
        if (database != null) {
            database.delete();
            database = null;
            collection = null;
        }
    }

    public boolean isDatabaseReady() {
        return database != null && collection != null;
    }

    // --- Core replicator lifecycle ---

    /**
     * Creates a new MultipeerReplicator, registers listeners, and starts it.
     * MultipeerReplicator is one-shot: once stopped, it cannot be restarted.
     * Always call tearDownReplicator() before calling this again.
     */
    private void createAndStartReplicator() throws Exception {
        TLSIdentity identity = cachedIdentity != null ? cachedIdentity : getOrCreateIdentity();

        MultipeerCollectionConfiguration colConfig =
                new MultipeerCollectionConfiguration.Builder(collection).build();
        Set<MultipeerCollectionConfiguration> collections = new HashSet<>();
        collections.add(colConfig);

        MultipeerCertificateAuthenticator authenticator =
                new MultipeerCertificateAuthenticator((peer, certs) -> true);

        MultipeerReplicatorConfiguration config = new MultipeerReplicatorConfiguration.Builder()
                .setPeerGroupID(Constants.PEER_GROUP_ID)
                .setIdentity(identity)
                .setAuthenticator(authenticator)
                .setCollections(collections)
                .build();

        replicator = new MultipeerReplicator(config);
        registerListeners();
        replicator.start();

        PeerInfo.PeerId peerId = replicator.getPeerId();
        currentPeerId = peerId != null ? peerId.toString() : currentPeerId;
    }

    /**
     * Tears down the current replicator: removes listeners, stops, nulls out.
     */
    private void tearDownReplicator() {
        for (ListenerToken token : listenerTokens) {
            token.remove();
        }
        listenerTokens.clear();

        if (replicator != null) {
            replicator.stop();
            replicator = null;
            Log.i(TAG, "MultipeerReplicator stopped");
        }
    }

    // --- Private helpers ---

    /**
     * Returns the device-unique identity label. Each device gets its own
     * Keystore entry keyed by its persistent UUID, preventing PeerId collisions
     * across emulators/devices that share the same base system image.
     * Android Keystore entries survive pm clear, so using a device-unique label
     * ensures we never accidentally reuse another device's certificate.
     */
    private String getDeviceIdentityLabel() {
        return Constants.IDENTITY_LABEL + "." + persistentDeviceUuid;
    }

    private TLSIdentity getOrCreateIdentity() throws CouchbaseLiteException {
        String label = getDeviceIdentityLabel();
        String suffix = persistentDeviceUuid != null
                ? persistentDeviceUuid
                : UUID.randomUUID().toString().substring(0, 8);

        // Try to reuse existing identity for THIS device
        TLSIdentity existing = TLSIdentity.getIdentity(label);
        if (existing != null) {
            Log.i(TAG, "Reusing existing TLS identity (label: " + label + ")");
            return existing;
        }

        // Clean up any old identity under the generic label (from previous code versions)
        TLSIdentity.deleteIdentity(Constants.IDENTITY_LABEL);

        Map<String, String> attrs = new HashMap<>();
        attrs.put(TLSIdentity.CERT_ATTRIBUTE_COMMON_NAME,
                "KitchenSync-" + Build.MODEL + "-" + suffix);
        attrs.put(TLSIdentity.CERT_ATTRIBUTE_ORGANIZATION, "Kitchen Sync");

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 1);

        Set<KeyUsage> keyUsages = new HashSet<>();
        keyUsages.add(KeyUsage.CLIENT_AUTH);
        keyUsages.add(KeyUsage.SERVER_AUTH);

        TLSIdentity identity = TLSIdentity.createIdentity(
                keyUsages, attrs, cal.getTime(), label);
        Log.i(TAG, "Created TLS identity: KitchenSync-" + Build.MODEL + "-" + suffix
                + " (label: " + label + ")");
        return identity;
    }

    /**
     * Discovery boost: if no peers found within DISCOVERY_BOOST_DELAY_MS,
     * tear down old replicator and create a new one for fresh mDNS announcement.
     */
    private void scheduleDiscoveryBoost() {
        mainHandler.postDelayed(() -> {
            if (peerFound.get() || replicator == null || intentionallyStopped.get()) {
                Log.i(TAG, "Discovery boost: peers already found or stopped, skipping");
                return;
            }

            if (boostAttempts >= MAX_BOOST_ATTEMPTS) {
                Log.i(TAG, "Discovery boost: max attempts reached, relying on normal discovery");
                return;
            }

            boostAttempts++;
            Log.i(TAG, "Discovery boost #" + boostAttempts
                    + ": no peers found after " + DISCOVERY_BOOST_DELAY_MS
                    + "ms, creating new replicator for fresh mDNS announcement");

            executor.execute(() -> {
                try {
                    boostInProgress.set(true);
                    tearDownReplicator();
                    Thread.sleep(500);
                    createAndStartReplicator();
                    boostInProgress.set(false);
                    Log.i(TAG, "Discovery boost: new replicator started. PeerId: " + currentPeerId);
                    mainHandler.postDelayed(() -> scheduleDiscoveryBoost(),
                            DISCOVERY_BOOST_DELAY_MS);
                } catch (Exception e) {
                    Log.e(TAG, "Discovery boost failed", e);
                }
            });
        }, DISCOVERY_BOOST_DELAY_MS);
    }

    /**
     * Auto-recovery: if the replicator goes inactive unexpectedly (not from
     * intentional stop), automatically create a new one to restore P2P.
     * Handles cases like a peer emulator being killed and restarted.
     */
    private void scheduleAutoRecovery() {
        mainHandler.postDelayed(() -> {
            if (intentionallyStopped.get() || collection == null) {
                return;
            }

            // Check if replicator is dead or null
            if (replicator == null) {
                Log.i(TAG, "Auto-recovery: replicator is null, recreating");
                executor.execute(() -> {
                    try {
                        createAndStartReplicator();
                        peerFound.set(false);
                        boostAttempts = 0;
                        scheduleDiscoveryBoost();
                        Log.i(TAG, "Auto-recovery: new replicator started. PeerId: " + currentPeerId);
                    } catch (Exception e) {
                        Log.e(TAG, "Auto-recovery failed, will retry", e);
                        scheduleAutoRecovery();
                    }
                });
            }
        }, AUTO_RECOVERY_DELAY_MS);
    }

    private void registerListeners() {
        PeerEventBus bus = PeerEventBus.getInstance();

        // Replicator status -- includes auto-recovery on unexpected inactive
        ListenerToken statusToken = replicator.addStatusListener(status -> {
            boolean active = status.isActive();
            String error = status.getError() != null ? status.getError().getMessage() : null;
            Log.i(TAG, "Replicator status: " + (active ? "active" : "inactive") +
                    (error != null ? ", error: " + error : ""));
            bus.fireReplicatorStatus(active, error);

            // Auto-recovery: if replicator went inactive and we didn't stop it on purpose
            // and we're not in the middle of a discovery boost cycle
            if (!active && !intentionallyStopped.get() && !boostInProgress.get()) {
                Log.w(TAG, "Replicator went inactive unexpectedly -- scheduling auto-recovery");
                // Null out the dead replicator so auto-recovery creates a new one
                replicator = null;
                scheduleAutoRecovery();
            }
        });
        listenerTokens.add(statusToken);

        // Peer discovery
        ListenerToken discoveryToken = replicator.addPeerDiscoveryStatusListener(status -> {
            String peerId = status.getPeer().toString();
            boolean online = status.isOnline();
            Log.i(TAG, "Peer " + (online ? "discovered" : "lost") + ": " + peerId);
            if (online) {
                peerFound.set(true);
            }
            bus.firePeerDiscovered(peerId, online);
        });
        listenerTokens.add(discoveryToken);

        // Peer replicator status
        ListenerToken peerReplToken = replicator.addPeerReplicatorStatusListener(status -> {
            String peerId = status.getPeerId().toString();
            boolean outgoing = status.isOutgoing();
            String activity = status.getStatus().getActivityLevel().name().toLowerCase();
            String error = status.getStatus().getError() != null
                    ? status.getStatus().getError().getMessage() : null;
            Log.i(TAG, "Peer repl status: " + peerId + " " + activity +
                    (error != null ? " error: " + error : ""));
            bus.firePeerReplicatorStatus(peerId, outgoing, activity, error);
        });
        listenerTokens.add(peerReplToken);

        // Document replication
        ListenerToken docToken = replicator.addPeerDocumentReplicationListener(status -> {
            String peerId = status.getPeer().toString();
            boolean push = status.isPush();
            int count = status.getDocuments().size();
            Log.i(TAG, "Doc sync: " + peerId + " " + (push ? "push" : "pull") + " " + count + " docs");
            bus.fireDocumentSynced(peerId, push, count);
        });
        listenerTokens.add(docToken);
    }

    private void writeDeviceDocument(String deviceName, DeviceRole role) {
        try {
            String docId = "device::" + currentPeerId;
            MutableDocument doc = new MutableDocument(docId);
            doc.setString("type", Constants.DOC_TYPE_DEVICE);
            doc.setString("peerId", currentPeerId);
            doc.setString("deviceName", deviceName);
            doc.setString("role", role.name());
            doc.setString("joinedAt", java.time.Instant.now().toString());
            collection.save(doc);
            Log.i(TAG, "Device document written: " + docId);
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Error writing device document", e);
        }
    }
}
