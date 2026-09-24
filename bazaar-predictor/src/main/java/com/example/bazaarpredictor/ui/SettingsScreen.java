package com.example.bazaarpredictor.ui;

import com.example.bazaarpredictor.config.ClientConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class SettingsScreen extends Screen {
    private final Screen parent;
    private EditBox profit, volume, spread, coins;
    public SettingsScreen(Screen parent) { super(Component.literal("Bazaar Predictor Settings")); this.parent = parent; }
    @Override protected void init() {
        ClientConfig c = ClientConfig.get();
        profit = field("Minimum profit", String.valueOf(c.minimumProfit), 70, 100);
        volume = field("Minimum volume", String.valueOf(c.minimumVolume), 70, 145);
        spread = field("Maximum spread %", String.valueOf(c.maximumSpreadPercent), 70, 190);
        coins = field("Available purse / coins", String.valueOf(c.availableCoins), 70, 235);
        addRenderableWidget(Button.builder(Component.literal(c.autoProfitMode ? "Auto profit: ON" : "Auto profit: OFF"), b -> {
            c.autoProfitMode = !c.autoProfitMode; b.setMessage(Component.literal(c.autoProfitMode ? "Auto profit: ON" : "Auto profit: OFF"));
        }).bounds(70, 280, 180, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Auto-set safe filters"), b -> autoSet()).bounds(260, 280, 180, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> { save(); minecraft.setScreenAndShow(parent); }).bounds(70, 320, 120, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> minecraft.setScreenAndShow(parent)).bounds(200, 320, 120, 20).build());
    }
    private EditBox field(String hint, String value, int x, int y) { EditBox box = new EditBox(font, x, y, 260, 20, Component.literal(hint)); box.setValue(value); box.setHint(Component.literal(hint)); addRenderableWidget(box); return box; }
    private void save() { ClientConfig c = ClientConfig.get(); c.minimumProfit = number(profit, c.minimumProfit); c.minimumVolume = number(volume, c.minimumVolume); c.maximumSpreadPercent = number(spread, c.maximumSpreadPercent); c.availableCoins = number(coins, c.availableCoins); }
    private void autoSet() { double budget = number(coins, ClientConfig.get().availableCoins); profit.setValue(String.valueOf(Math.max(100, budget * 0.002))); volume.setValue(String.valueOf(Math.max(10, budget > 1_000_000 ? 100 : 10))); spread.setValue(String.valueOf(budget < 100_000 ? 25 : 50)); }
    private double number(EditBox box, double fallback) { try { return Math.max(0, Double.parseDouble(box.getValue())); } catch (NumberFormatException e) { return fallback; } }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) { extractTransparentBackground(g); text(g, "BAZAAR / PREDICT  •  SETTINGS", 40, 35, 0x35E4D0); text(g, "Tune the advisor without editing config files.", 40, 55, 0xAAB8CC); text(g, "Minimum net profit per item", 70, 92, 0xE0E0E0); text(g, "Minimum available market volume", 70, 137, 0xE0E0E0); text(g, "Maximum accepted spread (%)", 70, 182, 0xE0E0E0); text(g, "Budget used for safe recommendations", 70, 227, 0xE0E0E0); text(g, "Auto-profit ranks opportunities only; it never clicks trades.", 70, 365, 0xFFCC66); super.extractRenderState(g, mouseX, mouseY, delta); }
    private void text(GuiGraphicsExtractor g, String value, int x, int y, int color) { MutableComponent c = Component.literal(value).withStyle(s -> s.withColor(color)); g.textRenderer().accept(x, y, c); }
}
