package org.example.persion.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.persion.entity.ElderlyFamilyRelation;

import java.util.List;
import java.util.Map;

/**
 * 老人-子女关系Mapper
 */
@Mapper
public interface ElderlyFamilyRelationMapper extends BaseMapper<ElderlyFamilyRelation> {

    /**
     * 查询老人的子女关联列表(包含子女用户信息)
     */
    @Select("SELECT efr.*, u.username, u.real_name, u.phone, u.email " +
            "FROM elderly_family_relation efr " +
            "LEFT JOIN sys_user u ON efr.family_user_id = u.id " +
            "WHERE efr.elderly_id = #{elderlyId} AND efr.deleted = 0")
    List<Map<String, Object>> selectFamilyListWithUserInfo(Long elderlyId);

    /**
     * 反向查询: 查询某个子女用户关联的老人列表(包含老人信息)
     */
    @Select("SELECT efr.*, e.name, e.age, e.gender, e.birthday, e.id_card, e.address, " +
            "e.emergency_contact, e.emergency_phone, e.medical_history, e.allergy_history " +
            "FROM elderly_family_relation efr " +
            "LEFT JOIN elderly_info e ON efr.elderly_id = e.id " +
            "WHERE efr.family_user_id = #{familyUserId} AND efr.deleted = 0")
    List<Map<String, Object>> selectElderlyListByFamilyUser(Long familyUserId);
}
