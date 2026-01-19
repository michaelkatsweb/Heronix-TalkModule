package com.heronix.talkmodule.repository;

import com.heronix.talkmodule.model.domain.LocalNewsItem;
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
public interface LocalNewsItemRepository extends JpaRepository<LocalNewsItem, Long> {

    Optional<LocalNewsItem> findByServerId(Long serverId);

    List<LocalNewsItem> findByActiveTrue();

    @Query("SELECT n FROM LocalNewsItem n WHERE n.active = true AND n.published = true " +
            "AND (n.expiresAt IS NULL OR n.expiresAt > :now) " +
            "AND (n.scheduledAt IS NULL OR n.scheduledAt <= :now) " +
            "ORDER BY n.priority DESC, n.publishedAt DESC")
    List<LocalNewsItem> findVisibleNews(@Param("now") LocalDateTime now);

    @Query("SELECT n FROM LocalNewsItem n WHERE n.active = true AND n.published = true " +
            "AND (n.expiresAt IS NULL OR n.expiresAt > :now) " +
            "AND (n.scheduledAt IS NULL OR n.scheduledAt <= :now) " +
            "ORDER BY n.priority DESC, n.publishedAt DESC")
    Page<LocalNewsItem> findVisibleNewsPaged(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT n FROM LocalNewsItem n WHERE n.pinned = true AND n.active = true AND n.published = true")
    List<LocalNewsItem> findPinnedNews();

    @Query("SELECT n FROM LocalNewsItem n WHERE n.urgent = true AND n.active = true AND n.published = true")
    List<LocalNewsItem> findUrgentNews();

    @Query("SELECT n FROM LocalNewsItem n WHERE n.scheduledAt IS NOT NULL AND n.scheduledAt > :now AND n.active = true")
    List<LocalNewsItem> findScheduledNews(@Param("now") LocalDateTime now);

    List<LocalNewsItem> findByCategory(String category);

    List<LocalNewsItem> findByAuthorId(Long authorId);

    List<LocalNewsItem> findBySyncStatus(SyncStatus status);

    @Query("SELECT DISTINCT n.category FROM LocalNewsItem n WHERE n.category IS NOT NULL ORDER BY n.category")
    List<String> findAllCategories();

    long countByActiveTrue();
}
