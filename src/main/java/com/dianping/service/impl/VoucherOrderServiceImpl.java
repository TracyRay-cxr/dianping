package com.dianping.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dianping.config.RabbitmqConfig;
import com.dianping.dto.Result;
import com.dianping.dto.UserDTO;
import com.dianping.entity.VoucherOrder;
import com.dianping.mapper.VoucherOrderMapper;
import com.dianping.service.ISeckillVoucherService;
import com.dianping.service.IVoucherOrderService;
import com.dianping.utils.RedisIdWorker;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper,VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static{
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    @PostConstruct
    private void init(){
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    private class VoucherOrderHandler implements Runnable{
        @Override
        public void run() {
            try {
                ConnectionFactory factory = new ConnectionFactory();
                // 设置服务端的地址、端口、用户名和密码...
                factory.setHost("47.115.201.202");
                factory.setPort(5672);
                factory.setPassword("123");
                factory.setUsername("admin");
                factory.setVirtualHost("/");
                Connection connection = factory.newConnection();
                Channel channel =connection.createChannel() ;
                VoucherOrder voucherOrder =null;
                int i =0;
            while (true) {
                    // 从队列中获取消息，不自动确认
                    GetResponse response  = channel.basicGet("queue_inform_orders", false);
                    if(null != response) {
                        byte[] body = response.getBody();
                        String message = new String(body);
                        //解析JSON字符串为订单对象
                        voucherOrder = JSON.parseObject(message, VoucherOrder.class);
                        //创建订单
                        createVoucherOrder(voucherOrder);
//                      System.out.println((i++)+voucherOrder.toString());
                        // 手工确认
                        long deliveryTag = response.getEnvelope().getDeliveryTag();
                        channel.basicAck(deliveryTag, false);
                    }
            }
            } catch (Exception e) {
            log.error("处理订单异常", e);
            e.printStackTrace();
        }

        }

    }
    private void createVoucherOrder(VoucherOrder voucherOrder){
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();
        //获取锁对象
        RLock redisLock = redissonClient.getLock("lock:order:"+userId);
        //尝试获取锁
        boolean isLock = redisLock.tryLock();
        //判断
        if (!isLock){
            //获取锁失败，直接返回失败信息
            log.error("不允许重复下单");
            return;
        }

        try{
            //查询订单
            int count = query().eq("user_id",userId).eq("voucher_id",voucherId).count();
            //判断是否存在
            if (count>0){
                //用户已经购买过
                log.error("不允许重复下单");
                return;
            }
            //扣减库存
            boolean success = seckillVoucherService.update()
                    .setSql("stock = stock - 1")//set stock = stock - 1
                    .eq("voucher_id",voucherId).gt("stock",0)
                    .update();
            if (!success) {
                //扣减失败
                log.error("库存不足！");
                return;
            }
            //创建订单
            save(voucherOrder);
        }finally{
            //释放锁
            redisLock.unlock();
        }
    }


    @Override
    public Result seckillVoucher(Long voucherId) {
        UserDTO user = (UserDTO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = user.getId();
        long orderId = redisIdWorker.nextId("order");
        Long result = stringRedisTemplate.execute(SECKILL_SCRIPT, Collections.emptyList(), voucherId.toString(), userId.toString());
        int res = result.intValue();
        //判断是否为0
        if (res!=0){
            //不为零，返回错误信息
          Result.error( res==1?"库存不足" : "不能重复下单") ;
        }
        //将订单发送到消息队列，异步添加订单
        Map<String,Long> message = new HashMap<>();
        message.put("userId",userId);
        message.put("Id",orderId);
        message.put("voucherId",voucherId);
        String msgJSON = JSON.toJSONString(message);

        rabbitTemplate.convertAndSend(RabbitmqConfig.EXCHANGE_TOPICS_INFORM,"inform.orders",msgJSON);
        //返回订单id
        return Result.ok(orderId);
    }
}
