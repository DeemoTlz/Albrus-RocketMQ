package com.deemo.transaction.template;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RocketMQMessageListener(topic = "RLT_TEST_TOPIC", selectorExpression = "charge", consumerGroup = "Con_Group_Four") // topic、tag保持一致
public class MQTXConsumerService implements RocketMQListener<UserCharge> {

    @Autowired
    private CreditMapper creditMapper;

    @Override
    public void onMessage(UserCharge userCharge) {
        // 一般真实环境这里消费前，得做幂等性判断，防止重复消费
        // 方法一：如果你的业务中有某个字段是唯一的，有标识性，如订单号，那就可以用此字段来判断
        // 方法二：新建一张消费记录表t_mq_consumer_log，字段consumer_key是唯一性，能插入则表明该消息还未消费，往下走，否则停止消费
        // 我个人建议用方法二，根据你的项目业务来定义key，这里我就不做幂等判断了，因为此案例只是模拟，重在分布式事务

        // 给用户增加积分
        int i = creditMapper.addNumber(userCharge.getUserId(), userCharge.getChargeAmount());
        if (1 == i) {
            log.info("【MQ消费】用户增加积分成功，userCharge={}", JSONObject.toJSONString(userCharge));
        } else {
            log.error("【MQ消费】用户充值增加积分消费失败，userCharge={}", JSONObject.toJSONString(userCharge));
        }
    }
}
