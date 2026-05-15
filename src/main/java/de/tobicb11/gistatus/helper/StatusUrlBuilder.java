package de.tobicb11.gistatus.helper;

public final class StatusUrlBuilder {

    private static final String STATUS_URL_PREFIX = "https://griefer.info/botshop-status-api/update?uuid=";

    public static String buildStatusUrl(String uuid) {
        assertInternalRuntime();
        return STATUS_URL_PREFIX + uuid;
    }

    private static void assertInternalRuntime() {
        if (!isLabyModRuntime()) {
            throw new IllegalStateException("Restricted runtime");
        }
        if (!isCalledFromSendStatus()) {
            throw new SecurityException("Restricted API");
        }
    }

    private static boolean isLabyModRuntime() {
        try {
            Class<?> labyModClass = Class.forName("net.labymod.main.LabyMod");
            Object instance = labyModClass.getMethod("getInstance").invoke(null);
            return instance != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isCalledFromSendStatus() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stack) {
            if ("de.tobicb11.gistatus.SendStatus".equals(element.getClassName())
                    && "sendStatus".equals(element.getMethodName())) {
                return true;
            }
        }
        return false;
    }
}

