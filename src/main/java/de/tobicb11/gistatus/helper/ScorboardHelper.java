package de.tobicb11.gistatus.helper;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.labymod.utils.Consumer;
import net.minecraft.network.play.server.SPacketTeams;

import de.tobicb11.gistatus.StatusThread;

public class ScorboardHelper implements Consumer<Object> {
    public static String currentServer = "none";
    public static String currentRealServer = "none";

    // Debounce-Scheduler für Leave-Events (verhindert sofortiges Stop bei kurzzeitigen leeren Paketen)
    private static final ScheduledExecutorService leaveScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "GIStatus-LeaveScheduler");
        t.setDaemon(true);
        return t;
    });
    private static ScheduledFuture<?> pendingLeaveTask = null;
    private static volatile long lastServerChangeMillis = 0L;

    public void accept(Object o) {
        if (o instanceof SPacketTeams) {
            try {
                SPacketTeams scoreboard = (SPacketTeams) o;
                String name = scoreboard.getName();
                String value = scoreboard.getPrefix();

                if (name.equals("server_value")) {
                    // value kann leer sein (z. B. beim Leave)
                    if (value == null || value.isEmpty()) {
                        long now = System.currentTimeMillis();
                        long sinceChange = now - lastServerChangeMillis;
                        // Wenn wir erst vor kurzem (innerhalb 5s) eine Serveränderung hatten, ignorieren wir kurzzeitige leere Pakete
                        if (sinceChange < 5000) {
                            return;
                        }

                        // Debounced Leave: plane einen Stop in 5 Sekunden; wenn in der Zeit ein neuer server_value kommt, brechen wir ab
                        if (pendingLeaveTask != null) {
                            pendingLeaveTask.cancel(false);
                        }
                        pendingLeaveTask = leaveScheduler.schedule(() -> {
                            currentRealServer = "none";
                            currentServer = "none";
                            StatusThread.stop();
                        }, 5, TimeUnit.SECONDS);
                        return;
                    }

                    // Wenn ein gültiger server_value ankommt, hebe einen eventuell geplanten Leave-Stop auf
                    if (pendingLeaveTask != null) {
                        pendingLeaveTask.cancel(false);
                        pendingLeaveTask = null;
                    }

                    String oldServer = currentServer;
                    currentRealServer = value;
                    currentServer = "none";
                    currentServer = value.replace("§f", "").toLowerCase(Locale.ROOT);
                    lastServerChangeMillis = System.currentTimeMillis();
                    if (!oldServer.equalsIgnoreCase(currentServer)) {
                        // Prüfe ob es ein CityBuild/CBX-Server ist
                        if (isTargetServer(currentServer)) {
                            // Starte sofortigen Send und wiederkehrendes Senden alle 45 Minuten
                            StatusThread.startForServer(currentServer);
                        } else {
                            // Falls wir zuvor einen tracked Server hatten, stoppen wir das
                            StatusThread.stop();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private boolean isTargetServer(String serverName) {
        if (serverName == null) return false;
        String lower = serverName.toLowerCase(Locale.ROOT);
        return lower.contains("citybuild") || lower.contains("cbx") || lower.contains("cb");
    }
}
