package com.heronix.talkmodule.repository;

import com.heronix.talkmodule.model.domain.LocalChannel;
import com.heronix.talkmodule.model.enums.ChannelType;
import com.heronix.talkmodule.model.enums.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocalChannelRepository extends JpaRepository<LocalChannel, Long> {

    List<LocalChannel> findByActiveTrue();

    List<LocalChannel> findByActiveTrueOrderByLastMessageTimeDesc();

    List<LocalChannel> findByChannelType(ChannelType type);

    List<LocalChannel> findByChannelTypeAndActiveTrue(ChannelType type);

    Optional<LocalChannel> findByDirectMessageKey(String dmKey);

    @Query("SELECT c FROM LocalChannel c WHERE c.channelType = 'PUBLIC' AND c.active = true ORDER BY c.name")
    List<LocalChannel> findPublicChannels();

    @Query("SELECT c FROM LocalChannel c WHERE c.channelType = 'ANNOUNCEMENT' AND c.active = true")
    List<LocalChannel> findAnnouncementChannels();

    @Query("SELECT c FROM LocalChannel c WHERE c.favorite = true AND c.active = true")
    List<LocalChannel> findFavoriteChannels();

    @Query("SELECT c FROM LocalChannel c WHERE c.unreadCount > 0 AND c.active = true")
    List<LocalChannel> findChannelsWithUnread();

    List<LocalChannel> findBySyncStatus(SyncStatus status);

    @Query("SELECT c FROM LocalChannel c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :term, '%')) AND c.active = true")
    List<LocalChannel> searchByName(@Param("term") String term);

    @Query("SELECT SUM(c.unreadCount) FROM LocalChannel c WHERE c.active = true")
    Long getTotalUnreadCount();
}
