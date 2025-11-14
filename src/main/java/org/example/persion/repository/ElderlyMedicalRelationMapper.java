package org.example.persion.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.persion.entity.ElderlyMedicalRelation;

import java.util.List;
import java.util.Map;

/**
 * 老人-医护人员关系Mapper
 */
@Mapper
public interface ElderlyMedicalRelationMapper extends BaseMapper<ElderlyMedicalRelation> {

    /**
     * 查询老人的医护分配列表(包含医护人员信息)
     */
    @Select("SELECT emr.*, u.username, u.real_name, u.phone " +
            "FROM elderly_medical_relation emr " +
            "LEFT JOIN sys_user u ON emr.medical_user_id = u.id " +
            "WHERE emr.elderly_id = #{elderlyId} AND emr.deleted = 0")
    List<Map<String, Object>> selectMedicalListWithUserInfo(Long elderlyId);
}
