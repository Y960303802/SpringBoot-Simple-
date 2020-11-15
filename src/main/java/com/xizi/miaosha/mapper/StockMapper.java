package com.xizi.miaosha.mapper;

import com.xizi.miaosha.pojo.Stock;
import org.springframework.stereotype.Repository;

@Repository
public interface StockMapper {

    //根据商品id查询库存信息的方法
    Stock checkStock(Integer id);

    /*
    根据商品扣除库存
 */
    int updateStock(Stock stock);
}
