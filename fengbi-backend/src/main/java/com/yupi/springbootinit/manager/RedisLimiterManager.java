package com.yupi.springbootinit.manager;

import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 专门提供RedisLimiter限流基础服务（提供了通用的能力）
 */
@Service
public class RedisLimiterManager {

    @Resource
    private RedissonClient redissonClient;


    /**
     * 限流操作
     * @Params key 区分不同的限流器，比如不同的 id 应该分别统计
     */

    public void doRateLimit(String key){
        //创建限流器，限制每秒请求2次
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.trySetRate(RateType.OVERALL,2,1, RateIntervalUnit.SECONDS);

        //每当来了一个操作后，请求一个令牌
        boolean capOp = rateLimiter.tryAcquire(1);
        if(!capOp){
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }
    }
}
