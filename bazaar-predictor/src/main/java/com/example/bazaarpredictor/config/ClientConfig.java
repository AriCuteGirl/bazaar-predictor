package com.example.bazaarpredictor.config;

public final class ClientConfig {
    public boolean hudEnabled = true;
    public int refreshSeconds = 5;
    public double minimumProfit = 100.0;
    public double minimumVolume = 100.0;
    private static final ClientConfig INSTANCE = new ClientConfig();
    public static ClientConfig get() { return INSTANCE; }
}
