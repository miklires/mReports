package io.github.miklires.mreports.notification;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SafeWebhookUrlTest {
    @Test void acceptsOnlyDiscordHttpsWebhookUrls() {
        assertNotNull(SafeWebhookUrl.parse("https://discord.com/api/webhooks/1/token"));
        assertNull(SafeWebhookUrl.parse("http://discord.com/api/webhooks/1/token"));
        assertNull(SafeWebhookUrl.parse("https://discord.com.evil.example/api/webhooks/1/token"));
        assertNull(SafeWebhookUrl.parse("https://discord.com/channels/1"));
    }
}
