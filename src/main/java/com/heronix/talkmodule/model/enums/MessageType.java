package com.heronix.talkmodule.model.enums;

/**
 * Types of messages in the system.
 */
public enum MessageType {
    TEXT,           // Regular text message
    FILE,           // File attachment
    IMAGE,          // Image attachment
    SYSTEM,         // System-generated message
    ANNOUNCEMENT,   // Important announcement
    REPLY,          // Reply to another message
    REACTION,       // Emoji reaction
    EDITED,         // Indicates message was edited
    DELETED         // Placeholder for deleted message
}
