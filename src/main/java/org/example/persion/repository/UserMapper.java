package org.example.persion.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.persion.entity.User;

import java.util.List;

/**
 * 用户数据访问接口
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 查询所有FAMILY角色的用户
     */
    @Select("SELECT * FROM sys_user WHERE role = 'FAMILY' AND deleted = 0")
    List<User> selectFamilyUsers();
}
