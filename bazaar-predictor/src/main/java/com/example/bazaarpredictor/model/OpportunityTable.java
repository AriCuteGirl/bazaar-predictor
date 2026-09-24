package com.example.bazaarpredictor.model;
import java.util.*;

/** Shared ordering and selection for rendering, hit testing and item actions. */
public final class OpportunityTable {
    public enum Column { ITEM, BUY, SELL, NET, SPREAD, AGE }
    private List<Opportunity> rows = List.of();
    private String selectedId;
    private Column column = Column.NET;
    private boolean ascending;
    private int offset;
    public void sortBy(Column next) {
        ascending = next == column ? !ascending : next != Column.NET;
        column = next;
        offset = 0;
    }
    public void update(List<Opportunity> source, String query, double profit, double volume,
                       double spread, Set<String> stars, boolean starsOnly, int capacity) {
        String needle = query.toLowerCase(Locale.ROOT).trim();
        Comparator<Opportunity> comparator = switch (column) {
            case ITEM -> Comparator.comparing(Opportunity::name, String.CASE_INSENSITIVE_ORDER);
            case BUY -> Comparator.comparingDouble(Opportunity::buyPrice);
            case SELL -> Comparator.comparingDouble(Opportunity::sellPrice);
            case NET -> Comparator.comparingDouble(Opportunity::netProfit);
            case SPREAD -> Comparator.comparingDouble(Opportunity::spreadPercent);
            case AGE -> Comparator.comparingLong(Opportunity::observedAt).reversed();
        };
        if (!ascending) comparator = comparator.reversed();
        rows = source.stream().filter(o -> o.name().toLowerCase(Locale.ROOT).contains(needle)
                    || o.productId().toLowerCase(Locale.ROOT).contains(needle))
                .filter(o -> o.netProfit() >= profit && o.volume() >= volume && o.spreadPercent() <= spread)
                .filter(o -> !starsOnly || stars.contains(o.productId()))
                .sorted(comparator.thenComparing(Opportunity::productId)).toList();
        if (selectedId != null && rows.stream().noneMatch(o -> o.productId().equals(selectedId))) selectedId = null;
        scrollTo(offset, capacity);
    }
    public List<Opportunity> rows() { return rows; }
    public Column column() { return column; }
    public boolean ascending() { return ascending; }
    public int offset() { return offset; }
    public void scrollTo(int value, int capacity) { offset = Math.max(0, Math.min(value, Math.max(0, rows.size() - capacity))); }
    public Opportunity atVisibleRow(int row, int capacity) {
        int index = offset + row;
        return row < 0 || row >= capacity || index >= rows.size() ? null : rows.get(index);
    }
    public void selectVisibleRow(int row, int capacity) {
        Opportunity value = atVisibleRow(row, capacity);
        if (value != null) selectedId = value.productId();
    }
    public Opportunity selected() { return rows.stream().filter(o -> o.productId().equals(selectedId)).findFirst().orElse(null); }
    public boolean selected(Opportunity value) { return value.productId().equals(selectedId); }
}
