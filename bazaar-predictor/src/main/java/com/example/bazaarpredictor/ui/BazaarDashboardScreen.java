package com.example.bazaarpredictor.ui;

import com.example.bazaarpredictor.model.Opportunity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.List;

public final class BazaarDashboardScreen extends Screen {
    private final List<Opportunity> opportunities;
    private int scroll;
    private EditBox search;
    public BazaarDashboardScreen(List<Opportunity> opportunities) {
        super(Component.literal("Bazaar Predictor")); this.opportunities = opportunities;
    }
    @Override protected void init() {
        search = new EditBox(font, 20, 335, 220, 20, Component.literal("Search"));
        search.setHint(Component.literal("Search items")); addRenderableWidget(search);
        addRenderableWidget(Button.builder(Component.literal("Prepare selected order"), b -> {
            if (!opportunities.isEmpty()) minecraft.setScreen(new ConfirmationScreen(opportunities.get(Math.min(scroll, opportunities.size() - 1))));
        }).bounds(250, 335, 180, 20).build());
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g, mouseX, mouseY, delta);
        g.drawString(font, title, 20, 16, 0xFFFFFF);
        g.drawString(font, "Item                         Buy       Sell       Net/item    Spread     Volume     Fill", 20, 34, 0xA0A0A0);
        int y = 50;
        String query = search == null ? "" : search.getValue().toLowerCase();
        int shown = 0;
        for (int i = scroll; i < opportunities.size() && shown < 18; i++) {
            Opportunity o = opportunities.get(i);
            if (!query.isBlank() && !o.name().toLowerCase().contains(query) && !o.productId().toLowerCase().contains(query)) continue;
            int color = o.risk().equals("ok") ? 0xE0E0E0 : 0xFFCC66;
            g.drawString(font, String.format("%-26s %7.0f  %7.0f  %9.0f  %6.1f%%  %8.0f  %5.1fm", o.name(), o.buyPrice(), o.sellPrice(), o.netProfit(), o.spreadPercent(), o.volume(), o.fillMinutes()), 20, y, color);
            y += 12; shown++;
        }
        if (opportunities.isEmpty()) g.drawString(font, "No opportunities yet. Is the companion service running?", 20, 60, 0xFF7777);
        super.render(g, mouseX, mouseY, delta);
    }
    @Override public boolean mouseScrolled(double x, double y, double amount) { scroll = Math.max(0, Math.min(Math.max(0, opportunities.size() - 1), scroll - (int)Math.signum(amount))); return true; }
}
