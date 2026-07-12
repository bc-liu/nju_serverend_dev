import requests, time, statistics
URL = "http://localhost:8080/api/products/1"

ts = []
# 第1次：L1空 -> L2 Redis命中 -> 回填L1
t0 = time.perf_counter()
requests.get(URL, timeout=10)
t1 = time.perf_counter()
print(f"第1次(L2命中): {(t1-t0)*1000:.1f} ms")

# 接下来50次：L1命中
for _ in range(50):
    t0 = time.perf_counter()
    requests.get(URL, timeout=10)
    t1 = time.perf_counter()
    ts.append((t1-t0)*1000)
print(f"L1命中 平均: {statistics.mean(ts):.1f} ms")
print(f"L1命中 P95: {sorted(ts)[int(len(ts)*0.95)]:.1f} ms")