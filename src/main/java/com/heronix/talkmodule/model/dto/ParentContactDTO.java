package com.heronix.talkmodule.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Parent/Guardian contact information DTO
 *
 * Used to display parent contact information and enable
 * communication from the TalkModule interface.
 *
 * @author Heronix TalkModule Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentContactDTO {

    private Long id;
    private String parentToken;

    // Basic Information
    private String firstName;
    private String lastName;
    private String relationshipType; // Mother, Father, Guardian, etc.

    // Contact Information
    private String email;
    private String cellPhone;
    private String homePhone;
    private String workPhone;
    private String preferredContactMethod; // EMAIL, PHONE, SMS, APP

    // Address (if different from student)
    private String address;
    private String city;
    private String state;
    private String zipCode;

    // Work Information
    private String employer;
    private String occupation;

    // Permissions
    private boolean hasLegalCustody;
    private boolean canPickUp;
    private boolean emergencyContact;
    private boolean receiveSchoolCommunication;
    private boolean receiveEmergencyAlerts;
    private boolean receiveTextNotifications;
    private boolean receiveEmailNotifications;

    // Contact Priority
    private int priority; // 1=Primary, 2=Secondary, etc.

    // Student Relationship
    private Long studentId;
    private String studentName;
    private String studentGrade;

    // Notification Preferences
    private NotificationPreferences notificationPreferences;

    /**
     * Get full name
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Get display label (name with relationship)
     */
    public String getDisplayLabel() {
        return getFullName() + " (" + relationshipType + ")";
    }

    /**
     * Get primary contact info based on preferred method
     */
    public String getPrimaryContactInfo() {
        if (preferredContactMethod == null) {
            return email != null ? email : cellPhone;
        }
        return switch (preferredContactMethod.toUpperCase()) {
            case "EMAIL" -> email;
            case "PHONE", "SMS" -> cellPhone != null ? cellPhone : homePhone;
            default -> email;
        };
    }

    /**
     * Check if parent can receive notifications
     */
    public boolean canReceiveNotifications() {
        return receiveSchoolCommunication &&
               (receiveEmailNotifications || receiveTextNotifications);
    }

    /**
     * Notification preferences for this parent
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NotificationPreferences {
        private boolean attendanceAlerts;
        private boolean gradeUpdates;
        private boolean behaviorReports;
        private boolean hallPassNotifications;
        private boolean emergencyAlerts;
        private boolean announcements;
        private boolean calendarEvents;

        // Quiet hours
        private String quietHoursStart; // e.g., "22:00"
        private String quietHoursEnd;   // e.g., "07:00"

        // Frequency
        private String digestFrequency; // REALTIME, DAILY, WEEKLY
    }

    /**
     * Student relationship summary
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentRelationship {
        private Long studentId;
        private String studentToken;
        private String firstName;
        private String lastName;
        private String grade;
        private String homeroom;
        private String relationshipType;

        public String getFullName() {
            return firstName + " " + lastName;
        }
    }

    /**
     * Contact summary for lists
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactSummary {
        private Long id;
        private String parentToken;
        private String fullName;
        private String relationshipType;
        private String email;
        private String phone;
        private int priority;
        private boolean canReceiveMessages;
        private List<StudentRelationship> students;
    }
}
