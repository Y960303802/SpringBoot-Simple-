package com.xizi.miaosha.service.impl;

import com.xizi.miaosha.mapper.OrderMapper;
import com.xizi.miaosha.mapper.StockMapper;
import com.xizi.miaosha.mapper.UserMapper;
import com.xizi.miaosha.pojo.Order;
import com.xizi.miaosha.pojo.Stock;
import com.xizi.miaosha.pojo.User;
import com.xizi.miaosha.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;


/**
 * 开发高并发系统有三把利器：缓存 降级 限流
 * 缓存：缓存的目的是提高系统访问的速度和增大系统处理容量
 * 降级：降级是当前服务器压力剧增的情况下。根据当前业务及5浏览对一些服务页面有策略的降级  保证核心任务运行
 * 限流：限流的目的是通过并发访问/请求进行限速，或者对一个时间窗口的请求进行限速来保护系统
 * 一旦达到限制速率则可以拒绝服务，排队或者等待，降级等处理
 */

/**
 * 漏斗算法： 请求先进入到漏斗桶里，漏桶以一定的速度出水，当水流入速度过大会直接溢出
 * 令牌桶算法： 来源于计算机网络，为了防止网络拥塞，需限制出网络的流量，使流量以均匀的速度向外发送
 */

/**
 * 1.限时抢购       (redis )set kill1 1 EX 20
 * 2.抢购接口隐藏
 * 3.单用户限制频率(单位时间内限制访问次数)
 */

//1.限时抢购       (redis )set kill1 1 EX 20

//2.抢购接口隐藏
/**
 *  抢购接口隐藏(接口加盐)
 *  每次点击秒杀按钮，先从服务器获取一个秒杀验证值（接口内判断是否到秒杀时间）
 *  redis以缓存用户ID和商品ID为key ,秒杀地址为value缓存验证值
 *  用户请求秒杀商品的时候，要带上秒杀验证值进行校验
 */


/*
//@Transactional 事务注解带有同步得功能 当前事务同步能力大于 synchronized方法
 */

/**
 * 使用乐观锁解决商品超卖得问题，实际上主要是防止超卖得问题交给数据库解决，利用
 * 数据库中定义得verison字段以及数据库中得事务实现并发情况下商品得超卖问题
 */
@Service
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService {
    
    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;



    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    // //生成md5签名得方法
    @Override
    public String getMd5(Integer id, Integer userId) {
        //验证userId 用户是否存在
        User user = userMapper.findById(userId);
        if(user==null){
            throw new RuntimeException("用户信息不存在");
        }
        log.info("用户的信息:[{}]",user.toString());
        //验证商品id  存放商品信息
        Stock stock = stockMapper.checkStock(id);
        if(stock==null){
            throw new RuntimeException("商品信息不存在");
        }
        log.info("商品的信息:[{}]",stock.toString());
        //生成md5签名 放入redis 服务
        String hashKey="KEY_"+userId+id;
        //随机盐
        String key = DigestUtils.md5DigestAsHex((userId + id + "!XIZIzz").getBytes());
        stringRedisTemplate.opsForValue().set(hashKey, key,240, TimeUnit.SECONDS);
        log.info("Redis写入：[{}] [{}]",hashKey,key);
        return key;
    }

    @Override
    public  Integer  kill(Integer id) {
//        校验redis中秒杀商品是否超时
        if(!stringRedisTemplate.hasKey("kill"+id)){
            throw new RuntimeException("当前商品的抢购活动已经结束了----");
        }

        //根据商品id效验ku库存
        Stock stock = checkStock(id);
        //扣除库存
        updateSale(stock);
            //创建订单
        Integer orderId = createOrder(stock);
        return orderId;
    }

    //用来处理秒杀的下单方法 并返回订单id 加入 md5接口
    @Override
    public Integer kill(Integer id, Integer userId, String md5) {
//        校验redis中秒杀商品是否超时
        if(!stringRedisTemplate.hasKey("kill"+id)){
            throw new RuntimeException("当前商品的抢购活动已经结束了----");
        }

        //先验证签名
        String hashKey="KEY_"+userId+id;
        String s = stringRedisTemplate.opsForValue().get(hashKey);
        if(s==null){
            throw new RuntimeException("没有携带验证签名，请求不合法");
        }
        if(!s.equals(md5)){
            throw new RuntimeException("当前请求数据不合法，请稍后再试！");
        }
        //根据商品id效验ku库存
        Stock stock = checkStock(id);
        //扣除库存
        updateSale(stock);
        //创建订单
        Integer orderId = createOrder(stock);
        return orderId;

    }

    //效验库存
    private Stock checkStock(Integer id){
        Stock stock = stockMapper.checkStock(id);
        if(stock.getSale().equals(stock.getCount())){
            throw new RuntimeException("库存不足!!!");
        }
        return stock;
    }
    //扣除库存
    private void updateSale(Stock stock){
        //在sql层面完成销量的+1 和版本号的+1  并且根据商品id和版本号同时查询更新的商品
//        stock.setSale(stock.getSale()+1);
        int updateRows = stockMapper.updateStock(stock);
        if(updateRows==0){
            throw new RuntimeException("抢购失败，请重试!!!");
        }
    }

    //创建订单
    private Integer createOrder(Stock stock){
        Order order = new Order();
        order.setSid(stock.getId()).setName(stock.getName()).setCreateTime(new Date());
        orderMapper.createOrder(order);
        return order.getId();
    }
}
