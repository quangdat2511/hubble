package com.hubble.controller;

import com.hubble.dto.common.ApiResponse;
import com.hubble.dto.response.SearchAttachmentResponse;
import com.hubble.dto.response.SearchChannelResponse;
import com.hubble.dto.response.SearchMemberResponse;
import com.hubble.dto.response.SearchMessageResponse;
import com.hubble.security.UserPrincipal;
import com.hubble.service.SearchService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SearchController {

    SearchService searchService;

    // ─────────────────────────────────────────────────────────────────────────
    // Channel scope
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/channel/{channelId}/messages")
    public ApiResponse<Page<SearchMessageResponse>> searchChannelMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String channelId,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<Page<SearchMessageResponse>>builder()
                .result(searchService.searchChannelMessages(
                        principal.getId().toString(), channelId, q, page, size))
                .build();
    }

    @GetMapping("/channel/{channelId}/members")
    public ApiResponse<List<SearchMemberResponse>> searchChannelMembers(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String channelId,
            @RequestParam(defaultValue = "") String q
    ) {
        return ApiResponse.<List<SearchMemberResponse>>builder()
                .result(searchService.searchChannelMembers(
                        principal.getId().toString(), channelId, q))
                .build();
    }

    @GetMapping("/channel/{channelId}/media")
    public ApiResponse<List<SearchAttachmentResponse>> searchChannelMedia(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String channelId
    ) {
        return ApiResponse.<List<SearchAttachmentResponse>>builder()
                .result(searchService.searchChannelMedia(principal.getId().toString(), channelId))
                .build();
    }

    @GetMapping("/channel/{channelId}/files")
    public ApiResponse<List<SearchAttachmentResponse>> searchChannelFiles(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String channelId
    ) {
        return ApiResponse.<List<SearchAttachmentResponse>>builder()
                .result(searchService.searchChannelFiles(principal.getId().toString(), channelId))
                .build();
    }

    @GetMapping("/channel/{channelId}/pins")
    public ApiResponse<List<SearchMessageResponse>> searchChannelPins(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String channelId,
            @RequestParam(defaultValue = "") String q
    ) {
        return ApiResponse.<List<SearchMessageResponse>>builder()
                .result(searchService.searchChannelPins(
                        principal.getId().toString(), channelId, q))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Server scope
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/server/{serverId}/messages")
    public ApiResponse<Page<SearchMessageResponse>> searchServerMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String serverId,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<Page<SearchMessageResponse>>builder()
                .result(searchService.searchServerMessages(
                        principal.getId().toString(), serverId, q, page, size))
                .build();
    }

    @GetMapping("/server/{serverId}/members")
    public ApiResponse<List<SearchMemberResponse>> searchServerMembers(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String serverId,
            @RequestParam(defaultValue = "") String q
    ) {
        return ApiResponse.<List<SearchMemberResponse>>builder()
                .result(searchService.searchServerMembers(
                        principal.getId().toString(), serverId, q))
                .build();
    }

    @GetMapping("/server/{serverId}/channels")
    public ApiResponse<List<SearchChannelResponse>> searchServerChannels(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String serverId,
            @RequestParam(defaultValue = "") String q
    ) {
        return ApiResponse.<List<SearchChannelResponse>>builder()
                .result(searchService.searchServerChannels(
                        principal.getId().toString(), serverId, q))
                .build();
    }

    @GetMapping("/server/{serverId}/media")
    public ApiResponse<List<SearchAttachmentResponse>> searchServerMedia(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String serverId
    ) {
        return ApiResponse.<List<SearchAttachmentResponse>>builder()
                .result(searchService.searchServerMedia(principal.getId().toString(), serverId))
                .build();
    }

    @GetMapping("/server/{serverId}/files")
    public ApiResponse<List<SearchAttachmentResponse>> searchServerFiles(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String serverId
    ) {
        return ApiResponse.<List<SearchAttachmentResponse>>builder()
                .result(searchService.searchServerFiles(principal.getId().toString(), serverId))
                .build();
    }

    @GetMapping("/server/{serverId}/pins")
    public ApiResponse<List<SearchMessageResponse>> searchServerPins(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String serverId,
            @RequestParam(defaultValue = "") String q
    ) {
        return ApiResponse.<List<SearchMessageResponse>>builder()
                .result(searchService.searchServerPins(
                        principal.getId().toString(), serverId, q))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DM scope
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/dm/friends")
    public ApiResponse<List<SearchMemberResponse>> searchDmFriends(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "") String q
    ) {
        return ApiResponse.<List<SearchMemberResponse>>builder()
                .result(searchService.searchDmFriends(principal.getId().toString(), q))
                .build();
    }

    @GetMapping("/dm/media")
    public ApiResponse<List<SearchAttachmentResponse>> searchDmMedia(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.<List<SearchAttachmentResponse>>builder()
                .result(searchService.searchDmMedia(principal.getId().toString()))
                .build();
    }

    @GetMapping("/dm/files")
    public ApiResponse<List<SearchAttachmentResponse>> searchDmFiles(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.<List<SearchAttachmentResponse>>builder()
                .result(searchService.searchDmFiles(principal.getId().toString()))
                .build();
    }
}
