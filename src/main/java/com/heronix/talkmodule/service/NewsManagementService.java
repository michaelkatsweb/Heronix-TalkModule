package com.heronix.talkmodule.service;

import com.heronix.talkmodule.model.domain.LocalNewsItem;
import com.heronix.talkmodule.model.dto.NewsItemDTO;
import com.heronix.talkmodule.model.enums.SyncStatus;
import com.heronix.talkmodule.network.TalkServerClient;
import com.heronix.talkmodule.repository.LocalNewsItemRepository;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing news items and announcements.
 * Admin features for creating, scheduling, and managing news.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NewsManagementService {

    private final LocalNewsItemRepository newsRepository;
    private final TalkServerClient serverClient;
    private final SessionManager sessionManager;

    @Getter
    private final ObservableList<LocalNewsItem> newsItems = FXCollections.observableArrayList();

    @Getter
    private final ObservableList<LocalNewsItem> scheduledItems = FXCollections.observableArrayList();

    @Transactional
    public void loadNews() {
        if (sessionManager.isConnected()) {
            // Fetch from server and cache
            List<NewsItemDTO> serverNews = serverClient.getNews();
            for (NewsItemDTO dto : serverNews) {
                LocalNewsItem local = convertToLocalNews(dto);
                if (newsRepository.findByServerId(dto.getId()).isEmpty()) {
                    newsRepository.save(local);
                }
            }
        }

        // Load visible news
        List<LocalNewsItem> visible = newsRepository.findVisibleNews(LocalDateTime.now());
        Platform.runLater(() -> {
            newsItems.clear();
            newsItems.addAll(visible);
        });

        // Load scheduled news (admin view)
        List<LocalNewsItem> scheduled = newsRepository.findScheduledNews(LocalDateTime.now());
        Platform.runLater(() -> {
            scheduledItems.clear();
            scheduledItems.addAll(scheduled);
        });

        log.info("Loaded {} news items, {} scheduled", visible.size(), scheduled.size());
    }

    @Transactional
    public LocalNewsItem createNews(String headline, String content, String category,
                                     boolean urgent, boolean pinned) {
        LocalNewsItem news = LocalNewsItem.builder()
                .headline(headline)
                .content(content)
                .category(category)
                .authorId(sessionManager.getCurrentUserId())
                .authorName(sessionManager.getCurrentSession() != null ?
                        sessionManager.getCurrentSession().getFullName() : "Admin")
                .urgent(urgent)
                .pinned(pinned)
                .priority(urgent ? 100 : (pinned ? 50 : 0))
                .publishedAt(LocalDateTime.now())
                .syncStatus(sessionManager.isConnected() ? SyncStatus.PENDING : SyncStatus.LOCAL_ONLY)
                .build();

        newsRepository.save(news);
        Platform.runLater(() -> newsItems.add(0, news));

        // Sync to server if connected
        if (sessionManager.isConnected()) {
            syncNewsToServer(news);
        }

        log.info("Created news: {}", headline);
        return news;
    }

    @Transactional
    public LocalNewsItem scheduleNews(String headline, String content, String category,
                                       LocalDateTime scheduledAt, LocalDateTime expiresAt) {
        LocalNewsItem news = LocalNewsItem.builder()
                .headline(headline)
                .content(content)
                .category(category)
                .authorId(sessionManager.getCurrentUserId())
                .authorName(sessionManager.getCurrentSession() != null ?
                        sessionManager.getCurrentSession().getFullName() : "Admin")
                .scheduledAt(scheduledAt)
                .expiresAt(expiresAt)
                .published(true)
                .syncStatus(SyncStatus.LOCAL_ONLY)
                .build();

        newsRepository.save(news);
        Platform.runLater(() -> scheduledItems.add(news));

        log.info("Scheduled news: {} for {}", headline, scheduledAt);
        return news;
    }

    @Transactional
    public void updateNews(LocalNewsItem news) {
        news.setSyncStatus(SyncStatus.PENDING);
        newsRepository.save(news);

        if (sessionManager.isConnected()) {
            syncNewsToServer(news);
        }
    }

    @Transactional
    public void pinNews(Long newsId, boolean pinned) {
        newsRepository.findById(newsId).ifPresent(news -> {
            news.setPinned(pinned);
            news.setPriority(pinned ? 50 : 0);
            newsRepository.save(news);
            log.info("News {} {}", newsId, pinned ? "pinned" : "unpinned");
        });
    }

    @Transactional
    public void deactivateNews(Long newsId) {
        newsRepository.findById(newsId).ifPresent(news -> {
            news.setActive(false);
            newsRepository.save(news);
            Platform.runLater(() -> newsItems.remove(news));
            log.info("News deactivated: {}", newsId);
        });
    }

    private void syncNewsToServer(LocalNewsItem news) {
        try {
            NewsItemDTO dto = NewsItemDTO.builder()
                    .headline(news.getHeadline())
                    .content(news.getContent())
                    .category(news.getCategory())
                    .urgent(news.isUrgent())
                    .pinned(news.isPinned())
                    .build();

            Optional<NewsItemDTO> result;
            if (news.isUrgent()) {
                result = serverClient.createUrgentNews(news.getHeadline(), news.getContent());
            } else {
                result = serverClient.createNews(dto);
            }

            if (result.isPresent()) {
                news.setServerId(result.get().getId());
                news.setSyncStatus(SyncStatus.SYNCED);
                news.setLastSyncTime(LocalDateTime.now());
                newsRepository.save(news);
            }
        } catch (Exception e) {
            log.error("Failed to sync news to server", e);
        }
    }

    private LocalNewsItem convertToLocalNews(NewsItemDTO dto) {
        return LocalNewsItem.builder()
                .serverId(dto.getId())
                .headline(dto.getHeadline())
                .content(dto.getContent())
                .category(dto.getCategory())
                .authorId(dto.getAuthorId())
                .authorName(dto.getAuthorName())
                .priority(dto.getPriority())
                .active(dto.isActive())
                .pinned(dto.isPinned())
                .urgent(dto.isUrgent())
                .publishedAt(dto.getPublishedAt())
                .expiresAt(dto.getExpiresAt())
                .scheduledAt(dto.getScheduledAt())
                .linkUrl(dto.getLinkUrl())
                .imagePath(dto.getImagePath())
                .viewCount(dto.getViewCount())
                .syncStatus(SyncStatus.SYNCED)
                .lastSyncTime(LocalDateTime.now())
                .build();
    }

    public List<String> getAllCategories() {
        return newsRepository.findAllCategories();
    }

    public long getActiveNewsCount() {
        return newsRepository.countByActiveTrue();
    }
}
