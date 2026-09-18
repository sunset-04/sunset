package com.hmdp.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置 —— 异步秒杀
 * <p>
 * 交换机：seckill.direct（direct 类型）
 * 队列：  seckill.order.queue
 * routingKey：seckill.order
 */
@Configuration
public class QueueConfig {

    /** 秒杀订单交换机 */
    public static final String SECKILL_EXCHANGE = "seckill.direct";
    /** 秒杀订单队列 */
    public static final String SECKILL_QUEUE = "seckill.order.queue";
    /** 秒杀订单 routingKey */
    public static final String SECKILL_ROUTING_KEY = "seckill.order";

    /**
     * 声明 direct 类型交换机（持久化，不自动删除）
     */
    @Bean
    public DirectExchange seckillExchange() {
        return new DirectExchange(SECKILL_EXCHANGE, true, false);
    }

    /**
     * 声明秒杀订单队列（持久化）
     */
    @Bean
    public Queue seckillQueue() {
        return new Queue(SECKILL_QUEUE, true);
    }

    /**
     * 将队列按 routingKey 绑定到交换机
     */
    @Bean
    public Binding seckillBinding() {
        return BindingBuilder.bind(seckillQueue())
                .to(seckillExchange())
                .with(SECKILL_ROUTING_KEY);
    }

    /**
     * 使用 JSON 序列化消息，避免 JDK 序列化带来的类不匹配问题
     */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
