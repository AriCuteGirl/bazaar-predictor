package com.example.bazaarpredictor.model;

public record Opportunity(String productId, String name, double buyPrice, double sellPrice,
                          double netProfit, double spreadPercent, double volume, double fillMinutes,
                          long observedAt, String risk) {
    public boolean stale(long now) { return now - observedAt > 90_000; }
}
