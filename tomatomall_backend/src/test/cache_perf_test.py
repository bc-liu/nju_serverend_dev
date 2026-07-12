import requests, time, statistics

BASE = "http://localhost:8080"
PID = 1
URL = f"{BASE}/api/products/{PID}"

def measure(n=50):
    ts = []
    for _ in range(n):
        t0 = time.perf_counter()
        r = requests.get(URL, timeout=10)
        t1 = time.perf_counter()
        ts.append((t1 - t0) * 1000)  # ms
        if r.status_code != 200:
            print("warn:", r.status_code, r.text[:100])
    return ts

print("=== 第1次请求（冷启动，穿透到DB）===")
cold = measure(1)
print(f"  耗时: {cold[0]:.1f} ms")

print("=== 接下来 50 次（应命中 L1 Caffeine）===")
warm_l1 = measure(50)
print(f"  平均: {statistics.mean(warm_l1):.1f} ms")
print(f"  中位数: {statistics.median(warm_l1):.1f} ms")
print(f"  P95: {sorted(warm_l1)[int(len(warm_l1)*0.95)]:.1f} ms")

# 模拟 L1 失效：重启服务后立即请求（L1 空，L2 Redis 命中）
# 此处仅打印提示，需手动重启服务后再跑下面一段
print("\n>>> 请重启后端服务（清空 Caffeine，保留 Redis），然后运行 cache_perf_l2.py <<<")