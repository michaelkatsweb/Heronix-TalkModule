package com.heronix.talkmodule.model.enums;

/**
 * Application connection modes.
 */
public enum ConnectionMode {
    CONNECTED,      // Connected to Heronix-Talk server
    OFFLINE,        // Working in standalone offline mode
    SYNCING,        // Currently synchronizing with server
    DISCONNECTED    // Was connected but lost connection
}
