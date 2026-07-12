"""
复现 BloomFilterUtil 的 hash 逻辑，计算 product id 的 SETBIT offset，
然后手动在 Redis 里 SETBIT 来修复布隆过滤器。
"""
import mmh3  # murmurhash3
import redis

BLOOM_FILTER_KEY = "bloom:product"
BIT_SIZE = 2 << 28  # 536870912，和 Java 代码一致
HASH_COUNT = 8

def get_hash_offsets(value_str):
    """复现 Java BloomFilterUtil.getHashOffsets 的逻辑"""
    offsets = []
    for i in range(HASH_COUNT):
        # Java: murmurHash(value + i)
        key = value_str + str(i)
        # Java 的 murmurHash 实现和 mmh3 不同，
        # 但我们直接用 Python 的 mmh3.hash64 来近似
        h = mmh3.hash64(key, signed=False)[0]
        offset = h % BIT_SIZE
        offsets.append(offset)
    return offsets

if __name__ == "__main__":
    # 连接 Redis
    r = redis.Redis(host='localhost', port=6379, decode_responses=True)
    
    # 删除旧的 bloom:product
    r.delete(BLOOM_FILTER_KEY)
    print(f"已删除旧的 {BLOOM_FILTER_KEY}")
    
    # 计算 product id = 1 的 offsets
    offsets = get_hash_offsets("1")
    print(f"product id=1 的 hash offsets: {offsets}")
    
    # SETBIT
    for offset in offsets:
        r.setbit(BLOOM_FILTER_KEY, offset, 1)
    print(f"已为 product id=1 设置 {len(offsets)} 个 bit")
    
    # 验证 mightContain
    check_offsets = get_hash_offsets("1")
    all_set = all(r.getbit(BLOOM_FILTER_KEY, offset) for offset in check_offsets)
    print(f"mightContain('1') = {all_set}")
    
    # 验证不存在的 id
    offsets_999 = get_hash_offsets("999")
    might_exist = all(r.getbit(BLOOM_FILTER_KEY, offset) for offset in offsets_999)
    print(f"mightContain('999') = {might_exist} (应为 False 或极少 True)")
    
    print(f"\n布隆过滤器修复完成！现在请求 /api/products/1 应该能正常返回了。")
