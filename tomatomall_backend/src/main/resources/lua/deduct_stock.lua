-- ============================================================
-- 原子性库存扣减 Lua 脚本
-- ------------------------------------------------------------
-- 利用 Redis 单线程执行 Lua 脚本的特性，将"检查 + 扣减"合并为
-- 一个不可分割的原子操作，从根本上避免并发条件下的超卖问题。
--
-- 入参:
--   KEYS[1] = 库存 Redis Key, 例如 "stock:product:1:amount"
--   ARGV[1] = 本次要扣减的数量 (quantity)
--
-- 返回值:
--    1  = 扣减成功
--    0  = 库存不足 (current < quantity)
--   -1  = Redis 中未初始化该商品的库存 (需要从 DB 加载)
-- ============================================================

local current = redis.call('GET', KEYS[1])
if not current then
    return -1
end

local stock = tonumber(current)
local quantity = tonumber(ARGV[1])

if stock == nil or quantity == nil or quantity <= 0 then
    return 0
end

if stock >= quantity then
    redis.call('DECRBY', KEYS[1], quantity)
    return 1
else
    return 0
end
