package com.example.hubble.data.api;

import android.text.TextUtils;

import com.example.hubble.BuildConfig;

import java.util.Locale;

public final class NetworkConfig {

    private static final String DEFAULT_DEBUG_SCHEME = "http";
    private static final String DEFAULT_DEBUG_PORT = "8080";
    private static final String DEV_BACKEND_IDENTITY_HOST = "dev-local";
    private static final String API_PATH_SEGMENT = "/api";

    private NetworkConfig() {
    }

    public static String getApiBaseUrl() {
        if (!BuildConfig.DEBUG) {
            return normalizeBaseUrl(BuildConfig.BASE_URL);
        }

        String overrideUrl = normalizeBaseUrl(BuildConfig.DEBUG_BASE_URL_OVERRIDE);
        if (!TextUtils.isEmpty(overrideUrl)) {
            return overrideUrl;
        }

        if (!TextUtils.isEmpty(BuildConfig.DEV_BACKEND_HOST)) {
            String scheme = defaultIfBlank(BuildConfig.DEV_BACKEND_SCHEME, DEFAULT_DEBUG_SCHEME);
            String host = BuildConfig.DEV_BACKEND_HOST.trim();
            String port = defaultIfBlank(BuildConfig.DEV_BACKEND_PORT, DEFAULT_DEBUG_PORT);
            return normalizeBaseUrl(buildBaseUrl(scheme, host, port));
        }

        return normalizeBaseUrl(BuildConfig.BASE_URL);
    }

    public static String getBackendIdentity() {
        if (!BuildConfig.DEBUG) {
            return getApiBaseUrl();
        }

        String overrideUrl = ensureTrailingSlash(BuildConfig.DEBUG_BASE_URL_OVERRIDE);
        if (!TextUtils.isEmpty(overrideUrl)) {
            return overrideUrl;
        }

        if (!TextUtils.isEmpty(BuildConfig.DEV_BACKEND_HOST)) {
            String scheme = defaultIfBlank(BuildConfig.DEV_BACKEND_SCHEME, DEFAULT_DEBUG_SCHEME);
            String host = defaultIfBlank(BuildConfig.DEV_BACKEND_HOST, DEV_BACKEND_IDENTITY_HOST);
            String port = defaultIfBlank(BuildConfig.DEV_BACKEND_PORT, DEFAULT_DEBUG_PORT);
            return ensureTrailingSlash(buildBaseUrl(scheme, host, port));
        }

        return normalizeBaseUrl(BuildConfig.BASE_URL);
    }

    public static String getWebSocketBaseUrl() {
        String normalizedBaseUrl = trimTrailingSlash(getApiBaseUrl());
        String lower = normalizedBaseUrl.toLowerCase(Locale.ROOT);

        if (lower.startsWith("https://")) {
            return "wss://" + normalizedBaseUrl.substring("https://".length()) + "/";
        }
        if (lower.startsWith("http://")) {
            return "ws://" + normalizedBaseUrl.substring("http://".length()) + "/";
        }
        return "ws://" + normalizedBaseUrl + "/";
    }

    public static String getWebSocketUrl(String relativePath) {
        String normalizedPath = relativePath == null ? "" : relativePath.trim();
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        return getWebSocketBaseUrl() + normalizedPath;
    }

    public static String resolveUrl(String urlOrPath) {
        if (TextUtils.isEmpty(urlOrPath)) {
            return null;
        }

        String trimmed = urlOrPath.trim();
        if (isAbsoluteNetworkUrl(trimmed)) {
            return trimmed;
        }

        String baseUrl = getApiBaseUrl();
        if (baseUrl.endsWith("/") && trimmed.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + trimmed;
        }
        if (!baseUrl.endsWith("/") && !trimmed.startsWith("/")) {
            return baseUrl + "/" + trimmed;
        }
        return baseUrl + trimmed;
    }

    private static boolean isAbsoluteNetworkUrl(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://")
                || lower.startsWith("https://")
                || lower.startsWith("ws://")
                || lower.startsWith("wss://");
    }

    private static String buildBaseUrl(String scheme, String host, String port) {
        StringBuilder builder = new StringBuilder();
        builder.append(scheme).append("://").append(host);
        if (!TextUtils.isEmpty(port)) {
            builder.append(":").append(port);
        }
        return builder.toString();
    }

    private static String defaultIfBlank(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value.trim();
    }

    private static String normalizeBaseUrl(String value) {
        if (TextUtils.isEmpty(value)) {
            return value;
        }

        String trimmed = trimTrailingSlash(value);
        if (trimmed.toLowerCase(Locale.ROOT).endsWith(API_PATH_SEGMENT)) {
            trimmed = trimmed.substring(0, trimmed.length() - API_PATH_SEGMENT.length());
        }
        return ensureTrailingSlash(trimmed);
    }

    private static String ensureTrailingSlash(String value) {
        if (TextUtils.isEmpty(value)) {
            return value;
        }
        String trimmed = value.trim();
        return trimmed.endsWith("/") ? trimmed : trimmed + "/";
    }

    private static String trimTrailingSlash(String value) {
        if (TextUtils.isEmpty(value)) {
            return value;
        }
        String trimmed = value.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
