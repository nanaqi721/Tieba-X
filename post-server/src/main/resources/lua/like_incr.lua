-- 帖子点赞/收藏/评论计数：更新计数缓冲（供 xxl-job 刷库），摘要缓存存在则同步更新对应计数字段（立即显示）
-- KEYS[1] = 计数缓冲 hash key（post_server:post_count_incr:{metric}）
-- KEYS[2] = 摘要缓存 hash key（post_server:post_cache:{barId}:{postId}）
-- ARGV[1] = postId
-- ARGV[2] = 增量（1 增 / -1 减）
-- ARGV[3] = 摘要缓存字段名（likeCount / favoriteCount / commentCount ...）
local c = redis.call('HINCRBY', KEYS[1], ARGV[1], ARGV[2])
-- 摘要缓存存在才同步，避免新建残缺 hash（只有单个计数字段）
if redis.call('EXISTS', KEYS[2]) == 1 then
    redis.call('HINCRBY', KEYS[2], ARGV[3], ARGV[2])
end
return c
