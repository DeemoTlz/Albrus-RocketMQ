package com.deemo.transaction.template;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private MQTransactionLogMapper mqTransactionLogMapper;

    /**
     * 用户增加余额+事务日志
     */
    @Transactional(rollbackFor = Exception.class)
    public void addBalance(UserCharge userCharge, String transactionId) {
        // 1. 增加余额
        userMapper.addBalance(userCharge.getUserId(), userCharge.getChargeAmount());
        // 2. 写入mq事务日志
        saveMQTransactionLog(transactionId, userCharge);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveMQTransactionLog(String transactionId, UserCharge userCharge) {
        MQTransactionLog transactionLog = new MQTransactionLog();
        transactionLog.setTransactionId(transactionId);
        transactionLog.setLog(JSON.toJSONString(userCharge));
        mqTransactionLogMapper.insertSelective(transactionLog);
    }

}
