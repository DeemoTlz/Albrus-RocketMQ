package com.deemo.transaction.template;

import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.springframework.beans.factory.annotation.Autowired;

public class RocketMQController {

    @Autowired
    private MQTXProducerService mqtxProducerService;

    @PostMapping("/charge")
    public Result<TransactionSendResult> charge(UserCharge userCharge) {
        TransactionSendResult sendResult = mqtxProducerService.sendHalfMsg(userCharge);
        return Result.success(sendResult);
    }
}
