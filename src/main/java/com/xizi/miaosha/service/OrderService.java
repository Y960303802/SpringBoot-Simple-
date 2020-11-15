package com.xizi.miaosha.service;


/**
 * 订单业务
 */
public interface OrderService {

    /*
    处理秒杀的下单方法
     */
     Integer kill(Integer id);

    //生成md5签名得方法
    String getMd5(Integer id, Integer userId);

    //用来处理秒杀的下单方法 并返回订单id 加入 md5接口
    Integer kill(Integer id, Integer userId, String md5);
}
