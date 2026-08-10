-- bar 关注数：更新计数缓冲（供 xxl-job 刷库），详情缓存存在则同步更新 followerCount 字段（立即显示）
-- KEYS[1] = 计数缓冲 hash key（bar_server:bar_count_incr:follower_count）
-- KEYS[2] = 详情缓存 hash key（bar_server:bar_cache:{barId}）
-- ARGV[1] = barId
-- ARGV[2] = 增量（1 增 / -1 减）
-- ARGV[3] = 详情缓存字段名（followerCount）
local c = redis.call('HINCRBY', KEYS[1], ARGV[1], ARGV[2])
-- 详情缓存存在才同步，避免新建残缺 hash（只有单个计数字段）
if redis.call('EXISTS', KEYS[2]) == 1 then
    redis.call('HINCRBY', KEYS[2], ARGV[3], ARGV[2])
end
return c
