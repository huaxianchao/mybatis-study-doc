package com.hy.mapper;

import com.hy.VO.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * @Description:
 * @Author: xianchao.hua
 * @Create: 2021-01-04 11:47
 */
public interface UserMapper {

    User getById(int id);

    User getByCondition(User user);

    @Update("update user set name = #{name},age = #{age} where id = #{id}")
    void update(User user);
}
