package com.dianping;

import com.alibaba.fastjson.JSON;
import com.dianping.config.RabbitmqConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
@RunWith(SpringRunner.class)
public class ProdcerTopics {
    @Resource
    RabbitTemplate rabbitTemplate;
    @Test
    public void Producer(){
        //使用rabbitTemplate发送消息
//        VoucherOrder voucherOrder = new VoucherOrder();
//        voucherOrder.setVoucherId(1L);
//        voucherOrder.setUserId(5L);
//        voucherOrder.setId(2L);
//        String o = JSON.toJSONString(voucherOrder);
//        System.out.println(o);
        Map<String,Object> map = new HashMap<>();
        map.put("userId",1L);
        map.put("Id",2L);
        map.put("voucherId",3L);
        String o = JSON.toJSONString(map);
        System.out.println(map);
        System.out.println(o);
        /**
         * 参数：
         *1.exchange name
         *2.routingkey
         *3.message context
         */
        for (int  i = 0 ; i<50 ; i++)
        {
            System.out.println(i);
            rabbitTemplate.convertAndSend(RabbitmqConfig.EXCHANGE_TOPICS_INFORM,"inform.orders", o);
        }

    }
}
