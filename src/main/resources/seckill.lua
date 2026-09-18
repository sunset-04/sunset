---@diagnostic disable: undefined-global
-- 1.参数列表
-- 1.1优惠券id
local voucherId = ARGV[1]
-- 1.2用户id
local userId = ARGV[2]

-- 2.数据key
-- 2.1库存key
local stockKey = "seckill:stock:" .. voucherId
-- 2.2订单key
local orderKey = "seckill:order:" .. voucherId

-- 3.业务逻辑
-- 3.1.判断库存是否充足
local stock = tonumber(redis.call('get', stockKey))
if(stock == nil or stock <= 0) then
    -- 3.2.库存不足，返回1
    return 1
end
-- 3.3.判断用户是否下单
if(redis.call('sismember', orderKey, userId) == 1) then
    --3.4.存在，说明重复下单，返回2
    return 2
end 
-- 3.5.库存充足，扣库存
redis.call('incrby', stockKey, -1)
-- 3.6.下单
redis.call('sadd', orderKey, userId)
return 0