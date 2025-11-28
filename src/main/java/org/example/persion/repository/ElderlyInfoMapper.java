package org.example.persion.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.vo.ElderlyInfoVO;

import java.util.List;

/**
 * 老人信息数据访问接口
 */
@Mapper
public interface ElderlyInfoMapper extends BaseMapper<ElderlyInfo> {

    /**
     * 查询老人信息并关联房间信息
     * 
     * @param elderlyId 老人 ID
     * @return 老人信息 VO（包含房间号和房间类型）
     */
    ElderlyInfoVO selectElderlyWithRoom(@Param("elderlyId") Long elderlyId);

    /**
     * 根据医护人员ID查询其负责的老人列表
     *
     * @param medicalUserId 医护人员用户ID
     * @return 老人信息列表
     */
    List<ElderlyInfo> selectElderlyListByMedicalUserId(@Param("medicalUserId") Long medicalUserId);

    /**
     * 根据家庭成员ID查询其关联的老人列表
     *
     * @param familyUserId 家庭成员用户ID
     * @return 老人信息列表
     */
    List<ElderlyInfo> selectElderlyListByFamilyUserId(@Param("familyUserId") Long familyUserId);

    /**
     * 查询所有老人信息并关联房间信息
     * 
     * @return 老人信息 VO 列表（包含房间号和房间类型）
     */
    List<ElderlyInfoVO> selectAllElderlyWithRoom();
}
