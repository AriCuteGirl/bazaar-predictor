package com.example.bazaarpredictor.ui;

import com.example.bazaarpredictor.model.Opportunity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class ConfirmationScreen extends Screen {
    private final Opportunity opportunity;
    public ConfirmationScreen(Opportunity opportunity) { super(Component.literal("Confirm Bazaar preparation")); this.opportunity = opportunity; }
    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Prepare in Bazaar"), b -> {
            // Deliberately only closes this summary; no final transaction click is automated.
            minecraft.setScreenAndShow(null);
        }).bounds(width / 2 - 100, height / 2 + 55, 200, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> minecraft.setScreenAndShow(null)).bounds(width / 2 - 100, height / 2 + 82, 200, 20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int x, int y, float delta) {
        extractTransparentBackground(g);
        int left = width / 2 - 150; int top = height / 2 - 80;
        g.text(font, title, left, top, 0xFFFFFF);
        g.text(font, "Item: " + opportunity.name(), left, top + 22, 0xE0E0E0);
        g.text(font, String.format("Buy order: %.0f   Sell order: %.0f", opportunity.buyPrice(), opportunity.sellPrice()), left, top + 38, 0xE0E0E0);
        g.text(font, String.format("Net profit/item: %.0f   Spread: %.2f%%", opportunity.netProfit(), opportunity.spreadPercent()), left, top + 54, 0x80FF80);
        g.text(font, "The final confirmation click remains yours.", left, top + 78, 0xFFCC66);
        super.extractRenderState(g, x, y, delta);
    }
}
