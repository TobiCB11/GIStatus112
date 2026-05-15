package de.tobicb11.gistatus;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;

public class StatusThread {

    private static final boolean DEBUG = true; // Auf false setzen für Produktion (45 Minuten)

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "GIStatus-Scheduler");
        t.setDaemon(true);
        return t;
    });
    private static final long INTERVAL_MINUTES = 45L;
    private static final long DEBUG_INTERVAL_SECONDS = 30L; // kurzer Intervall zum Testen

    private static ScheduledFuture<?> repeatingTask = null;
    private static String trackedServer = null;

    // Executor für die tatsächlichen Send-Aufrufe (damit Scheduler nicht blockiert)
    private static final ExecutorService senderExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "GIStatus-Sender");
        t.setDaemon(true);
        return t;
    });

    // Startet sofort einen Send und plant wiederholtes Senden alle INTERVAL_MINUTES für den angegebenen Server.
    public static synchronized void startForServer(String serverId) {
        if (serverId == null) return;
        if (trackedServer != null && trackedServer.equalsIgnoreCase(serverId) && repeatingTask != null && !repeatingTask.isCancelled()) {
            // Bereits gestartet für diesen Server
            return;
        }
        stop();
        trackedServer = serverId;

        final String serverSnapshot = serverId;

        // Sofortiges Senden (asynchron, über senderExecutor)
        senderExecutor.execute(() -> {
            try {
                invokeSendStatus();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        repeatingTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                senderExecutor.execute(() -> {
                    try {
                        invokeSendStatus();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, INTERVAL_MINUTES, INTERVAL_MINUTES, TimeUnit.MINUTES);

        if (repeatingTask != null) {
            try {
                long delaySec = repeatingTask.getDelay(TimeUnit.SECONDS);
            } catch (Exception e) {
            }
        }
    }

    // Stoppt das wiederkehrende Senden (z. B. beim Verlassen des Servers)
    public static synchronized void stop() {
        if (repeatingTask != null) {
            repeatingTask.cancel(false);
            repeatingTask = null;
        } else {
        }
        trackedServer = null;
    }

    // Sofortiges einmaliges Senden
    public static void sendNow() {
        senderExecutor.execute(() -> {
            try {
                invokeSendStatus();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // Zum sauberen Herunterfahren, falls benötigt (z. B. onDisable)
    public static synchronized void shutdown() {
        stop();
        try {
            scheduler.shutdownNow();
        } catch (Exception ignored) {
        }
        try {
            senderExecutor.shutdownNow();
        } catch (Exception ignored) {
        }
    }

    // Status-Abfrage, ob das wiederkehrende Task aktiv ist
    public static synchronized boolean isRepeatingActive() {
        return repeatingTask != null && !repeatingTask.isCancelled() && !repeatingTask.isDone();
    }

    // Versucht, die statische Methode de.tobicb11.GIStatus.SendStatus.sendStatus() per Reflection aufzurufen.
    private static void invokeSendStatus() {
        try {
            Class<?> GIStatusClass = Class.forName("de.tobicb11.gistatus.SendStatus");
            GIStatusClass.getMethod("sendStatus").invoke(null);
        } catch (ClassNotFoundException cnfe) {

        } catch (NoSuchMethodException nsme) {

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
