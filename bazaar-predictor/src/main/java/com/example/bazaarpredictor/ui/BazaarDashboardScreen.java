package com.example.bazaarpredictor.ui;

import com.example.bazaarpredictor.config.ClientConfig;
import com.example.bazaarpredictor.model.Opportunity;
import com.example.bazaarpredictor.model.OpportunityTable;
import com.example.bazaarpredictor.network.CompanionClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class BazaarDashboardScreen extends Screen {
    private static final int ROW_HEIGHT = 19;
    private static final String[] HEADINGS = {"Item", "Buy order", "Sell order", "Net/item", "Spread", "Age"};
    private final CompanionClient companion;
    private final OpportunityTable table = new OpportunityTable();
    private final Button[] headings = new Button[6];
    private List<Opportunity> snapshot;
    private EditBox search;
    private Button info, star, auto, stars;
    private String query = "";
    private boolean paused, starsOnly, dragging, refreshPending;
    private List<Opportunity> refreshBaseline;
    private int left, top, panelWidth, panelHeight, rowTop, rowBottom, capacity;
    private final int[] columns = new int[7];

    public BazaarDashboardScreen(CompanionClient companion) {
        super(Component.literal("Bazaar Predictor"));
        this.companion = companion;
        this.snapshot = companion.opportunities();
    }
    @Override protected void init() {
        panelWidth = Math.min(width - 12, 940);
        panelHeight = height - 12;
        left = (width - panelWidth) / 2;
        top = 6;
        int inner = panelWidth - 28;
        double[] positions = {0, .39, .53, .67, .81, .91, 1};
        for (int i = 0; i < columns.length; i++) columns[i] = left + 8 + (int)(inner * positions[i]);
        search = new EditBox(font, left + 8, top + 38, Math.max(50, panelWidth - 240), 20, Component.literal("Search items"));
        search.setHint(Component.literal("Search items"));
        search.setValue(query);
        search.setResponder(value -> { query = value; table.scrollTo(0, capacity); updateRows(); });
        addRenderableWidget(search);
        button("Refresh", left + panelWidth - 226, top + 38, 68, () -> {
            refreshBaseline = companion.opportunities();
            refreshPending = true;
            companion.refreshNow();
        });
        auto = button("", left + panelWidth - 154, top + 38, 146, () -> { paused = !paused; updateRows(); });
        int bw = (panelWidth - 28) / 4;
        button("Settings", left + 8, top + 64, bw, () -> minecraft.setScreenAndShow(new SettingsScreen(this)));
        stars = button("", left + 12 + bw, top + 64, bw, () -> { starsOnly = !starsOnly; table.scrollTo(0, capacity); updateRows(); });
        star = button("", left + 16 + bw * 2, top + 64, bw, () -> {
            Opportunity selected = table.selected();
            if (selected != null && !ClientConfig.get().starredItems.add(selected.productId())) ClientConfig.get().starredItems.remove(selected.productId());
            updateRows();
        });
        info = button("View info", left + 20 + bw * 3, top + 64, bw, () -> {
            Opportunity selected = table.selected();
            if (selected != null) minecraft.setScreenAndShow(new OpportunityDetailsScreen(this, selected));
        });
        for (int i = 0; i < 6; i++) {
            final int index = i;
            headings[i] = button(HEADINGS[i], columns[i], top + 92, columns[i + 1] - columns[i] - 2,
                    () -> { table.sortBy(OpportunityTable.Column.values()[index]); updateRows(); });
        }
        rowTop = top + 116;
        capacity = Math.max(1, (panelHeight - 174) / ROW_HEIGHT);
        rowBottom = rowTop + capacity * ROW_HEIGHT;
        button("Close", left + panelWidth - 70, top + panelHeight - 28, 62, this::onClose);
        updateRows();
    }
    private Button button(String label, int x, int y, int w, Runnable action) {
        return addRenderableWidget(Button.builder(Component.literal(label), b -> action.run()).bounds(x, y, Math.max(16, w), 20).build());
    }
    private void updateRows() {
        if (search == null || info == null) return;
        var cfg = ClientConfig.get();
        table.update(snapshot, query, cfg.minimumProfit, cfg.minimumVolume, cfg.maximumSpreadPercent, cfg.starredItems, starsOnly, capacity);
        info.active = star.active = table.selected() != null;
        star.setMessage(Component.literal(table.selected() != null && cfg.starredItems.contains(table.selected().productId()) ? "Unstar item" : "Star item"));
        stars.setMessage(Component.literal(starsOnly ? "Starred only" : "All / stars"));
        auto.setMessage(Component.literal(paused ? "Live updates: OFF" : "Live updates: ON"));
        for (int i = 0; i < 6; i++) if (headings[i] != null)
            headings[i].setMessage(Component.literal(HEADINGS[i] + (i == table.column().ordinal() ? (table.ascending() ? " ↑" : " ↓") : "")));
    }
    @Override public void tick() {
        super.tick();
        if (!paused || (refreshPending && companion.opportunities() != refreshBaseline)) {
            snapshot = companion.opportunities();
            refreshPending = false;
        }
        updateRows();
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        // 26.2 fill takes x1, y1, x2, y2, ARGB (color LAST).
        g.fill(left, top, left + panelWidth, top + panelHeight, 0xF0141F2E);
        g.fill(left, top, left + panelWidth, top + 3, 0xFF35E4D0);
        text(g, "BAZAAR / PREDICT  0.1.1", left + 8, top + 10, 0x35E4D0, panelWidth - 16);
        text(g, table.rows().size() + "/" + snapshot.size() + " items | " + (paused ? "Display paused" : companion.status()), left + 8, top + 24, 0xAAB8CC, panelWidth - 16);
        for (int row = 0; row < capacity; row++) {
            Opportunity o = table.atVisibleRow(row, capacity);
            if (o == null) break;
            int y = rowTop + row * ROW_HEIGHT;
            boolean selected = table.selected(o);
            boolean hover = hitRow(mx, my) == row;
            g.fill(columns[0], y, columns[6], y + ROW_HEIGHT - 1, selected ? 0xFF245568 : hover ? 0xFF294054 : row % 2 == 0 ? 0xFF1D2D40 : 0xFF192738);
            border(g, columns[0], y, columns[6], y + ROW_HEIGHT - 1, selected ? 0xFF35E4D0 : hover ? 0xFFA0DCD6 : 0xFF34485D, selected ? 2 : 1);
            String name = (ClientConfig.get().starredItems.contains(o.productId()) ? "★ " : "") + o.name();
            String[] values = {name, coins(o.buyPrice()), coins(o.sellPrice()), coins(o.netProfit()), String.format(Locale.ROOT, "%.1f%%", o.spreadPercent()), age(o.observedAt())};
            for (int c = 0; c < values.length; c++) text(g, values[c], columns[c] + 4, y + 5, c == 3 ? 0x65E6AE : selected ? 0xFFFFFF : 0xD5DFEC, columns[c + 1] - columns[c] - 8);
        }
        if (table.rows().isEmpty()) text(g, "No matching items. Adjust Settings or search, then Refresh.", columns[0] + 4, rowTop + 7, 0xFFCC66, panelWidth - 32);
        int sx = columns[6] + 4;
        g.fill(sx, rowTop, sx + 7, rowBottom, 0xFF0C1420);
        int thumbHeight = Math.max(16, (rowBottom - rowTop) * Math.min(capacity, Math.max(1, table.rows().size())) / Math.max(1, table.rows().size()));
        int max = Math.max(0, table.rows().size() - capacity);
        int thumbY = rowTop + (max == 0 ? 0 : (rowBottom - rowTop - thumbHeight) * table.offset() / max);
        g.fill(sx, thumbY, sx + 7, thumbY + thumbHeight, 0xFF35E4D0);
        Opportunity selected = table.selected();
        text(g, selected == null ? "Click a row to select; View info opens details." : "Selected: " + selected.name(), left + 8, top + panelHeight - 44, 0x35E4D0, panelWidth - 16);
        text(g, "Sort: " + HEADINGS[table.column().ordinal()] + " " + (table.ascending() ? "low → high" : "high → low") + " | Click heading to reverse", left + 8, top + panelHeight - 27, 0xAAB8CC, panelWidth - 88);
        super.extractRenderState(g, mx, my, delta);
    }
    private static void border(GuiGraphicsExtractor g, int x, int y, int right, int bottom, int color, int thickness) {
        g.fill(x, y, right, y + thickness, color);
        g.fill(x, bottom - thickness, right, bottom, color);
        g.fill(x, y, x + thickness, bottom, color);
        g.fill(right - thickness, y, right, bottom, color);
    }
    private int hitRow(double x, double y) {
        if (x < columns[0] || x >= columns[6] || y < rowTop || y >= rowBottom) return -1;
        int row = (int)(y - rowTop) / ROW_HEIGHT;
        return table.atVisibleRow(row, capacity) == null ? -1 : row;
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        if (event.button() == 0) {
            int row = hitRow(event.x(), event.y());
            if (row >= 0) { table.selectVisibleRow(row, capacity); updateRows(); return true; }
            if (event.x() >= columns[6] + 4 && event.x() < columns[6] + 11 && event.y() >= rowTop && event.y() < rowBottom) {
                dragging = true; dragScroll(event.y()); return true;
            }
        }
        return super.mouseClicked(event, twice);
    }
    private void dragScroll(double y) {
        table.scrollTo((int)Math.round((y - rowTop) / Math.max(1, rowBottom - rowTop) * Math.max(0, table.rows().size() - capacity)), capacity);
    }
    @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (dragging) { dragScroll(e.y()); return true; }
        return super.mouseDragged(e, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent e) {
        if (dragging) { dragging = false; return true; }
        return super.mouseReleased(e);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (x >= columns[0] && x <= columns[6] + 11 && y >= rowTop && y < rowBottom) {
            table.scrollTo(table.offset() - (int)Math.signum(vertical) * 3, capacity); return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    private static String coins(double value) { return String.format(Locale.ROOT, value >= 1e6 ? "%.2fm" : value >= 1e3 ? "%.2fk" : "%.2f", value >= 1e6 ? value / 1e6 : value >= 1e3 ? value / 1e3 : value); }
    private static String age(long time) { return Math.max(0, (System.currentTimeMillis() - time) / 1000) + "s"; }
    private void text(GuiGraphicsExtractor g, String value, int x, int y, int color, int maxWidth) {
        g.textRenderer().accept(x, y, Component.literal(font.plainSubstrByWidth(value, Math.max(1, maxWidth))).withStyle(s -> s.withColor(color)));
    }
}
