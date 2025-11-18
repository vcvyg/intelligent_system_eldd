package org.example.persion.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.vo.ElderlyInfoVO;

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
}
