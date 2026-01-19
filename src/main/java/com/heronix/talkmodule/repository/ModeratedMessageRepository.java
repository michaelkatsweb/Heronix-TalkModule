package com.heronix.talkmodule.repository;

import com.heronix.talkmodule.model.domain.ModeratedMessage;
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
public interface ModeratedMessageRepository extends JpaRepository<ModeratedMessage, Long> {

    Optional<ModeratedMessage> findByMessageId(Long messageId);

    Optional<ModeratedMessage> findByMessageUuid(String uuid);

    @Query("SELECT m FROM ModeratedMessage m WHERE m.reviewed = false ORDER BY m.createdDate DESC")
    List<ModeratedMessage> findPendingReview();

    @Query("SELECT m FROM ModeratedMessage m WHERE m.reviewed = false ORDER BY m.createdDate DESC")
    Page<ModeratedMessage> findPendingReviewPaged(Pageable pageable);

    List<ModeratedMessage> findBySenderId(Long senderId);

    List<ModeratedMessage> findByChannelId(Long channelId);

    List<ModeratedMessage> findByModeratorId(Long moderatorId);

    @Query("SELECT m FROM ModeratedMessage m WHERE m.autoFlagged = true AND m.reviewed = false")
    List<ModeratedMessage> findAutoFlaggedPending();

    @Query("SELECT m FROM ModeratedMessage m WHERE m.userReported = true AND m.reviewed = false")
    List<ModeratedMessage> findUserReportedPending();

    @Query("SELECT m FROM ModeratedMessage m WHERE m.createdDate >= :since ORDER BY m.createdDate DESC")
    List<ModeratedMessage> findRecentModeration(@Param("since") LocalDateTime since);

    List<ModeratedMessage> findBySyncStatus(SyncStatus status);

    @Query("SELECT COUNT(m) FROM ModeratedMessage m WHERE m.reviewed = false")
    long countPendingReview();

    @Query("SELECT COUNT(m) FROM ModeratedMessage m WHERE m.deleted = true AND m.moderatedAt >= :since")
    long countDeletedSince(@Param("since") LocalDateTime since);
}
