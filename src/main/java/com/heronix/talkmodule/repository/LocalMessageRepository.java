package com.heronix.talkmodule.repository;

import com.heronix.talkmodule.model.domain.LocalMessage;
import com.heronix.talkmodule.model.enums.SyncStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LocalMessageRepository extends JpaRepository<LocalMessage, Long> {

    Optional<LocalMessage> findByMessageUuid(String uuid);

    Optional<LocalMessage> findByServerId(Long serverId);

    @Query("SELECT m FROM LocalMessage m WHERE m.channelId = :channelId AND m.deleted = false ORDER BY m.timestamp DESC")
    Page<LocalMessage> findByChannelIdOrderByTimestampDesc(@Param("channelId") Long channelId, Pageable pageable);

    @Query("SELECT m FROM LocalMessage m WHERE m.channelId = :channelId AND m.deleted = false ORDER BY m.timestamp ASC")
    List<LocalMessage> findByChannelIdOrderByTimestampAsc(@Param("channelId") Long channelId);

    @Query("SELECT m FROM LocalMessage m WHERE m.channelId = :channelId AND m.timestamp > :since AND m.deleted = false ORDER BY m.timestamp ASC")
    List<LocalMessage> findByChannelIdAndTimestampAfter(@Param("channelId") Long channelId, @Param("since") LocalDateTime since);

    @Query("SELECT m FROM LocalMessage m WHERE m.channelId = :channelId AND m.pinned = true AND m.deleted = false")
    List<LocalMessage> findPinnedByChannelId(@Param("channelId") Long channelId);

    @Query(value = "SELECT * FROM local_messages m WHERE m.channel_id = :channelId AND m.deleted = false AND " +
            "LOWER(m.content) LIKE LOWER(CONCAT('%', :term, '%')) ORDER BY m.timestamp DESC",
            countQuery = "SELECT COUNT(*) FROM local_messages m WHERE m.channel_id = :channelId AND m.deleted = false AND " +
            "LOWER(m.content) LIKE LOWER(CONCAT('%', :term, '%'))",
            nativeQuery = true)
    Page<LocalMessage> searchInChannel(@Param("channelId") Long channelId, @Param("term") String term, Pageable pageable);

    @Query(value = "SELECT * FROM local_messages m WHERE m.deleted = false AND " +
            "LOWER(m.content) LIKE LOWER(CONCAT('%', :term, '%')) ORDER BY m.timestamp DESC",
            countQuery = "SELECT COUNT(*) FROM local_messages m WHERE m.deleted = false AND " +
            "LOWER(m.content) LIKE LOWER(CONCAT('%', :term, '%'))",
            nativeQuery = true)
    Page<LocalMessage> searchAll(@Param("term") String term, Pageable pageable);

    List<LocalMessage> findBySyncStatus(SyncStatus status);

    @Query("SELECT m FROM LocalMessage m WHERE m.syncStatus IN ('PENDING', 'LOCAL_ONLY')")
    List<LocalMessage> findNeedingSync();

    @Query("SELECT COUNT(m) FROM LocalMessage m WHERE m.channelId = :channelId AND m.deleted = false")
    long countByChannelId(@Param("channelId") Long channelId);

    boolean existsByClientId(String clientId);
}
