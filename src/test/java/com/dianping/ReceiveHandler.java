package com.dianping;


import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


import javax.annotation.Resource;

@SpringBootTest
@RunWith(SpringRunner.class)
public class ReceiveHandler {
    @Resource
    RabbitTemplate rabbitTemplate;
    @Test
    public void Producer(){
        try {
        ConnectionFactory factory = new ConnectionFactory();
        // 设置服务端的地址、端口、用户名和密码...
        factory.setHost("47.115.201.202");
        factory.setPort(5672);
        factory.setPassword("123");
        factory.setUsername("admin");
        factory.setVirtualHost("/");
        Connection connection = null;
        Channel channel = null;
            connection=factory.newConnection();
            channel = connection.createChannel();


        // 从队列中获取消息，不自动确认
        GetResponse response  = channel.basicGet("queue_inform_orders", false);
        if(null != response) {
            byte[] body = response.getBody();
            String message = new String(body);
            System.out.println("Received: " + message);
            // 手工确认
            long deliveryTag = response.getEnvelope().getDeliveryTag();
            channel.basicAck(deliveryTag, false);
        }

        channel.close();
        connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
