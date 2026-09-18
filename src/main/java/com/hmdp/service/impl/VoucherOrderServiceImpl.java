package com.hmdp.service.impl;

import com.hmdp.config.QueueConfig;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;


/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    private final ISeckillVoucherService seckillVoucherService;

    private final StringRedisTemplate stringRedisTemplate;

    private final RedisIdWorker redisIdWorker;

    private final RedissonClient redissonClient;

    private final RabbitTemplate rabbitTemplate;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Override
    public Result secKillVoucher(Long voucherId) {
        // 1.执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),
                UserHolder.getUser().getId().toString()
        );
        // 2.判断结果是为0
        int r = result.intValue();
        if (r != 0) {
            // 2.1.不为0，代表没有资格购买
            return Result.fail(r == 1 ? "库存不足" : "请勿重复下单");
        }
        // 2.2.为0，把下单信息发送到 RabbitMQ 队列（异步下单）
        VoucherOrder voucherOrder = new VoucherOrder();
        // 2.3.订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 2.4.用户id
        voucherOrder.setUserId(UserHolder.getUser().getId());
        // 2.5.代金券id
        voucherOrder.setVoucherId(voucherId);
        // 2.6.发送消息到 RabbitMQ
        log.info("【异步秒杀】发送订单消息到MQ：orderId={}, userId={}, voucherId={}",
                orderId, voucherOrder.getUserId(), voucherId);
        rabbitTemplate.convertAndSend(
                QueueConfig.SECKILL_EXCHANGE,
                QueueConfig.SECKILL_ROUTING_KEY,
                voucherOrder
        );
        // 返回订单id
        return Result.ok(orderId);
    }

    @Transactional
    public void creatVoucherOrder(VoucherOrder voucherOrder) {
        // 5.一人一单
        Long userId = voucherOrder.getUserId();
        Long voucherId = voucherOrder.getVoucherId();
        // 5.1查询订单
        Long count = lambdaQuery()
                .eq(VoucherOrder::getUserId, userId)
                .eq(VoucherOrder::getVoucherId, voucherId)
                .count();
        // 判断是否存在同一个订单
        if (count > 0) {
            // 用户已经购买过
            log.error("用户已经购买过一次：userId={}, voucherId={}", userId, voucherId);
            return;
        }
        // 6.扣减库存
        boolean success = seckillVoucherService.lambdaUpdate()
                .setSql("stock = stock - 1")
                .eq(SeckillVoucher::getVoucherId, voucherId)
                .gt(SeckillVoucher::getStock, 0)
                .update();
        if (!success) {
            // 扣减失败
            log.error("库存不足！voucherId={}", voucherId);
            return;
        }
        // 7.创建订单
        save(voucherOrder);
    }
}
