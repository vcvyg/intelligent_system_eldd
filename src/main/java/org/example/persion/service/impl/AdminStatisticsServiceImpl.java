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
import java.util.List;

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

        // 优化: 一次性查询所有用户,在内存中统计,避免多次数据库查询
        List<User> allUsers = userMapper.selectList(null);

        // 统计各类数据
        long totalUsers = allUsers.size();
        long activeUsers = allUsers.stream().filter(u -> u.getStatus() == 1).count();
        long disabledUsers = allUsers.stream().filter(u -> u.getStatus() == 0).count();
        long adminCount = allUsers.stream().filter(u -> "ADMIN".equals(u.getRole())).count();
        long familyCount = allUsers.stream().filter(u -> "FAMILY".equals(u.getRole())).count();
        long medicalCount = allUsers.stream().filter(u -> "MEDICAL".equals(u.getRole())).count();
        long elderlyCount = allUsers.stream().filter(u -> "ELDERLY".equals(u.getRole())).count();

        vo.setTotalUsers(totalUsers);
        vo.setActiveUsers(activeUsers);
        vo.setDisabledUsers(disabledUsers);
        vo.setAdminCount(adminCount);
        vo.setFamilyCount(familyCount);
        vo.setMedicalCount(medicalCount);
        vo.setElderlyCount(elderlyCount);

        // 总老人数
        vo.setTotalElderly(elderlyInfoMapper.selectCount(null));

        // 今日健康数据记录数
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        LambdaQueryWrapper<HealthData> healthWrapper = new LambdaQueryWrapper<>();
        healthWrapper.between(HealthData::getCreateTime, startOfDay, endOfDay);
        vo.setTodayHealthRecords(healthDataMapper.selectCount(healthWrapper));

        return vo;
    }
}
