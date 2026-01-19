package com.heronix.talkmodule.model.enums;

/**
 * Types of communication channels.
 */
public enum ChannelType {
    PUBLIC,         // Open to all users
    PRIVATE,        // Invite-only channel
    DEPARTMENT,     // Department-specific channel
    DIRECT_MESSAGE, // One-on-one conversation
    GROUP_MESSAGE,  // Small group conversation
    ANNOUNCEMENT    // Read-only announcements channel
}
