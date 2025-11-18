package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.exception.BusinessException;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.entity.HealthData;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.repository.HealthDataMapper;
import org.example.persion.service.MedicalPatientService;
import org.example.persion.vo.ElderlyInfoVO;
import org.example.persion.vo.HealthDataVO;
import org.example.persion.vo.PatientHealthDetailsVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicalPatientServiceImpl implements MedicalPatientService {

    private final ElderlyInfoMapper elderlyInfoMapper;
    private final HealthDataMapper healthDataMapper;

    /**
     * 获取当前医护人员负责的老人列表
     * 注意：当前实现为获取所有老人列表，后续可根据业务扩展为只获取分配给该医护人员的老人
     * @param medicalStaffId 当前登录的医护人员ID (当前未使用)
     * @return 老人信息VO列表
     */
    @Override
    public List<ElderlyInfoVO> getMyPatients(Long medicalStaffId) {
        // TODO: 实现医护人员与老人的关联关系后，此处应修改为查询分配给该医护的老人
        LambdaQueryWrapper<ElderlyInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ElderlyInfo::getCreateTime);

        List<ElderlyInfo> elderlyList = elderlyInfoMapper.selectList(wrapper);

        return elderlyList.stream().map(elderly -> {
            ElderlyInfoVO vo = new ElderlyInfoVO();
            BeanUtils.copyProperties(elderly, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public PatientHealthDetailsVO getPatientHealthDetails(Long elderlyId) {
        // 1. 获取老人基本信息
        ElderlyInfo elderlyInfo = elderlyInfoMapper.selectById(elderlyId);
        if (elderlyInfo == null) {
            throw new BusinessException("未找到指定的老人信息");
        }
        ElderlyInfoVO elderlyInfoVO = new ElderlyInfoVO();
        BeanUtils.copyProperties(elderlyInfo, elderlyInfoVO);

        // 2. 获取该老人的健康数据列表 (按测量时间倒序)
        LambdaQueryWrapper<HealthData> healthDataWrapper = new LambdaQueryWrapper<>();
        healthDataWrapper.eq(HealthData::getElderlyId, elderlyId);
        healthDataWrapper.orderByDesc(HealthData::getMeasureTime);
        // 可以加上分页或者limit来限制返回的记录数，这里为了简单先获取全部
        List<HealthData> healthDataList = healthDataMapper.selectList(healthDataWrapper);

        List<HealthDataVO> healthDataVOList = healthDataList.stream().map(healthData -> {
            HealthDataVO vo = new HealthDataVO();
            BeanUtils.copyProperties(healthData, vo);
            vo.setElderlyName(elderlyInfo.getName()); // 设置老人姓名
            return vo;
        }).collect(Collectors.toList());

        // 3. 组装并返回
        return new PatientHealthDetailsVO(elderlyInfoVO, healthDataVOList);
    }
}
