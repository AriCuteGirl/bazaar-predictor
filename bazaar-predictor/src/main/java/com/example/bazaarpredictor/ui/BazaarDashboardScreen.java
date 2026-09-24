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
        addRenderableWidget(Button.builder(Component.literal("Prepare selected order"), b -> {
            if (!opportunities.isEmpty()) minecraft.setScreenAndShow(new ConfirmationScreen(opportunities.get(Math.min(scroll, opportunities.size() - 1))));
        }).bounds(250, 335, 180, 20).build());
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
        text(g, title.getString(), 20, 16, 0xFFFFFF);
        text(g, "Item                         Buy       Sell       Net/item    Spread     Volume     Fill", 20, 34, 0xA0A0A0);
        int y = 50;
        String query = search == null ? "" : search.getValue().toLowerCase();
        int shown = 0;
        for (int i = scroll; i < opportunities.size() && shown < 18; i++) {
            Opportunity o = opportunities.get(i);
            if (!query.isBlank() && !o.name().toLowerCase().contains(query) && !o.productId().toLowerCase().contains(query)) continue;
            if (o.netProfit() < ClientConfig.get().minimumProfit || o.volume() < ClientConfig.get().minimumVolume || o.spreadPercent() > ClientConfig.get().maximumSpreadPercent) continue;
            int color = o.risk().equals("ok") ? 0xE0E0E0 : 0xFFCC66;
            text(g, String.format("%-26s %7.0f  %7.0f  %9.0f  %6.1f%%  %8.0f  %5.1fm", o.name(), o.buyPrice(), o.sellPrice(), o.netProfit(), o.spreadPercent(), o.volume(), o.fillMinutes()), 20, y, color);
            y += 12; shown++;
        }
        if (opportunities.isEmpty()) text(g, "No opportunities yet. Check the companion service and filters.", 20, 60, 0xFF7777);
        super.extractRenderState(g, mouseX, mouseY, delta);
    }
    private void text(GuiGraphicsExtractor g, String value, int x, int y, int color) {
        MutableComponent component = Component.literal(value).withStyle(style -> style.withColor(color));
        g.textRenderer().accept(x, y, component);
    }
}
