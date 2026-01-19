package com.heronix.talkmodule.repository;

import com.heronix.talkmodule.model.domain.EmergencyAlert;
import com.heronix.talkmodule.model.enums.AlertLevel;
import com.heronix.talkmodule.model.enums.AlertType;
import com.heronix.talkmodule.model.enums.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmergencyAlertRepository extends JpaRepository<EmergencyAlert, Long> {

    Optional<EmergencyAlert> findByAlertUuid(String uuid);

    Optional<EmergencyAlert> findByServerId(Long serverId);

    List<EmergencyAlert> findByActiveTrue();

    @Query("SELECT a FROM EmergencyAlert a WHERE a.active = true " +
            "AND a.cancelledAt IS NULL " +
            "AND (a.expiresAt IS NULL OR a.expiresAt > :now) " +
            "ORDER BY a.alertLevel ASC, a.issuedAt DESC")
    List<EmergencyAlert> findActiveAlerts(@Param("now") LocalDateTime now);

    @Query("SELECT a FROM EmergencyAlert a WHERE a.active = true " +
            "AND a.cancelledAt IS NULL " +
            "AND a.alertLevel IN ('EMERGENCY', 'URGENT') " +
            "AND (a.expiresAt IS NULL OR a.expiresAt > :now)")
    List<EmergencyAlert> findActiveEmergencyAlerts(@Param("now") LocalDateTime now);

    @Query("SELECT a FROM EmergencyAlert a WHERE a.active = true " +
            "AND a.acknowledged = false " +
            "AND a.requiresAcknowledgment = true " +
            "AND a.cancelledAt IS NULL")
    List<EmergencyAlert> findUnacknowledgedAlerts();

    List<EmergencyAlert> findByAlertLevel(AlertLevel level);

    List<EmergencyAlert> findByAlertType(AlertType type);

    List<EmergencyAlert> findByIssuedById(Long issuerId);

    List<EmergencyAlert> findBySyncStatus(SyncStatus status);

    @Query("SELECT a FROM EmergencyAlert a ORDER BY a.issuedAt DESC")
    List<EmergencyAlert> findAllOrderByIssuedAtDesc();

    @Query("SELECT COUNT(a) FROM EmergencyAlert a WHERE a.active = true " +
            "AND a.cancelledAt IS NULL " +
            "AND (a.expiresAt IS NULL OR a.expiresAt > :now)")
    long countActiveAlerts(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(a) FROM EmergencyAlert a WHERE a.active = true " +
            "AND a.alertLevel = 'EMERGENCY' " +
            "AND a.cancelledAt IS NULL")
    long countActiveEmergencies();
}
