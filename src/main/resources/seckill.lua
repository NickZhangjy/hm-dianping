 -- 参数列表
 -- 优惠卷id
 local voucherId = ARGV[1]
 -- 用户id
 local userId= ARGV[2]
 -- 订单id
 local orderId= ARGV[3]

-- 数据key
-- 库存key
local stockKey = 'seckill:stock:' .. voucherId
-- 订单key
local orderKey = 'seckill:order:' .. voucherId

-- 脚本业务
-- 判断库存是否充足
if(tonumber(redis.call('get', stockKey)) <= 0) then
    -- 库存不足，返回1
    return 1
end

-- 判断用户是否重复下单
if (redis.call('sismember', orderKey, userId) == 1) then
    -- 重复下单
    return 2
end

-- 扣库存
redis.call('incrby', stockKey, -1)
-- 下单（保存用户）
redis.call('sadd', orderKey, userId)
-- 发送消息到队列当中
redis.call('xadd', 'stream.orders', '*', 'userId', userId, 'voucherId', voucherId, 'id', orderId)
return 0

