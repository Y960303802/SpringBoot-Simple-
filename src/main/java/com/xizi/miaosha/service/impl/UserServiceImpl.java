package com.xizi.miaosha.service.impl;

import com.xizi.miaosha.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    //根据不同用户id生成调用次数的key


    @Override
    public int saveUserCount(Integer userId) {
        String limitKey="LIMIT"+"_"+userId;
        //获取redis中指定key的调用次数
        String limitNum = stringRedisTemplate.opsForValue().get(limitKey);
        int limit=-1;
        if(limitNum==null){
            //第一次调用放入redis中设置为0
            stringRedisTemplate.opsForValue().set(limitKey, "0",3600, TimeUnit.SECONDS);
        }else{
            //不是第一次调用每次+1
             limit= Integer.parseInt(limitNum) + 1;
             stringRedisTemplate.opsForValue().set(limitKey, String.valueOf(limit),3600, TimeUnit.SECONDS);
        }
        return limit; //返回调用次数
    }

    @Override
    public boolean getUserCount(Integer userId) {
        String limitKey="LIMIT"+"_"+userId;
        String limitNum = stringRedisTemplate.opsForValue().get(limitKey);
        if(limitKey==null){
            log.error("该用户没有申请验证值记录，异常");
            return  true;
        }
        return Integer.parseInt(limitNum)>10;
    }
}
