package com.xizi.miaosha.mapper;

import com.xizi.miaosha.pojo.User;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMapper {
    User findById(Integer id);
}
