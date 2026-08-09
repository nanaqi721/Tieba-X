-- 帖子点赞/取消赞：更新计数缓冲（供 xxl-job 刷库），摘要缓存存在则同步更新 likeCount（立即显示）
-- KEYS[1] = 计数缓冲 hash key（post_server:post_count_incr:like_count）
-- KEYS[2] = 摘要缓存 hash key（post_server:post_cache:{barId}:{postId}）
-- ARGV[1] = postId
-- ARGV[2] = 增量（1 点赞 / -1 取消）
local c = redis.call('HINCRBY', KEYS[1], ARGV[1], ARGV[2])
-- 摘要缓存存在才同步，避免新建残缺 hash（只有 likeCount 一个字段）
if redis.call('EXISTS', KEYS[2]) == 1 then
    redis.call('HINCRBY', KEYS[2], 'likeCount', ARGV[2])
end
return c
