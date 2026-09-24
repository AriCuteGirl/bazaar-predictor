package com.example.bazaarpredictor.config;

public final class ClientConfig {
    public boolean hudEnabled = true;
    public int refreshSeconds = 5;
    public double minimumProfit = 100.0;
    public double minimumVolume = 100.0;
    public double maximumSpreadPercent = 50.0;
    public double availableCoins = 100_000.0;
    public boolean autoProfitMode = true;
    private static final ClientConfig INSTANCE = new ClientConfig();
    public static ClientConfig get() { return INSTANCE; }
}
