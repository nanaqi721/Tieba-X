-- KEYS[1] = 缓存key
-- ARGV[1] = ttl 秒
-- ARGV[2] = field value field value ...
-- unpack截取参数 从2开始进行 #ARGV表示全部长度
redis.call('HSET',KEYS[1],unpack(ARGV,2,#ARGV))
return redis.call('EXPIRE',KEYS[1],ARGV[1])
