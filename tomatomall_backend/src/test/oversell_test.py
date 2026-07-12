import requests, threading, time
import urllib3
urllib3.disable_warnings()

BASE = "http://localhost:8080"
TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI4IiwiZXhwIjoxNzgzOTE0ODM1fQ.anKrySQDyWqwef4HzqLu5axoehZHv8-zu_7OEWIedzk"
PRODUCT_ID = 2
THREADS = 100

# 创建 session 并设�?UTF-8 编码
session = requests.Session()
session.headers.update({"token": TOKEN})

results = {"success": 0, "fail": 0, "errors": []}
lock = threading.Lock()

def worker():
    try:
        # 1. 加入购物车（接口�?@RequestParam，必须用 params�?
        r = session.post(f"{BASE}/api/cart",
                        params={"productId": PRODUCT_ID, "quantity": 1},
                        timeout=15)
        r.encoding = 'utf-8'  # 明确指定响应编码
        
        if r.status_code != 200:
            with lock:
                results["fail"] += 1
                if len(results["errors"]) < 3:
                    results["errors"].append(f"addCart: {r.status_code} {r.text[:200]}")
            return
        resp = r.json()
        if resp.get("code") != "200":
            with lock:
                results["fail"] += 1
                if len(results["errors"]) < 3:
                    results["errors"].append(f"addCart code: {resp}")
            return
        cart_id = resp["data"]["cartItemId"]
        
        # 2. 立即结算（@RequestParam List<Integer> cartItemId, 纯英文地址�?
        r2 = session.post(f"{BASE}/api/cart/checkout",
                         params={"cartItemId": cart_id,
                                 "shoppingAddress": "test address",
                                 "paymentMethod": "ALIPAY"},
                         timeout=15)
        r2.encoding = 'utf-8'  # 明确指定响应编码
        
        with lock:
            if r2.status_code == 200 and r2.json().get("code") == "200":
                results["success"] += 1
            else:
                results["fail"] += 1
                if len(results["errors"]) < 3:
                    results["errors"].append(f"checkout: {r2.text[:200]}")
    except Exception as e:
        with lock:
            results["fail"] += 1
            if len(results["errors"]) < 3:
                results["errors"].append(str(e))

t0 = time.time()
ts = [threading.Thread(target=worker) for _ in range(THREADS)]
for t in ts: t.start()
for t in ts: t.join()
dt = time.time() - t0

print(f"并发�? {THREADS}")
print(f"成功下单: {results['success']}")
print(f"失败(含库存不�?: {results['fail']}")
print(f"总耗时: {dt:.2f}s")
print(f"QPS: {THREADS/dt:.1f}")
print(f"错误样本: {results['errors']}")
