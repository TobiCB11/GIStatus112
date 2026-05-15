package de.tobicb11.gistatus;

import de.tobicb11.gistatus.helper.ScorboardHelper;
import net.labymod.api.LabyModAddon;
import net.labymod.main.LabyMod;
import net.labymod.settings.elements.SettingsElement;

import java.util.List;

public class Main extends LabyModAddon {
    @Override
    public void onEnable() {
        System.out.println("[GIStatus] Addon enabled!");

        this.getApi().getEventManager().registerOnIncomingPacket(new ScorboardHelper());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            StatusThread.shutdown();

        }));
    }

    @Override
    public void loadConfig() {

    }

    @Override
    protected void fillSettings(List<SettingsElement> list) {

    }
}
