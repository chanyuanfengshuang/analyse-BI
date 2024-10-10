package com.yupi.springbootinit.mq;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author bood
 * @since 2024/05/23 8:06
 */
@SpringBootTest
class MyMessageProducerTest {

    @Resource MyMessageProducer myMessageProducer;

    @Test
    void sendMessage(){
//        myMessageProducer.sendMessage("你好");
    }
}