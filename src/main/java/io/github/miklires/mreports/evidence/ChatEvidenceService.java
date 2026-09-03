package io.github.miklires.mreports.evidence;

import io.github.miklires.mreports.MReportsPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatEvidenceService implements Listener {
    private final MReportsPlugin plugin;
    private final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
    private final Map<UUID, Buffer> messages = new ConcurrentHashMap<>();

    public ChatEvidenceService(MReportsPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("evidence.chat.enabled", true)) return;
        int tracked = Math.clamp(plugin.getConfig().getInt("evidence.chat.maximum-tracked-players", 10_000), 100, 100_000);
        if (!messages.containsKey(event.getPlayer().getUniqueId()) && messages.size() >= tracked) purgeExpired();
        if (!messages.containsKey(event.getPlayer().getUniqueId()) && messages.size() >= tracked) return;
        int maximumLength = Math.clamp(plugin.getConfig().getInt("evidence.chat.maximum-message-length", 256), 16, 1000);
        String text = sanitize(plain.serialize(event.message()), maximumLength);
        if (text.isBlank()) return;
        int maximumMessages = Math.clamp(plugin.getConfig().getInt("evidence.chat.maximum-messages-per-player", 10), 1, 50);
        messages.computeIfAbsent(event.getPlayer().getUniqueId(), ignored -> new Buffer()).add(
                new EvidenceView(event.getPlayer().getUniqueId(), event.getPlayer().getName(), text, Instant.now()), maximumMessages);
    }

    public List<EvidenceView> snapshot(UUID playerId) {
        if (!plugin.getConfig().getBoolean("evidence.chat.enabled", true)) return List.of();
        Buffer buffer = messages.get(playerId);
        if (buffer == null) return List.of();
        Instant cutoff = Instant.now().minusSeconds(Math.clamp(plugin.getConfig().getLong("evidence.chat.retention-seconds", 300), 30L, 86_400L));
        return buffer.snapshot(cutoff);
    }

    private void purgeExpired() {
        Instant cutoff = Instant.now().minusSeconds(Math.clamp(plugin.getConfig().getLong("evidence.chat.retention-seconds", 300), 30L, 86_400L));
        messages.entrySet().removeIf(entry -> entry.getValue().snapshot(cutoff).isEmpty());
    }

    private static String sanitize(String value, int maximum) {
        StringBuilder clean = new StringBuilder(Math.min(value.length(), maximum));
        for (int index = 0; index < value.length() && clean.length() < maximum; index++) {
            char character = value.charAt(index);
            if (!Character.isISOControl(character)) clean.append(character);
        }
        return clean.toString().trim();
    }

    private static final class Buffer {
        private final ArrayDeque<EvidenceView> entries = new ArrayDeque<>();
        private synchronized void add(EvidenceView evidence, int maximum) {
            while (entries.size() >= maximum) entries.removeFirst();
            entries.addLast(evidence);
        }
        private synchronized List<EvidenceView> snapshot(Instant cutoff) {
            while (!entries.isEmpty() && entries.getFirst().occurredAt().isBefore(cutoff)) entries.removeFirst();
            return List.copyOf(entries);
        }
    }
}
