package com.example.bazaarpredictor.client;

import com.example.bazaarpredictor.config.ClientConfig;
import com.example.bazaarpredictor.network.CompanionClient;
import com.example.bazaarpredictor.ui.BazaarDashboardScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import com.example.bazaarpredictor.config.ClientConfig;

public final class BazaarPredictorClient implements ClientModInitializer {
    private final CompanionClient companion = new CompanionClient();
    private KeyMapping hudKey, dashboardKey;
    @Override public void onInitializeClient() {
        var category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("bazaarpredictor", "keys"));
        hudKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.bazaarpredictor.toggle_hud", GLFW.GLFW_KEY_B, category));
        dashboardKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.bazaarpredictor.open_dashboard", GLFW.GLFW_KEY_N, category));
        companion.start();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (hudKey.consumeClick()) ClientConfig.get().hudEnabled = !ClientConfig.get().hudEnabled;
            while (dashboardKey.consumeClick()) client.setScreenAndShow(new BazaarDashboardScreen(companion));
        });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("bazaarpredictor", "hud"), this::renderHud);
    }
    private void renderHud(net.minecraft.client.gui.GuiGraphicsExtractor g, net.minecraft.client.DeltaTracker tick) {
        if (!ClientConfig.get().hudEnabled) return;
        int x = 8, y = 8;
        g.fill(x - 4, y - 4, 310, 86 + Math.min(5, companion.opportunities().size()) * 12, 0xB0101824);
        g.text(Minecraft.getInstance().font, "BAZAAR / PREDICT", x, y, 0x35E4D0); y += 12;
        g.text(Minecraft.getInstance().font, companion.status(), x, y, 0xD0D8E8); y += 12;
        var cfg = ClientConfig.get();
        g.text(Minecraft.getInstance().font, String.format("Prepared: %d  Est. +%.0f  Loss: %.0f", cfg.preparedOrders, cfg.estimatedSessionProfit, cfg.estimatedSessionLoss), x, y, 0xAAB8CC); y += 14;
        var list = companion.opportunities();
        for (int i = 0; i < Math.min(3, list.size()); i++) {
            var o = list.get(i);
            g.text(Minecraft.getInstance().font, String.format("%s  +%.0f/item  %.1f%%", o.name(), o.netProfit(), o.spreadPercent()), x, y + i * 12, 0x80FF80);
        }
    }
}
