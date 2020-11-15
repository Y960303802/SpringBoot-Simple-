package com.xizi.miaosha.controller;


import com.google.common.util.concurrent.RateLimiter;
import com.xizi.miaosha.service.OrderService;
import com.xizi.miaosha.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * jmeter压力测试  jmeter -n -t [文件地址]
 */


/**
 * 1.限时抢购       (redis )
 * 2.抢购接口隐藏
 * 3.单用户限制频率(单位时间内限制访问次数)
 */
@RestController
@RequestMapping("/stock")
@Slf4j
public class StockController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    //创建令牌桶的实例
    //每次令牌桶放行10个请求
    private RateLimiter rateLimiter= RateLimiter.create(10);


    //生成md5值得方法
    @RequestMapping("md5")
    public String getMd5(Integer id,Integer userId){
        String md5;
        try {
            md5=orderService.getMd5(id,userId);
        } catch (Exception e) {
            e.printStackTrace();
            return "获取md5失败: "+e.getMessage();
        }
        return "获取md5信息为: "+md5;
    }

    //开发秒杀方法  使用悲观锁防止超卖
    @RequestMapping(value = "/kill0",method = RequestMethod.GET)
    public  String kill0(Integer id){
        try {
            //悲观锁  同步代码块 同步执行 效率降低
            //保证当前线程得执行比事务大
//            synchronized(this){
            System.out.println("秒杀商品的id： "+id);
            //根据秒杀的商品id 去调用秒杀业务
            Integer orderId = orderService.kill(id);
            return "秒杀成功，订单id为："+orderId;
//            }

        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }


    //开发秒杀方法  使用乐观锁防止超卖
    @RequestMapping(value = "/kill",method = RequestMethod.GET)
    public  String kill(Integer id){
        try {
            //悲观锁  同步代码块 同步执行 效率降低
            //保证当前线程得执行比事务大
//            synchronized(this){
            System.out.println("秒杀商品的id： "+id);
                //根据秒杀的商品id 去调用秒杀业务
                Integer orderId = orderService.kill(id);
                return "秒杀成功，订单id为："+orderId;
//            }

        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    //开发秒杀方法  使用乐观锁防止超卖+令牌桶算法限流
    @RequestMapping(value = "/killtoken",method = RequestMethod.GET)
    public  String killtoken(Integer id){

        //1.没有获取到token请求一直知道获取到token 令牌
//        log.info("等待的时间: "+  rateLimiter.acquire());
//            加入令牌桶的限流措施
            if(!rateLimiter.tryAcquire(2,TimeUnit.SECONDS )){
                log.info("抛弃的请求：抢购失败，当前秒杀活动过于火爆，请重试");
                return "抢购失败，当前秒杀活动过于火爆，请重试";
            }
        System.out.println("秒杀商品的id： "+id);
        try {
            //根据秒杀的商品id 去调用秒杀业务
            Integer orderId = orderService.kill(id);
            System.out.println("秒杀成功，订单id为："+orderId);
            return "秒杀成功，订单id为："+orderId;

        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    //开发秒杀方法  使用乐观锁防止超卖+令牌桶算法限流+md5加密(id+userId)
//    抢购接口隐藏  不能直接访问 必须先进行MD5加密存入redis 在请求接口的时候比较MD5是否相等
    @RequestMapping(value = "/killtokenmd5",method = RequestMethod.GET)
    public  String killtokenmd5(Integer id,Integer userId,String md5){
        System.out.println("秒杀商品的id： "+id);
        //加入令牌桶的限流措施
        if(!rateLimiter.tryAcquire(2,TimeUnit.SECONDS )){
            log.info("抛弃的请求：抢购失败，当前秒杀活动过于火爆，请重试");
            return "抢购失败，当前秒杀活动过于火爆，请重试";
        }

        try {
            //根据秒杀的商品id 去调用秒杀业务
            Integer orderId = orderService.kill(id,userId,md5);
            System.out.println("秒杀成功，订单id为："+orderId);
            return "秒杀成功，订单id为："+orderId;

        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    //开发秒杀方法  使用乐观锁防止超卖+令牌桶算法限流+md5加密(id+userId)+单用户次数调用频率
    @RequestMapping(value = "/killtokenmd5limit",method = RequestMethod.GET)
    public  String killtokenmd5limit(Integer id,Integer userId,String md5){
        System.out.println("秒杀商品的id： "+id);
        //加入令牌桶的限流措施
        if(!rateLimiter.tryAcquire(2,TimeUnit.SECONDS )){
            log.info("抛弃的请求：抢购失败，当前秒杀活动过于火爆，请重试");
            return "抢购失败，当前秒杀活动过于火爆，请重试";
        }
        try {
            //加入单用户
            int count = userService.saveUserCount(userId);
            log.info("用户截止该次访问次数为：[{}]",count);
            //进行判断
            boolean userCount = userService.getUserCount(id);
            if(userCount){
               log.info("购买失败，超过频率限制！");
               return "购买失败，超过频率限制！";
            }
            //根据秒杀的商品id 去调用秒杀业务
            Integer orderId = orderService.kill(id,userId,md5);
            System.out.println("秒杀成功，订单id为："+orderId);
            return "秒杀成功，订单id为："+orderId;

        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }




    @GetMapping("sale")
    public String sale(Integer id){
        //1. 没有获取到token请求一直直到获取到token 令牌
//        log.info("等待的时间"+rateLimiter.acquire());

        //2.设置一个等待时间，如果在等待的时间内获取到了token 令牌，则处理业务，如果在等待的时间内没有获取token 则抛弃
        if(!rateLimiter.tryAcquire(2, TimeUnit.SECONDS)){
            System.out.println("当前请求被限流了，直接抛弃，无法调用后续秒杀逻辑");
            return "抢购失败";
        }
        System.out.println("处理业务................");
        return "抢购成功";
    }
}
