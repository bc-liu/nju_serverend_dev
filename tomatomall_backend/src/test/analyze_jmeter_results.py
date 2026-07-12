# -*- coding: utf-8 -*-
"""
JMeter 压测结果分析脚本
解析 JTL 文件，计算成功率、QPS、响应时间百分位等指标
用法: python analyze_jmeter_results.py <result.jtl>
"""
import sys
import csv
from collections import defaultdict


def analyze(jtl_file):
    """分析 JMeter JTL 结果文件"""
    samples = []
    by_label = defaultdict(lambda: {"total": 0, "success": 0, "fail": 0, "times": []})

    with open(jtl_file, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            label = row.get("label", "unknown")
            success = row.get("success", "false").lower() == "true"
            elapsed = int(row.get("elapsed", 0))
            rc = row.get("responseCode", "")

            samples.append({"label": label, "success": success, "elapsed": elapsed, "rc": rc})

            stats = by_label[label]
            stats["total"] += 1
            if success:
                stats["success"] += 1
            else:
                stats["fail"] += 1
            stats["times"].append(elapsed)

    # ====== 总体统计 ======
    total = len(samples)
    total_success = sum(1 for s in samples if s["success"])
    total_fail = total - total_success
    success_rate = (total_success / total * 100) if total > 0 else 0

    print("=" * 70)
    print("  TomatoMall 压测结果分析")
    print("=" * 70)

    print(f"\n【总体统计】")
    print(f"  总请求数:   {total}")
    print(f"  成功请求:   {total_success}")
    print(f"  失败请求:   {total_fail}")
    print(f"  成功率:     {success_rate:.2f}%")

    if success_rate >= 99.5:
        print(f"  结论:       [PASS] 成功率 >= 99.5%")
    else:
        print(f"  结论:       [FAIL] 成功率 < 99.5%，需要优化")

    # ====== 按接口统计 ======
    print(f"\n【按接口统计】")
    print(f"  {'接口名':<45} {'总数':>6} {'成功':>6} {'失败':>6} {'成功率':>8} {'平均ms':>8} {'99%ms':>8}")
    print(f"  {'-'*45} {'-'*6} {'-'*6} {'-'*6} {'-'*8} {'-'*8} {'-'*8}")

    for label, stats in sorted(by_label.items()):
        sr = (stats["success"] / stats["total"] * 100) if stats["total"] > 0 else 0
        times = sorted(stats["times"])
        avg = sum(times) / len(times) if times else 0
        p99_index = int(len(times) * 0.99) - 1
        p99 = times[p99_index] if times and p99_index >= 0 else 0

        status = "[PASS]" if sr >= 99.5 else "[FAIL]"
        print(f"  {label:<45} {stats['total']:>6} {stats['success']:>6} {stats['fail']:>6} {sr:>7.2f}% {avg:>8.1f} {p99:>8}")

    # ====== QPS 计算 ======
    print(f"\n【吞吐量(QPS)】")
    all_times = [s["elapsed"] for s in samples]
    if all_times:
        avg_response = sum(all_times) / len(all_times)
        # QPS = 并发数 / 平均响应时间(秒)
        qps_estimate = 500 / (avg_response / 1000) if avg_response > 0 else 0
        print(f"  平均响应时间: {avg_response:.1f} ms")
        print(f"  估算 QPS:     {qps_estimate:.1f} req/s (基于500并发)")

    # ====== 错误分析 ======
    if total_fail > 0:
        print(f"\n【错误分析】")
        error_types = defaultdict(int)
        for s in samples:
            if not s["success"]:
                error_types[s["rc"]] += 1
        for rc, count in sorted(error_types.items(), key=lambda x: -x[1]):
            print(f"  HTTP {rc}: {count} 次")

    print("\n" + "=" * 70)
    if success_rate >= 99.5:
        print("  [最终结论] 500并发成功率 >= 99.5%，测试通过！")
    else:
        print(f"  [最终结论] 成功率 {success_rate:.2f}%，未达到 99.5% 目标")
    print("=" * 70)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("用法: python analyze_jmeter_results.py <result.jtl>")
        sys.exit(1)
    analyze(sys.argv[1])
