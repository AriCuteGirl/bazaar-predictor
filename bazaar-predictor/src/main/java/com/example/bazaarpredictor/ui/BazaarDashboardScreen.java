package com.example.bazaarpredictor.ui;

import com.example.bazaarpredictor.model.Opportunity;
import com.example.bazaarpredictor.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import java.util.List;

public final class BazaarDashboardScreen extends Screen {
    private final List<Opportunity> opportunities;
    private int scroll;
    private EditBox search;
    private EditBox minProfit, minVolume, maxSpread;
    private boolean paused;
    public BazaarDashboardScreen(List<Opportunity> opportunities) {
        super(Component.literal("Bazaar Predictor")); this.opportunities = opportunities;
    }
    @Override protected void init() {
        search = new EditBox(font, 20, 335, 220, 20, Component.literal("Search"));
        search.setHint(Component.literal("Search items")); addRenderableWidget(search);
        minProfit = field("Min profit", "100", 20, 365);
        minVolume = field("Min volume", "100", 145, 365);
        maxSpread = field("Max spread %", "50", 270, 365);
        addRenderableWidget(Button.builder(Component.literal("Apply filters"), b -> applyFilters()).bounds(395, 365, 130, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Settings"), b -> { }).bounds(535, 365, 90, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Pause"), b -> { paused = !paused; b.setMessage(Component.literal(paused ? "Resume" : "Pause")); }).bounds(630, 365, 80, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Prepare order"), b -> {
            if (!opportunities.isEmpty()) minecraft.setScreenAndShow(new ConfirmationScreen(opportunities.get(Math.min(scroll, opportunities.size() - 1))));
        }).bounds(715, 365, 145, 20).build());
    }
    private EditBox field(String hint, String value, int x, int y) {
        EditBox box = new EditBox(font, x, y, 115, 20, Component.literal(hint));
        box.setValue(value); box.setHint(Component.literal(hint)); addRenderableWidget(box); return box;
    }
    private void applyFilters() {
        try { ClientConfig.get().minimumProfit = Double.parseDouble(minProfit.getValue()); } catch (NumberFormatException ignored) { }
        try { ClientConfig.get().minimumVolume = Double.parseDouble(minVolume.getValue()); } catch (NumberFormatException ignored) { }
        try { ClientConfig.get().maximumSpreadPercent = Double.parseDouble(maxSpread.getValue()); } catch (NumberFormatException ignored) { }
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
            text(g, o.name(), 24, y, color);
            text(g, formatCoins(o.buyPrice()), 350, y, color);
            text(g, formatCoins(o.sellPrice()), 470, y, color);
            text(g, formatCoins(o.netProfit()), 610, y, 0x65E6AE);
            text(g, String.format("%.1f%%", o.spreadPercent()), 735, y, color);
            text(g, age(o.observedAt()), 835, y, 0xB8C4D6);
            y += 12; shown++;
        }
        if (opportunities.isEmpty()) text(g, "No opportunities yet. Check the companion service and filters.", 24, 104, 0xFF7777);
        text(g, "Click a row for details  •  Mouse wheel changes pages", 24, height - 38, 0xAAB8CC);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }
    private static String formatCoins(double value) { return value >= 1_000_000 ? String.format("%.2fm", value / 1_000_000) : value >= 1_000 ? String.format("%.2fk", value / 1_000) : String.format("%.0f", value); }
    private static String age(long timestamp) { long seconds = Math.max(0, (System.currentTimeMillis() - timestamp) / 1000); return seconds < 60 ? seconds + "s" : (seconds / 60) + "m"; }
    private void text(GuiGraphicsExtractor g, String value, int x, int y, int color) {
        MutableComponent component = Component.literal(value).withStyle(style -> style.withColor(color));
        g.textRenderer().accept(x, y, component);
    }
}
