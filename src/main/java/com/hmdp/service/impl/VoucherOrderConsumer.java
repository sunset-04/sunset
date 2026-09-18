package com.hmdp.service.impl;

import com.hmdp.config.QueueConfig;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.IVoucherOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 秒杀订单消费者 —— 监听 RabbitMQ 队列，异步完成下单落库
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoucherOrderConsumer {

    private final IVoucherOrderService voucherOrderService;

    /**
     * 监听秒杀订单队列：收到消息后创建订单
     *
     * @param voucherOrder 秒杀订单消息
     */
    @RabbitListener(queues = QueueConfig.SECKILL_QUEUE)
    public void onVoucherOrder(VoucherOrder voucherOrder) {
        log.info("【RabbitMQ消费者】收到秒杀订单消息：orderId={}, userId={}, voucherId={}",
                voucherOrder.getId(), voucherOrder.getUserId(), voucherOrder.getVoucherId());
        try {
            voucherOrderService.creatVoucherOrder(voucherOrder);
            log.info("【RabbitMQ消费者】秒杀订单处理完成：orderId={}", voucherOrder.getId());
        } catch (Exception e) {
            log.error("【RabbitMQ消费者】秒杀订单处理异常：orderId={}", voucherOrder.getId(), e);
        }
    }
}
