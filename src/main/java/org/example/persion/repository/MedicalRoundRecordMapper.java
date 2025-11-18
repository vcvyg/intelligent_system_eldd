package org.example.persion.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.persion.entity.HealthData;
import org.example.persion.vo.MedicalRoundRecordVO;
import java.util.List;

@Mapper
public interface MedicalRoundRecordMapper extends BaseMapper<HealthData> {
    List<MedicalRoundRecordVO> findByMedicalUser();
    List<MedicalRoundRecordVO> findByElderly(Long elderlyId);
}

