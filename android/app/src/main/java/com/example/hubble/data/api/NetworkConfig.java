package com.example.hubble.data.api;

import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import com.example.hubble.BuildConfig;

import java.util.Locale;

public final class NetworkConfig {

    private static final String DEFAULT_DEBUG_SCHEME = "http";
    private static final String DEFAULT_DEBUG_PORT = "8080";
    private static final String EMULATOR_HOST = "10.0.2.2";
    private static final String DEVICE_LOOPBACK_HOST = "127.0.0.1";
    private static final String DEV_BACKEND_IDENTITY_HOST = "dev-local";

    private NetworkConfig() {
    }

    public static String getApiBaseUrl() {
        if (!BuildConfig.DEBUG) {
            return ensureTrailingSlash(BuildConfig.BASE_URL);
        }

        String overrideUrl = ensureTrailingSlash(BuildConfig.DEBUG_BASE_URL_OVERRIDE);
        if (!TextUtils.isEmpty(overrideUrl)) {
            return overrideUrl;
        }

        if (!TextUtils.isEmpty(BuildConfig.DEV_BACKEND_HOST)) {
            String scheme = defaultIfBlank(BuildConfig.DEV_BACKEND_SCHEME, DEFAULT_DEBUG_SCHEME);
            String host = defaultIfBlank(BuildConfig.DEV_BACKEND_HOST, resolveDefaultDebugHost());
            String port = defaultIfBlank(BuildConfig.DEV_BACKEND_PORT, DEFAULT_DEBUG_PORT);
            return ensureTrailingSlash(buildBaseUrl(scheme, host, port));
        }

        return ensureTrailingSlash(BuildConfig.BASE_URL);
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

        return ensureTrailingSlash(BuildConfig.BASE_URL);
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
            return normalizeDebugLocalAlias(trimmed);
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

    private static String normalizeDebugLocalAlias(String url) {
        if (!BuildConfig.DEBUG) {
            return url;
        }

        Uri sourceUri = Uri.parse(url);
        String sourceHost = sourceUri.getHost();
        if (!isLocalDevAlias(sourceHost)) {
            return url;
        }

        Uri targetBaseUri = Uri.parse(getApiBaseUrl());
        String targetHost = targetBaseUri.getHost();
        if (TextUtils.isEmpty(targetHost)) {
            return url;
        }

        int targetPort = targetBaseUri.getPort();
        String targetScheme = chooseSchemeForAlias(sourceUri.getScheme(), targetBaseUri.getScheme());
        return sourceUri.buildUpon()
                .scheme(targetScheme)
                .encodedAuthority(buildAuthority(targetHost, targetPort))
                .build()
                .toString();
    }

    private static boolean isAbsoluteNetworkUrl(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://")
                || lower.startsWith("https://")
                || lower.startsWith("ws://")
                || lower.startsWith("wss://");
    }

    private static String chooseSchemeForAlias(String sourceScheme, String targetBaseScheme) {
        if (TextUtils.isEmpty(sourceScheme)) {
            return targetBaseScheme;
        }

        String lowerSourceScheme = sourceScheme.toLowerCase(Locale.ROOT);
        if (lowerSourceScheme.startsWith("ws")) {
            return "https".equalsIgnoreCase(targetBaseScheme) ? "wss" : "ws";
        }
        return targetBaseScheme;
    }

    private static String buildAuthority(String host, int port) {
        if (port <= 0) {
            return host;
        }
        return host + ":" + port;
    }

    private static boolean isLocalDevAlias(String host) {
        if (TextUtils.isEmpty(host)) {
            return false;
        }

        String normalizedHost = host.trim().toLowerCase(Locale.ROOT);
        return "localhost".equals(normalizedHost)
                || DEVICE_LOOPBACK_HOST.equals(normalizedHost)
                || EMULATOR_HOST.equals(normalizedHost);
    }

    private static String resolveDefaultDebugHost() {
        return isProbablyEmulator() ? EMULATOR_HOST : DEVICE_LOOPBACK_HOST;
    }

    private static boolean isProbablyEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.toLowerCase(Locale.ROOT).contains("emulator")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT)
                || Build.PRODUCT.contains("sdk_gphone");
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
