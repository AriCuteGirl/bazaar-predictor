import json, os, threading, time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs
import requests

ROOT = os.path.dirname(os.path.abspath(__file__))
CONFIG_PATH = os.path.join(ROOT, "config.json")
state = {"opportunities": [], "observedAt": 0, "status": "starting"}
lock = threading.Lock()

def load_config():
    try:
        with open(CONFIG_PATH, encoding="utf-8") as f:
            return json.load(f)
    except (OSError, ValueError):
        return {"api_key": "", "poll_seconds": 60, "min_profit": 100, "min_volume": 100}

def order_stats(summary):
    if not summary:
        return 0.0, 0.0
    price = max((x.get("pricePerUnit", 0) for x in summary), default=0.0)
    volume = sum(x.get("orders", 0) * x.get("amount", 0) for x in summary)
    return price, volume

def poll():
    global state
    while True:
        cfg = load_config()
        now = int(time.time() * 1000)
        try:
            headers = {"API-Key": cfg["api_key"]} if cfg.get("api_key") else {}
            response = requests.get("https://api.hypixel.net/v2/skyblock/bazaar", headers=headers, timeout=15)
            response.raise_for_status()
            products = response.json().get("products", {})
            rows = []
            for product_id, product in products.items():
                buy_price, buy_volume = order_stats(product.get("buy_summary", []))
                sell_price, sell_volume = order_stats(product.get("sell_summary", []))
                if not buy_price or not sell_price or buy_price <= sell_price:
                    continue
                gross = buy_price - sell_price
                net = gross - buy_price * 0.0125
                volume = min(buy_volume, sell_volume)
                if net < cfg.get("min_profit", 100):
                    continue
                rows.append({
                    "productId": product_id, "name": product_id.replace("_", " "),
                    "buyPrice": sell_price, "sellPrice": buy_price,
                    "netProfit": round(net, 2),
                    "spreadPercent": round(gross / max(sell_price, 1) * 100, 2),
                    "volume": round(volume, 1),
                    "fillMinutes": round(1000000 / max(volume, 1), 1),
                    "observedAt": now,
                    "risk": "ok" if volume >= cfg.get("min_volume", 100) else "low-volume",
                })
            rows.sort(key=lambda row: row["netProfit"], reverse=True)
            with lock:
                state = {"opportunities": rows, "observedAt": now, "status": "live"}
        except Exception as exc:
            with lock:
                state = {"opportunities": [], "observedAt": now, "status": "error", "error": type(exc).__name__}
        time.sleep(max(30, int(load_config().get("poll_seconds", 60))))

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if urlparse(self.path).path != "/opportunities":
            self.send_error(404)
            return
        with lock:
            body = dict(state)
        limit = int(parse_qs(urlparse(self.path).query).get("limit", [100])[0])
        body["opportunities"] = body["opportunities"][:max(1, min(limit, 100))]
        raw = json.dumps(body).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

    def log_message(self, *_):
        pass

if __name__ == "__main__":
    threading.Thread(target=poll, daemon=True).start()
    print("Bazaar Predictor companion listening on http://127.0.0.1:8765")
    ThreadingHTTPServer(("127.0.0.1", 8765), Handler).serve_forever()
