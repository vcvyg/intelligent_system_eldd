package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.entity.HealthData;
import org.example.persion.entity.User;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.repository.HealthDataMapper;
import org.example.persion.repository.UserMapper;
import org.example.persion.service.AdminStatisticsService;
import org.example.persion.vo.SystemStatisticsVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 管理员端-系统统计服务实现类
 */
@Service
@RequiredArgsConstructor
public class AdminStatisticsServiceImpl implements AdminStatisticsService {

    private final UserMapper userMapper;
    private final ElderlyInfoMapper elderlyInfoMapper;
    private final HealthDataMapper healthDataMapper;

    @Override
    public SystemStatisticsVO getSystemStatistics() {
        SystemStatisticsVO vo = new SystemStatisticsVO();

        // 总用户数
        vo.setTotalUsers(userMapper.selectCount(null));

        // 总老人数
        vo.setTotalElderly(elderlyInfoMapper.selectCount(null));

        // 活跃用户数（状态为1）
        LambdaQueryWrapper<User> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.eq(User::getStatus, 1);
        vo.setActiveUsers(userMapper.selectCount(activeWrapper));

        // 禁用用户数（状态为0）
        LambdaQueryWrapper<User> disabledWrapper = new LambdaQueryWrapper<>();
        disabledWrapper.eq(User::getStatus, 0);
        vo.setDisabledUsers(userMapper.selectCount(disabledWrapper));

        // 管理员数量
        LambdaQueryWrapper<User> adminWrapper = new LambdaQueryWrapper<>();
        adminWrapper.eq(User::getRole, "ADMIN");
        vo.setAdminCount(userMapper.selectCount(adminWrapper));

        // 子女用户数量
        LambdaQueryWrapper<User> familyWrapper = new LambdaQueryWrapper<>();
        familyWrapper.eq(User::getRole, "FAMILY");
        vo.setFamilyCount(userMapper.selectCount(familyWrapper));

        // 医护人员数量
        LambdaQueryWrapper<User> medicalWrapper = new LambdaQueryWrapper<>();
        medicalWrapper.eq(User::getRole, "MEDICAL");
        vo.setMedicalCount(userMapper.selectCount(medicalWrapper));

        // 老人用户数量
        LambdaQueryWrapper<User> elderlyWrapper = new LambdaQueryWrapper<>();
        elderlyWrapper.eq(User::getRole, "ELDERLY");
        vo.setElderlyCount(userMapper.selectCount(elderlyWrapper));

        // 今日健康数据记录数
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        LambdaQueryWrapper<HealthData> healthWrapper = new LambdaQueryWrapper<>();
        healthWrapper.between(HealthData::getCreateTime, startOfDay, endOfDay);
        vo.setTodayHealthRecords(healthDataMapper.selectCount(healthWrapper));

        return vo;
    }
}
