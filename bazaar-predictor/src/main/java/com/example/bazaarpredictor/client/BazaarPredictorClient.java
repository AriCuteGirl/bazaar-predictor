package com.example.bazaarpredictor.client;

import com.example.bazaarpredictor.config.ClientConfig;
import com.example.bazaarpredictor.network.CompanionClient;
import com.example.bazaarpredictor.ui.BazaarDashboardScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public final class BazaarPredictorClient implements ClientModInitializer {
    private final CompanionClient companion = new CompanionClient();
    private KeyMapping hudKey, dashboardKey;
    @Override public void onInitializeClient() {
        hudKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.bazaarpredictor.toggle_hud", GLFW.GLFW_KEY_B, "category.bazaarpredictor"));
        dashboardKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.bazaarpredictor.open_dashboard", GLFW.GLFW_KEY_N, "category.bazaarpredictor"));
        companion.start();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (hudKey.consumeClick()) ClientConfig.get().hudEnabled = !ClientConfig.get().hudEnabled;
            while (dashboardKey.consumeClick()) client.setScreen(new BazaarDashboardScreen(companion.opportunities()));
        });
        HudRenderCallback.EVENT.register(this::renderHud);
    }
    private void renderHud(GuiGraphics g, net.minecraft.client.DeltaTracker tick) {
        if (!ClientConfig.get().hudEnabled || Minecraft.getInstance().screen != null) return;
        g.drawString(Minecraft.getInstance().font, "Bazaar Predictor  •  " + companion.status(), 8, 8, 0xFFFFFF);
        var list = companion.opportunities();
        for (int i = 0; i < Math.min(3, list.size()); i++) {
            var o = list.get(i);
            g.drawString(Minecraft.getInstance().font, String.format("%s  +%.0f/item  %.1f%%", o.name(), o.netProfit(), o.spreadPercent()), 8, 20 + i * 11, 0x80FF80);
        }
    }
}
