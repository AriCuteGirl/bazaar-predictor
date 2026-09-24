package com.example.bazaarpredictor.ui;

import com.example.bazaarpredictor.model.Opportunity;
import com.example.bazaarpredictor.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.client.input.MouseButtonEvent;
import com.example.bazaarpredictor.network.CompanionClient;
import java.util.List;

public final class BazaarDashboardScreen extends Screen {
    private final List<Opportunity> opportunities;
    private int scroll;
    private EditBox search;
    private boolean paused;
    private int selectedIndex = -1;
    private int hoveredIndex = -1;
    private final CompanionClient companion;
    public BazaarDashboardScreen(List<Opportunity> opportunities) {
        super(Component.literal("Bazaar Predictor")); this.opportunities = opportunities;
        this.companion = null;
    }
    public BazaarDashboardScreen(CompanionClient companion) { super(Component.literal("Bazaar Predictor")); this.opportunities = companion.opportunities(); this.companion = companion; }
    @Override protected void init() {
        search = new EditBox(font, 20, 335, 220, 20, Component.literal("Search"));
        search.setHint(Component.literal("Search items")); addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.literal("Settings"), b -> minecraft.setScreenAndShow(new SettingsScreen(this))).bounds(535, 365, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Refresh"), b -> { if (companion != null) companion.refreshNow(); }).bounds(430, 365, 95, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Auto-scan: ON"), b -> { paused = !paused; b.setMessage(Component.literal(paused ? "Auto-scan: OFF" : "Auto-scan: ON")); }).bounds(320, 365, 105, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Pause"), b -> { paused = !paused; b.setMessage(Component.literal(paused ? "Resume" : "Pause")); }).bounds(630, 365, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Prepare order"), b -> {
            if (selectedIndex >= 0 && selectedIndex < opportunities.size()) minecraft.setScreenAndShow(new ConfirmationScreen(opportunities.get(selectedIndex)));
        }).bounds(715, 365, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("View info"), b -> {
            if (selectedIndex >= 0 && selectedIndex < opportunities.size()) minecraft.setScreenAndShow(new OpportunityDetailsScreen(this, opportunities.get(selectedIndex)));
        }).bounds(715, 390, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Star item"), b -> {
            if (selectedIndex >= 0 && selectedIndex < opportunities.size()) {
                String id = opportunities.get(selectedIndex).productId();
                if (!ClientConfig.get().starredItems.add(id)) ClientConfig.get().starredItems.remove(id);
                b.setMessage(Component.literal(ClientConfig.get().starredItems.contains(id) ? "Unstar item" : "Star item"));
            }
        }).bounds(535, 390, 90, 20).build());
        int rowCount = Math.min(18, opportunities.size());
        for (int row = 0; row < rowCount; row++) {
            final int index = row;
            Button hitbox = Button.builder(Component.empty(), b -> selectedIndex = index)
                    .bounds(18, 94 + row * 12, Math.max(100, width - 36), 13).build();
            hitbox.setAlpha(0.0f);
            addRenderableWidget(hitbox);
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        extractTransparentBackground(g);
        text(g, "BAZAAR / PREDICT", 24, 18, 0x35E4D0);
        text(g, "Live Bazaar opportunity scanner", 24, 36, 0xD0D8E8);
        text(g, String.format("%d items  |  %s  |  %s", opportunities.size(), paused ? "Paused" : "Scanning enabled", "5m history"), 24, 54, 0xAAB8CC);
        text(g, "ITEM", 24, 82, 0x9EADC2);
        text(g, "BUY ORDER", 350, 82, 0x9EADC2);
        text(g, "SELL ORDER", 470, 82, 0x9EADC2);
        text(g, "EST. NET", 610, 82, 0x9EADC2);
        text(g, "SPREAD", 735, 82, 0x9EADC2);
        text(g, "AGE", 835, 82, 0x9EADC2);
        int y = 98;
        String query = search == null ? "" : search.getValue().toLowerCase();
        int shown = 0;
        for (int i = scroll; i < opportunities.size() && shown < 18; i++) {
            Opportunity o = opportunities.get(i);
            if (!query.isBlank() && !o.name().toLowerCase().contains(query) && !o.productId().toLowerCase().contains(query)) continue;
            if (o.netProfit() < ClientConfig.get().minimumProfit || o.volume() < ClientConfig.get().minimumVolume || o.spreadPercent() > ClientConfig.get().maximumSpreadPercent) continue;
            int color = o.risk().equals("ok") ? 0xE0E0E0 : 0xFFCC66;
            if (shown % 2 == 0) g.fill(0x5520334A, 18, y - 3, width - 18, y + 12);
            if (i == selectedIndex || i == hoveredIndex) {
                int outline = i == selectedIndex ? 0xFF35E4D0 : 0xFFB0FFF5;
                g.fill(outline, 18, y - 4, width - 18, y - 3);
                g.fill(outline, 18, y + 12, width - 18, y + 13);
                g.fill(outline, 18, y - 4, 19, y + 13);
                g.fill(outline, width - 19, y - 4, width - 18, y + 13);
            }
            if (ClientConfig.get().starredItems.contains(o.productId())) text(g, "★", 8, y, 0xFFD75A);
            text(g, o.name(), 24, y, color);
            text(g, formatCoins(o.buyPrice()), 350, y, color);
            text(g, formatCoins(o.sellPrice()), 470, y, color);
            text(g, formatCoins(o.netProfit()), 610, y, 0x65E6AE);
            text(g, String.format("%.1f%%", o.spreadPercent()), 735, y, color);
            text(g, age(o.observedAt()), 835, y, 0xB8C4D6);
            y += 12; shown++;
        }
        if (opportunities.isEmpty()) text(g, "No opportunities yet. Check the companion service and filters.", 24, 104, 0xFF7777);
        int total = opportunities.size();
        int trackTop = 98, trackBottom = Math.max(trackTop + 1, height - 55);
        g.fill(0x55334455, width - 16, trackTop, width - 10, trackBottom);
        int visible = 18, maxScroll = Math.max(0, total - visible);
        int thumbHeight = Math.max(18, (trackBottom - trackTop) * Math.min(visible, Math.max(1, total)) / Math.max(1, total));
        int thumbTop = trackTop + (maxScroll == 0 ? 0 : (trackBottom - trackTop - thumbHeight) * scroll / maxScroll);
        g.fill(0xFF35E4D0, width - 16, thumbTop, width - 10, thumbTop + thumbHeight);
        text(g, "Click a row for details  •  Mouse wheel changes pages", 24, height - 38, 0xAAB8CC);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }
    @Override public void mouseMoved(double mouseX, double mouseY) {
        if (mouseY >= 98 && mouseY < 98 + 18 * 12) {
            int index = scroll + (int)((mouseY - 98) / 12);
            hoveredIndex = index >= 0 && index < opportunities.size() ? index : -1;
        } else hoveredIndex = -1;
        super.mouseMoved(mouseX, mouseY);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && event.y() >= 98 && event.y() < 98 + 18 * 12) {
            int index = hoveredIndex >= 0 ? hoveredIndex : scroll + (int)((event.y() - 98) / 12);
            if (index >= 0 && index < opportunities.size()) { selectedIndex = index; return true; }
        }
        return super.mouseClicked(event, doubleClick);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        int maxScroll = Math.max(0, opportunities.size() - 18);
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int)Math.signum(vertical)));
        return true;
    }
    private static String formatCoins(double value) { return value >= 1_000_000 ? String.format("%.2fm", value / 1_000_000) : value >= 1_000 ? String.format("%.2fk", value / 1_000) : String.format("%.0f", value); }
    private static String age(long timestamp) { long seconds = Math.max(0, (System.currentTimeMillis() - timestamp) / 1000); return seconds < 60 ? seconds + "s" : (seconds / 60) + "m"; }
    private void text(GuiGraphicsExtractor g, String value, int x, int y, int color) {
        MutableComponent component = Component.literal(value).withStyle(style -> style.withColor(color));
        g.textRenderer().accept(x, y, component);
    }
}
