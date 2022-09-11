package com.deemo.transaction.template;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

@Slf4j
// 这里的 txProducerGroup 的值要与发送半消息时保持一致
// @RocketMQTransactionListener(txProducerGroup = "Tx_Charge_Group")
// 现在不需要指定 txProducerGroup
@RocketMQTransactionListener
public class MQTXLocalService implements RocketMQLocalTransactionListener {

    @Autowired
    private UserService userService;
    @Autowired
    private MQTransactionLogMapper mqTransactionLogMapper;

    /**
     * 用于执行本地事务的方法
     */
    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message message, Object obj) {
        // 获取消息体里参数
        MessageHeaders messageHeaders = message.getHeaders();
        String transactionId = (String) messageHeaders.get(RocketMQHeaders.TRANSACTION_ID);
        log.info("【执行本地事务】消息体参数：transactionId={}", transactionId);

        // 执行带有事务注解的本地方法：增加用户余额+保存mq日志
        try {
            UserCharge userCharge = (UserCharge) obj;
            userService.addBalance(userCharge, transactionId);
            return RocketMQLocalTransactionState.COMMIT; // 正常：向MQ Server发送commit消息
        } catch (Exception e) {
            log.error("【执行本地事务】发生异常，消息将被回滚", e);
            return RocketMQLocalTransactionState.ROLLBACK; // 异常：向MQ Server发送rollback消息
        }
    }

    /**
     * 用于回查本地事务执行结果的方法
     */
    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message message) {
        MessageHeaders headers = message.getHeaders();
        String transactionId = headers.get(RocketMQHeaders.TRANSACTION_ID, String.class);
        log.info("【回查本地事务】transactionId={}", transactionId);

        // 根据事务id查询事务日志表
        MQTransactionLog mqTransactionLog = mqTransactionLogMapper.selectByPrimaryKey(transactionId);
        if (null == mqTransactionLog) { // 没查到表明本地事务执行失败,通知回滚
            return RocketMQLocalTransactionState.ROLLBACK;
        }
        return RocketMQLocalTransactionState.COMMIT; // 查到表明本地事务执行成功，提交
    }
}
