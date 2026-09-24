package com.example.bazaarpredictor.ui;

import com.example.bazaarpredictor.model.Opportunity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class OpportunityDetailsScreen extends Screen {
    private final Screen parent; private final Opportunity opportunity;
    public OpportunityDetailsScreen(Screen parent, Opportunity opportunity) { super(Component.literal(opportunity.name())); this.parent = parent; this.opportunity = opportunity; }
    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Prepare order summary"), b -> minecraft.setScreenAndShow(new ConfirmationScreen(opportunity))).bounds(width / 2 - 105, height / 2 + 60, 210, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> minecraft.setScreenAndShow(parent)).bounds(width / 2 - 105, height / 2 + 88, 210, 20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        extractTransparentBackground(g); int x = width / 2 - 220; int y = height / 2 - 100;
        g.fill(x - 20, y - 25, x + 420, y + 135, 0xEE142234);
        text(g, "BAZAAR / ITEM DETAILS", x, y, 0x35E4D0); text(g, opportunity.name(), x, y + 25, 0xFFFFFF);
        text(g, "Buy order: " + coins(opportunity.buyPrice()), x, y + 50, 0xD0D8E8); text(g, "Sell order: " + coins(opportunity.sellPrice()), x, y + 68, 0xD0D8E8);
        text(g, "Estimated net: " + coins(opportunity.netProfit()), x, y + 86, 0x65E6AE); text(g, "Spread: " + String.format("%.2f%%", opportunity.spreadPercent()), x + 210, y + 86, 0xD0D8E8);
        text(g, "Volume: " + String.format("%.0f", opportunity.volume()) + "  •  Fill estimate: " + String.format("%.1fm", opportunity.fillMinutes()), x, y + 104, 0xAAB8CC);
        text(g, "This does not place an order or click menus. Review and do it manually.", x, y + 122, 0xFFCC66); super.extractRenderState(g, mouseX, mouseY, delta);
    }
    private static String coins(double v) { return v >= 1_000_000 ? String.format("%.2fm", v / 1_000_000) : v >= 1_000 ? String.format("%.2fk", v / 1_000) : String.format("%.0f", v); }
    @Override public void onClose() { minecraft.setScreenAndShow(parent); }
    @Override public boolean isPauseScreen() { return false; }
    private void text(GuiGraphicsExtractor g, String value, int x, int y, int color) { MutableComponent c = Component.literal(value).withStyle(s -> s.withColor(color)); g.textRenderer().accept(x, y, c); }
}
