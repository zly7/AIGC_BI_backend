package com.yupi.springbootinit.manager;

import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
@Service
public class RedisLimiterManager {
    @Resource
    private RedissonClient redissonClientLimiter;
    public void doLimit(String key){
        RRateLimiter rateLimiter = redissonClientLimiter.getRateLimiter(key);
        rateLimiter.trySetRate(RateType.OVERALL, 2, 1, RateIntervalUnit.SECONDS);
        boolean canOperate = rateLimiter.tryAcquire(1);
        if(!canOperate){
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST,"用户被限流器限制");
        }
    }
}
