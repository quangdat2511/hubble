package com.hubble.service;

import com.hubble.dto.request.CreateServerRequest;
import com.hubble.dto.response.ChannelResponse;
import com.hubble.dto.response.ServerResponse;
import com.hubble.entity.Channel;
import com.hubble.entity.Server;
import com.hubble.entity.ServerMember;
import com.hubble.enums.ChannelType;
import com.hubble.exception.AppException;
import com.hubble.exception.ErrorCode;
import com.hubble.mapper.ChannelMapper;
import com.hubble.mapper.ServerMapper;
import com.hubble.repository.ChannelRepository;
import com.hubble.repository.ServerMemberRepository;
import com.hubble.repository.ServerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class ServerService {

    private static final String INVITE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int INVITE_CODE_LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml"
    );
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final ServerRepository serverRepository;
    private final ServerMemberRepository serverMemberRepository;
    private final ChannelRepository channelRepository;
    private final ServerMapper serverMapper;
    private final ChannelMapper channelMapper;
    private final String supabaseUrl;
    private final String serviceRoleKey;
    private final String serverIconBucket;

    public ServerService(
            ServerRepository serverRepository,
            ServerMemberRepository serverMemberRepository,
            ChannelRepository channelRepository,
            ServerMapper serverMapper,
            ChannelMapper channelMapper,
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.service-role-key}") String serviceRoleKey,
            @Value("${supabase.server-icon-bucket}") String serverIconBucket) {
        this.serverRepository = serverRepository;
        this.serverMemberRepository = serverMemberRepository;
        this.channelRepository = channelRepository;
        this.serverMapper = serverMapper;
        this.channelMapper = channelMapper;
        this.supabaseUrl = supabaseUrl;
        this.serviceRoleKey = serviceRoleKey;
        this.serverIconBucket = serverIconBucket;
    }

    public ServerResponse getServer(UUID serverId) {
        return serverMapper.toServerResponse(serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVER_NOT_FOUND)));
    }

    public List<ChannelResponse> getServerChannels(UUID serverId) {
        serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVER_NOT_FOUND));
        return channelRepository.findByServerId(serverId)
                .stream()
                .map(channelMapper::toChannelResponse)
                .toList();
    }

    public List<ServerResponse> getMyServers(UUID userId) {
        List<UUID> serverIds = serverMemberRepository.findAllByUserId(userId)
                .stream()
                .map(ServerMember::getServerId)
                .toList();
        return serverRepository.findAllById(serverIds)
                .stream()
                .map(serverMapper::toServerResponse)
                .toList();
    }

    @Transactional
    public ServerResponse createServer(UUID userId, CreateServerRequest request, MultipartFile iconFile) {
        String inviteCode = generateUniqueInviteCode();
        String iconUrl = (iconFile != null && !iconFile.isEmpty()) ? uploadIcon(iconFile) : null;

        Server server = Server.builder()
                .ownerId(userId)
                .name(request.getName())
                .description(request.getDescription())
                .iconUrl(iconUrl)
                .inviteCode(inviteCode)
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .build();

        server = serverRepository.save(server);

        serverMemberRepository.save(ServerMember.builder()
                .serverId(server.getId())
                .userId(userId)
                .build());

        createDefaultChannels(server.getId());
        return serverMapper.toServerResponse(server);
    }

    @Transactional
    public ServerResponse updateServerIcon(UUID userId, UUID serverId, MultipartFile iconFile) {
        Server server = getOwnedServer(userId, serverId);
        deleteIconQuietly(server.getIconUrl());
        server.setIconUrl(uploadIcon(iconFile));
        return serverMapper.toServerResponse(serverRepository.save(server));
    }

    @Transactional
    public ServerResponse removeServerIcon(UUID userId, UUID serverId) {
        Server server = getOwnedServer(userId, serverId);
        deleteIconQuietly(server.getIconUrl());
        server.setIconUrl(null);
        return serverMapper.toServerResponse(serverRepository.save(server));
    }

    private String uploadIcon(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new AppException(ErrorCode.INVALID_FILE_TYPE);
        String ct = file.getContentType();
        if (ct == null || !ALLOWED_IMAGE_TYPES.contains(ct)) throw new AppException(ErrorCode.INVALID_FILE_TYPE);

        try {
            String key = UUID.randomUUID() + "." + extension(file.getOriginalFilename());
            String uploadUrl = supabaseUrl + "/storage/v1/object/" + serverIconBucket + "/" + key;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl))
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("Content-Type", ct)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Supabase Storage upload failed [{}]: {}", response.statusCode(), response.body());
                throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
            }

            return supabaseUrl + "/storage/v1/object/public/" + serverIconBucket + "/" + key;
        } catch (IOException | InterruptedException e) {
            log.error("Failed to upload server icon", e);
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private void deleteIconQuietly(String iconUrl) {
        if (iconUrl == null || iconUrl.isBlank()) return;
        String prefix = supabaseUrl + "/storage/v1/object/public/" + serverIconBucket + "/";
        if (!iconUrl.startsWith(prefix)) return;

        try {
            String key = iconUrl.substring(prefix.length());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(supabaseUrl + "/storage/v1/object/" + serverIconBucket + "/" + key))
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .DELETE()
                    .build();
            HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            log.warn("Could not delete server icon: {}", iconUrl, e);
        }
    }

    private static String extension(String filename) {
        if (filename == null || !filename.contains(".")) return "bin";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private Server getOwnedServer(UUID userId, UUID serverId) {
        Server server = serverRepository.findById(serverId)
                .orElseThrow(() -> new AppException(ErrorCode.SERVER_NOT_FOUND));
        if (!server.getOwnerId().equals(userId)) throw new AppException(ErrorCode.NOT_SERVER_OWNER);
        return server;
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
            for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
                sb.append(INVITE_CODE_CHARS.charAt(RANDOM.nextInt(INVITE_CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (serverRepository.existsByInviteCode(code));
        return code;
    }

    private void createDefaultChannels(UUID serverId) {
        Channel chatCategory = channelRepository.save(Channel.builder()
                .serverId(serverId)
                .name("Kênh chat")
                .type(ChannelType.CATEGORY)
                .position((short) 0)
                .isPrivate(false)
                .build());

        channelRepository.save(Channel.builder()
                .serverId(serverId)
                .parentId(chatCategory.getId())
                .name("Kênh chung")
                .type(ChannelType.TEXT)
                .position((short) 0)
                .isPrivate(false)
                .build());

        Channel voiceCategory = channelRepository.save(Channel.builder()
                .serverId(serverId)
                .name("Kênh đàm thoại")
                .type(ChannelType.CATEGORY)
                .position((short) 1)
                .isPrivate(false)
                .build());

        channelRepository.save(Channel.builder()
                .serverId(serverId)
                .parentId(voiceCategory.getId())
                .name("Chung")
                .type(ChannelType.VOICE)
                .position((short) 0)
                .isPrivate(false)
                .build());
    }
}
