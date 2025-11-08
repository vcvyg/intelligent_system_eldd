package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.exception.BusinessException;
import org.example.persion.dto.AdminElderlyCreateDTO;
import org.example.persion.dto.AdminElderlyUpdateDTO;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.entity.User;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.repository.UserMapper;
import org.example.persion.service.AdminElderlyService;
import org.example.persion.vo.ElderlyInfoVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 管理员端-老人信息管理服务实现类
 */
@Service
@RequiredArgsConstructor
public class AdminElderlyServiceImpl implements AdminElderlyService {

    private final ElderlyInfoMapper elderlyInfoMapper;
    private final UserMapper userMapper;

    @Override
    public Page<ElderlyInfoVO> getElderlyList(Integer current, Integer size, String keyword) {
        Page<ElderlyInfo> page = new Page<>(current, size);

        LambdaQueryWrapper<ElderlyInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(ElderlyInfo::getName, keyword)
                   .or()
                   .like(ElderlyInfo::getIdCard, keyword)
                   .or()
                   .like(ElderlyInfo::getEmergencyContact, keyword);
        }
        wrapper.orderByDesc(ElderlyInfo::getCreateTime);

        Page<ElderlyInfo> elderlyPage = elderlyInfoMapper.selectPage(page, wrapper);

        // 转换为VO
        Page<ElderlyInfoVO> voPage = new Page<>(elderlyPage.getCurrent(), elderlyPage.getSize(), elderlyPage.getTotal());
        voPage.setRecords(elderlyPage.getRecords().stream().map(elderly -> {
            ElderlyInfoVO vo = new ElderlyInfoVO();
            BeanUtils.copyProperties(elderly, vo);
            return vo;
        }).toList());

        return voPage;
    }

    @Override
    public ElderlyInfo createElderly(AdminElderlyCreateDTO dto) {
        // 检查关联用户是否存在
        User user = userMapper.selectById(dto.getUserId());
        if (user == null) {
            throw new BusinessException("关联用户不存在");
        }

        // 检查该用户是否已有老人信息
        LambdaQueryWrapper<ElderlyInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ElderlyInfo::getUserId, dto.getUserId());
        if (elderlyInfoMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("该用户已有老人信息");
        }

        // 检查身份证号是否已存在
        if (StringUtils.hasText(dto.getIdCard())) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ElderlyInfo::getIdCard, dto.getIdCard());
            if (elderlyInfoMapper.selectCount(wrapper) > 0) {
                throw new BusinessException("身份证号已存在");
            }
        }

        ElderlyInfo elderlyInfo = new ElderlyInfo();
        BeanUtils.copyProperties(dto, elderlyInfo);

        elderlyInfoMapper.insert(elderlyInfo);
        return elderlyInfo;
    }

    @Override
    public ElderlyInfo updateElderly(Long id, AdminElderlyUpdateDTO dto) {
        ElderlyInfo elderlyInfo = elderlyInfoMapper.selectById(id);
        if (elderlyInfo == null) {
            throw new BusinessException("老人信息不存在");
        }

        // 检查身份证号是否被其他老人使用
        if (StringUtils.hasText(dto.getIdCard()) && !dto.getIdCard().equals(elderlyInfo.getIdCard())) {
            LambdaQueryWrapper<ElderlyInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ElderlyInfo::getIdCard, dto.getIdCard())
                   .ne(ElderlyInfo::getId, id);
            if (elderlyInfoMapper.selectCount(wrapper) > 0) {
                throw new BusinessException("身份证号已被其他老人使用");
            }
        }

        BeanUtils.copyProperties(dto, elderlyInfo, "id", "userId");
        elderlyInfoMapper.updateById(elderlyInfo);
        return elderlyInfo;
    }

    @Override
    public void deleteElderly(Long id) {
        ElderlyInfo elderlyInfo = elderlyInfoMapper.selectById(id);
        if (elderlyInfo == null) {
            throw new BusinessException("老人信息不存在");
        }

        elderlyInfoMapper.deleteById(id);
    }

    @Override
    public ElderlyInfoVO getElderlyDetail(Long id) {
        ElderlyInfo elderlyInfo = elderlyInfoMapper.selectById(id);
        if (elderlyInfo == null) {
            throw new BusinessException("老人信息不存在");
        }

        ElderlyInfoVO vo = new ElderlyInfoVO();
        BeanUtils.copyProperties(elderlyInfo, vo);
        return vo;
    }
}
