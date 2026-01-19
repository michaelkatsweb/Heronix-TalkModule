package com.heronix.talkmodule.model.enums;

/**
 * User roles within the Heronix TalkModule system.
 */
public enum UserRole {
    ADMIN,          // System administrator - full access
    PRINCIPAL,      // School principal - management access
    TEACHER,        // Teacher - standard staff access
    STAFF,          // Non-teaching staff
    COUNSELOR,      // School counselor
    DEPARTMENT_HEAD // Department head - additional permissions
}
