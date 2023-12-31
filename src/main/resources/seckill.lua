--1.参数列表
--1.1.优惠券id
local voucherId = ARGV[1]
--1.2.用户id
local userId = ARGV[2]

--2.数据key
--2.1.库存key
local stockKey = 'seckill:stock:'..voucherId
--2.2.订单key
local orderKey = 'seckill:order:'..voucherId

--3.业务脚本
--3.1.判断库存是否充足
if (tonumber(redis.call('get',stockKey))<=0) then
    --3.1.1库存不足返回1
    return 1
end
--3.2.判断用户是否重复下单
if (redis.call('sismember',orderKey,userId)==1) then
    --3.2.1.存在，说明是重复下单
    return 2
end
--3.4.扣减库存
redis.call('incrby',stockKey,-1)
--3.5.下单（保存用户）
reids.call('sadd',orderKey,userId)

return 0