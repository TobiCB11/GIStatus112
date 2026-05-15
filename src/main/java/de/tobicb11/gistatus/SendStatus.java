package de.tobicb11.gistatus;

import de.tobicb11.gistatus.helper.HttpRequestHelper;
import de.tobicb11.gistatus.helper.StatusUrlBuilder;
import net.labymod.main.LabyMod;

public class SendStatus {

    public static void sendStatus() {
        String uuid = LabyMod.getInstance().getPlayerUUID().toString();
        String requestUrl = StatusUrlBuilder.buildStatusUrl(uuid);
        HttpRequestHelper.requestURL(requestUrl);
        System.out.println("[GiStatus] Status gesendet.");
    }
}
