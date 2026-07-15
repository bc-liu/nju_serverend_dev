-- ============================================================
-- 库存回滚 Lua 脚本
-- ------------------------------------------------------------
-- 当 Redis 扣减成功但后续数据库事务失败时，调用此脚本将库存
-- 加回 Redis，保证 Redis 与最终数据状态一致。
--
-- 入参:
--   KEYS[1] = 库存 Redis Key
--   ARGV[1] = 要回滚的数量
--
-- 返回值: 回滚后的最新库存值
-- ============================================================

local quantity = tonumber(ARGV[1])
if quantity == nil or quantity <= 0 then
    return redis.call('GET', KEYS[1])
end

redis.call('INCRBY', KEYS[1], quantity)
return redis.call('GET', KEYS[1])
