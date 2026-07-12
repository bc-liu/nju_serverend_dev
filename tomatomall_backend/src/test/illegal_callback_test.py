# -*- coding: utf-8 -*-
"""
非法回调拦截率测试脚本
测试目标：
  1. 发送N次伪造/篡改的支付宝回调请求
  2. 验证后端 Redis 计数 security:illegalCallback:count 是否等于 N
  3. 验证订单状态未被错误更新（非法回调不应改变订单状态）

运行: python illegal_callback_test.py
"""
import requests
import time
import subprocess
import json

# ========== 配置 ==========
BACKEND_URL = "http://localhost:8080"
NOTIFY_URL = BACKEND_URL + "/api/orders/notify"
TARGET_ORDER_ID = 1  # 用于测试的订单ID，需提前创建一个PENDING订单
TEST_TIMES = 25      # 发送非法回调次数（>20 才能验证"20+"）
REDIS_COUNTER_KEY = "security:illegalCallback:count"

# 已存在的合法token（用于查询订单状态）
TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI4IiwiZXhwIjoxNzgzOTE2NDA2fQ.TP9sb6flRnMXSQ92gipFpryJ3Fbv_2mnNW8L5hvkDY0"

HEADERS = {"Authorization": TOKEN}


def redis_get(key):
    """从 Redis 读取值（Spring Boot 连的是 WSL Redis，通过 host.docker.internal 访问）"""
    cmd = ["docker", "exec", "redis", "redis-cli", "-h", "host.docker.internal", "-p", "6379", "GET", key]
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=5)
        return result.stdout.strip()
    except Exception as e:
        print(f"[WARN] Redis 读取失败: {e}")
        return None


def redis_del(key):
    """删除 Redis 键"""
    cmd = ["docker", "exec", "redis", "redis-cli", "-h", "host.docker.internal", "-p", "6379", "DEL", key]
    try:
        subprocess.run(cmd, capture_output=True, text=True, timeout=5)
    except Exception:
        pass


def redis_keys(pattern):
    """列出匹配的键"""
    cmd = ["docker", "exec", "redis", "redis-cli", "-h", "host.docker.internal", "-p", "6379", "KEYS", pattern]
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=5)
        return result.stdout.strip()
    except Exception:
        return ""


def get_order_status(order_id):
    """获取订单当前状态"""
    try:
        r = requests.get(f"{BACKEND_URL}/api/orders/pendingOrders",
                         headers=HEADERS, timeout=5)
        if r.status_code == 200:
            data = r.json()
            if "data" in data and data["data"]:
                for o in data["data"]:
                    if o.get("orderId") == order_id:
                        return o.get("status")
        return None
    except Exception as e:
        print(f"[WARN] 查询订单失败: {e}")
        return None


def send_forged_callback(variant, order_id):
    """
    发送伪造的支付宝回调
    variant:
      1 = 无sign字段
      2 = sign错误（短签名，触发SignatureException）
      3 = 篡改out_trade_no
      4 = 篡改total_amount
    所有变体都带 trade_status=TRADE_SUCCESS，确保进入验签逻辑
    """
    if variant == 1:
        # 无签名字段
        data = {
            "trade_status": "TRADE_SUCCESS",
            "out_trade_no": str(order_id),
            "total_amount": "0.01",
            "trade_no": "2026" + str(int(time.time())),
            "subject": "forged",
        }
    elif variant == 2:
        # 错误签名（短签名，会触发SignatureException）
        data = {
            "trade_status": "TRADE_SUCCESS",
            "out_trade_no": str(order_id),
            "total_amount": "0.01",
            "trade_no": "2026" + str(int(time.time())),
            "subject": "forged",
            "sign": "FAKE",
            "sign_type": "RSA2",
        }
    elif variant == 3:
        # 篡改订单号 + 错误签名
        data = {
            "trade_status": "TRADE_SUCCESS",
            "out_trade_no": "999999",
            "total_amount": "0.01",
            "trade_no": "2026" + str(int(time.time())),
            "sign": "FAKE",
            "sign_type": "RSA2",
        }
    else:
        # 篡改金额 + 错误签名
        data = {
            "trade_status": "TRADE_SUCCESS",
            "out_trade_no": str(order_id),
            "total_amount": "0.00",
            "trade_no": "2026" + str(int(time.time())),
            "sign": "FAKE",
            "sign_type": "RSA2",
        }

    try:
        r = requests.post(NOTIFY_URL, data=data, timeout=5)
        return r.status_code, r.text
    except Exception as e:
        return None, str(e)


def main():
    print("=" * 60)
    print("非法回调拦截率测试")
    print("=" * 60)

    # 1. 记录初始计数
    before_count = redis_get(REDIS_COUNTER_KEY)
    before_count = int(before_count) if before_count and before_count.isdigit() else 0
    print(f"[1] 测试前 Redis 拦截计数: {before_count}")

    # 2. 记录订单初始状态
    status_before = get_order_status(TARGET_ORDER_ID)
    print(f"[2] 测试前订单 #{TARGET_ORDER_ID} 状态: {status_before}")

    # 3. 发送伪造回调
    print(f"[3] 开始发送 {TEST_TIMES} 次伪造回调...")
    variants = [1, 2, 3, 4]
    success_count = 0
    for i in range(TEST_TIMES):
        v = variants[i % len(variants)]
        code, text = send_forged_callback(v, TARGET_ORDER_ID)
        ok = code is not None
        success_count += 1 if ok else 0
        if (i + 1) % 5 == 0:
            print(f"   进度: {i+1}/{TEST_TIMES}  最近响应: code={code}")

    print(f"   共发送 {success_count}/{TEST_TIMES} 次伪造请求")

    # 4. 等待1秒，确保Redis写入完成
    time.sleep(1)

    # 5. 读取测试后计数
    after_count = redis_get(REDIS_COUNTER_KEY)
    after_count = int(after_count) if after_count and after_count.isdigit() else 0
    print(f"[4] 测试后 Redis 拦截计数: {after_count}")

    # 6. 检查订单状态是否被错误改动
    status_after = get_order_status(TARGET_ORDER_ID)
    print(f"[5] 测试后订单 #{TARGET_ORDER_ID} 状态: {status_after}")

    # 7. 结果判定
    print("\n" + "=" * 60)
    print("测试结果")
    print("=" * 60)
    delta = after_count - before_count
    print(f"拦截次数增量: {delta} / 发送 {TEST_TIMES}")
    pass_rate = delta / TEST_TIMES * 100 if TEST_TIMES > 0 else 0
    print(f"拦截率: {pass_rate:.1f}%")

    if delta >= TEST_TIMES:
        print("[PASS] 所有非法回调均被拦截")
    elif delta > 0:
        print(f"[WARN] 部分拦截，存在 {TEST_TIMES - delta} 次未被计数")
    else:
        print("[FAIL] 拦截计数未增加，请检查后端埋点")

    if status_before == status_after:
        print("[PASS] 订单状态未被非法回调篡改")
    else:
        print(f"[FAIL] 订单状态被篡改: {status_before} -> {status_after}")

    # 8. 输出按IP维度的拦截统计
    print("\n按来源IP维度的拦截统计:")
    print(redis_keys("security:illegalCallback:ip:*"))


if __name__ == "__main__":
    main()
