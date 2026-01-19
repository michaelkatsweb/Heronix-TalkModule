package com.heronix.talkmodule.model.enums;

/**
 * Synchronization status for offline-first operations.
 */
public enum SyncStatus {
    PENDING,        // Item needs to be synced
    SYNCED,         // Item successfully synced
    CONFLICT,       // Data conflict detected
    LOCAL_ONLY      // Item exists only locally
}
