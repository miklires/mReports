package io.github.miklires.mreports.notification;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

public final class SafeWebhookUrl {
    private static final Set<String> HOSTS = Set.of("discord.com", "discordapp.com", "canary.discord.com", "ptb.discord.com");
    private SafeWebhookUrl() {}
    public static URI parse(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            URI uri = URI.create(value.trim());
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !HOSTS.contains(host) || uri.getUserInfo() != null
                    || uri.getFragment() != null || uri.getPath() == null || !uri.getPath().startsWith("/api/webhooks/")) return null;
            return uri;
        } catch (IllegalArgumentException exception) { return null; }
    }
}
