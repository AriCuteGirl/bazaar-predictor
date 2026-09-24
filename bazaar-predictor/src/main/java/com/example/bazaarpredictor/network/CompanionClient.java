package com.example.bazaarpredictor.network;
import com.example.bazaarpredictor.model.Opportunity;
import com.google.gson.*;
import java.net.URI; import java.net.http.*; import java.util.*; import java.util.concurrent.*;
public final class CompanionClient {
 private final HttpClient http=HttpClient.newHttpClient();
 private final ScheduledExecutorService executor=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"bazaar-predictor-http");t.setDaemon(true);return t;});
 private volatile List<Opportunity> opportunities=List.of(); private volatile String status="Connecting to companion service...";
    public void start(){executor.scheduleWithFixedDelay(this::refresh,0,5,TimeUnit.SECONDS);} public List<Opportunity> opportunities(){return opportunities;} public String status(){return status;} public void close(){executor.shutdownNow();}
    public void refreshNow(){executor.execute(this::refresh);}
 private void refresh(){try{var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:8765/opportunities?limit=100")).GET().build();http.sendAsync(req,HttpResponse.BodyHandlers.ofString()).thenAccept(res->{if(res.statusCode()!=200){status="Companion HTTP "+res.statusCode();return;}try{var root=JsonParser.parseString(res.body()).getAsJsonObject();var next=new ArrayList<Opportunity>();for(var e:root.getAsJsonArray("opportunities")){var o=e.getAsJsonObject();next.add(new Opportunity(o.get("productId").getAsString(),o.get("name").getAsString(),o.get("buyPrice").getAsDouble(),o.get("sellPrice").getAsDouble(),o.get("netProfit").getAsDouble(),o.get("spreadPercent").getAsDouble(),o.get("volume").getAsDouble(),o.get("fillMinutes").getAsDouble(),o.get("observedAt").getAsLong(),o.get("risk").getAsString()));}opportunities=List.copyOf(next);status="Live • "+next.size()+" opportunities";}catch(RuntimeException ex){status="Invalid companion response";}}).exceptionally(ex->{status="Companion unavailable";return null;});}catch(RuntimeException ex){status="Companion unavailable";}}
}
