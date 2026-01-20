package com.heronix.talkmodule.service;

import com.heronix.talkmodule.model.domain.LocalChannel;
import com.heronix.talkmodule.model.domain.LocalMessage;
import com.heronix.talkmodule.model.dto.*;
import com.heronix.talkmodule.model.enums.ConnectionMode;
import com.heronix.talkmodule.model.enums.MessageType;
import com.heronix.talkmodule.model.enums.SyncStatus;
import com.heronix.talkmodule.network.TalkServerClient;
import com.heronix.talkmodule.repository.LocalChannelRepository;
import com.heronix.talkmodule.repository.LocalMessageRepository;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for chat operations - messages, channels, etc.
 * Works in both online and offline modes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final LocalChannelRepository channelRepository;
    private final LocalMessageRepository messageRepository;
    private final TalkServerClient serverClient;
    private final SessionManager sessionManager;

    @Getter
    private final ObservableList<LocalChannel> channels = FXCollections.observableArrayList();

    @Getter
    private final ObservableList<LocalMessage> currentMessages = FXCollections.observableArrayList();

    @Getter
    private LocalChannel selectedChannel;

    // ===================== Channels =====================

    @Transactional
    public void loadChannels() {
        if (sessionManager.isConnected()) {
            // Fetch user's channels (memberships) from server
            List<ChannelDTO> myChannels = serverClient.getMyChannels();
            for (ChannelDTO dto : myChannels) {
                LocalChannel local = convertToLocalChannel(dto);
                channelRepository.save(local);
            }

            // Also fetch public channels (in case auto-join hasn't happened yet)
            List<ChannelDTO> publicChannels = serverClient.getPublicChannels();
            for (ChannelDTO dto : publicChannels) {
                // Only save if not already in local cache
                if (channelRepository.findById(dto.getId()).isEmpty()) {
                    LocalChannel local = convertToLocalChannel(dto);
                    channelRepository.save(local);
                }
            }
        }

        // Load from local cache
        List<LocalChannel> localChannels = channelRepository.findByActiveTrueOrderByLastMessageTimeDesc();
        Platform.runLater(() -> {
            channels.clear();
            channels.addAll(localChannels);
        });

        log.info("Loaded {} channels", localChannels.size());
    }

    public void selectChannel(LocalChannel channel) {
        this.selectedChannel = channel;
        loadChannelMessages(channel.getId());

        // Join channel via WebSocket if connected
        if (sessionManager.isConnected()) {
            // WebSocket join handled elsewhere
        }
    }

    @Transactional
    public Optional<LocalChannel> createChannel(String name, String description, String icon) {
        if (sessionManager.isConnected()) {
            CreateChannelRequestDTO request = CreateChannelRequestDTO.builder()
                    .name(name)
                    .description(description)
                    .icon(icon)
                    .build();

            Optional<ChannelDTO> result = serverClient.createChannel(request);
            if (result.isPresent()) {
                LocalChannel local = convertToLocalChannel(result.get());
                local.setSyncStatus(SyncStatus.SYNCED);
                channelRepository.save(local);
                Platform.runLater(() -> channels.add(0, local));
                return Optional.of(local);
            }
        } else {
            // Create locally for offline use
            LocalChannel local = LocalChannel.builder()
                    .name(name)
                    .description(description)
                    .icon(icon)
                    .syncStatus(SyncStatus.LOCAL_ONLY)
                    .createdDate(LocalDateTime.now())
                    .build();
            channelRepository.save(local);
            Platform.runLater(() -> channels.add(0, local));
            return Optional.of(local);
        }

        return Optional.empty();
    }

    // ===================== Messages =====================

    @Transactional
    public void loadChannelMessages(Long channelId) {
        if (sessionManager.isConnected()) {
            // Fetch from server
            List<MessageDTO> serverMessages = serverClient.getChannelMessages(channelId, 0, 50);
            for (MessageDTO dto : serverMessages) {
                LocalMessage local = convertToLocalMessage(dto);
                if (messageRepository.findByMessageUuid(local.getMessageUuid()).isEmpty()) {
                    messageRepository.save(local);
                }
            }
        }

        // Load from local cache
        List<LocalMessage> messages = messageRepository.findByChannelIdOrderByTimestampAsc(channelId);
        Platform.runLater(() -> {
            currentMessages.clear();
            currentMessages.addAll(messages);
        });
    }

    @Transactional
    public Optional<LocalMessage> sendMessage(Long channelId, String content) {
        String clientId = UUID.randomUUID().toString();

        LocalMessage localMessage = LocalMessage.builder()
                .channelId(channelId)
                .senderId(sessionManager.getCurrentUserId())
                .senderName(sessionManager.getCurrentSession().getFullName())
                .content(content)
                .messageType(MessageType.TEXT)
                .timestamp(LocalDateTime.now())
                .clientId(clientId)
                .syncStatus(sessionManager.isConnected() ? SyncStatus.PENDING : SyncStatus.LOCAL_ONLY)
                .build();

        messageRepository.save(localMessage);
        Platform.runLater(() -> currentMessages.add(localMessage));

        if (sessionManager.isConnected()) {
            SendMessageRequestDTO request = SendMessageRequestDTO.builder()
                    .channelId(channelId)
                    .content(content)
                    .clientId(clientId)
                    .build();

            Optional<MessageDTO> result = serverClient.sendMessage(request);
            if (result.isPresent()) {
                localMessage.setServerId(result.get().getId());
                localMessage.setSyncStatus(SyncStatus.SYNCED);
                localMessage.setLastSyncTime(LocalDateTime.now());
                messageRepository.save(localMessage);
            }
        }

        // Update channel last message time
        channelRepository.findById(channelId).ifPresent(channel -> {
            channel.setLastMessageTime(LocalDateTime.now());
            channel.setMessageCount(channel.getMessageCount() + 1);
            channelRepository.save(channel);
        });

        return Optional.of(localMessage);
    }

    public void receiveMessage(MessageDTO messageDto) {
        // Handle incoming message from WebSocket
        if (messageRepository.findByMessageUuid(messageDto.getMessageUuid()).isEmpty()) {
            LocalMessage local = convertToLocalMessage(messageDto);
            local.setSyncStatus(SyncStatus.SYNCED);
            messageRepository.save(local);

            if (selectedChannel != null && selectedChannel.getId().equals(messageDto.getChannelId())) {
                Platform.runLater(() -> currentMessages.add(local));
            }
        }
    }

    @Transactional
    public boolean deleteMessage(Long localId) {
        return messageRepository.findById(localId).map(message -> {
            message.setDeleted(true);
            message.setContent("[Message deleted]");
            messageRepository.save(message);

            if (sessionManager.isConnected() && message.getServerId() != null) {
                serverClient.deleteMessage(message.getServerId());
            }

            return true;
        }).orElse(false);
    }

    // ===================== Sync =====================

    @Transactional
    public int syncPendingMessages() {
        if (!sessionManager.isConnected()) return 0;

        List<LocalMessage> pending = messageRepository.findNeedingSync();
        int synced = 0;

        for (LocalMessage message : pending) {
            SendMessageRequestDTO request = SendMessageRequestDTO.builder()
                    .channelId(message.getChannelId())
                    .content(message.getContent())
                    .clientId(message.getClientId())
                    .build();

            Optional<MessageDTO> result = serverClient.sendMessage(request);
            if (result.isPresent()) {
                message.setServerId(result.get().getId());
                message.setSyncStatus(SyncStatus.SYNCED);
                message.setLastSyncTime(LocalDateTime.now());
                messageRepository.save(message);
                synced++;
            }
        }

        log.info("Synced {} pending messages", synced);
        return synced;
    }

    // ===================== Helpers =====================

    private LocalChannel convertToLocalChannel(ChannelDTO dto) {
        return LocalChannel.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .channelType(dto.getChannelType())
                .icon(dto.getIcon())
                .creatorId(dto.getCreatorId())
                .creatorName(dto.getCreatorName())
                .memberCount(dto.getMemberCount())
                .messageCount(dto.getMessageCount())
                .active(dto.isActive())
                .archived(dto.isArchived())
                .pinned(dto.isPinned())
                .directMessageKey(dto.getDirectMessageKey())
                .lastMessageTime(dto.getLastMessageTime())
                .muted(dto.isMuted())
                .favorite(dto.isFavorite())
                .unreadCount(dto.getUnreadCount())
                .lastReadMessageId(dto.getLastReadMessageId())
                .createdDate(dto.getCreatedDate())
                .syncStatus(SyncStatus.SYNCED)
                .lastSyncTime(LocalDateTime.now())
                .build();
    }

    private LocalMessage convertToLocalMessage(MessageDTO dto) {
        return LocalMessage.builder()
                .serverId(dto.getId())
                .messageUuid(dto.getMessageUuid())
                .channelId(dto.getChannelId())
                .channelName(dto.getChannelName())
                .senderId(dto.getSenderId())
                .senderName(dto.getSenderName())
                .senderAvatar(dto.getSenderAvatar())
                .content(dto.getContent())
                .messageType(dto.getMessageType())
                .timestamp(dto.getTimestamp())
                .editedAt(dto.getEditedAt())
                .edited(dto.isEdited())
                .deleted(dto.isDeleted())
                .pinned(dto.isPinned())
                .important(dto.isImportant())
                .replyToId(dto.getReplyToId())
                .replyToPreview(dto.getReplyToPreview())
                .replyToSenderName(dto.getReplyToSenderName())
                .replyCount(dto.getReplyCount())
                .attachmentPath(dto.getAttachmentPath())
                .attachmentName(dto.getAttachmentName())
                .attachmentType(dto.getAttachmentType())
                .attachmentSize(dto.getAttachmentSize())
                .reactions(dto.getReactions())
                .mentions(dto.getMentions())
                .clientId(dto.getClientId())
                .syncStatus(SyncStatus.SYNCED)
                .lastSyncTime(LocalDateTime.now())
                .build();
    }

    public long getTotalUnreadCount() {
        Long count = channelRepository.getTotalUnreadCount();
        return count != null ? count : 0;
    }
}
