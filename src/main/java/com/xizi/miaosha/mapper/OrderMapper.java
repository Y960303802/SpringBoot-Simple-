package com.xizi.miaosha.mapper;

import com.xizi.miaosha.pojo.Order;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderMapper {
    /*
        创建订单
     */
    int createOrder(Order order);
}
