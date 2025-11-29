package org.example.persion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.exception.BusinessException;
import org.example.persion.dto.AdminElderlyCreateDTO;
import org.example.persion.dto.AdminElderlyUpdateDTO;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.entity.Room;

import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.repository.RoomMapper;
import org.example.persion.repository.UserMapper;
import org.example.persion.service.AdminElderlyService;
import org.example.persion.vo.ElderlyInfoVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 管理员端-老人信息管理服务实现类
 */
@Service
@RequiredArgsConstructor
public class AdminElderlyServiceImpl implements AdminElderlyService {

    private final ElderlyInfoMapper elderlyInfoMapper;
    private final UserMapper userMapper;
    private final RoomMapper roomMapper;

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

        // 批量查询房间信息，避免N+1查询问题
        java.util.Set<Long> roomIds = elderlyPage.getRecords().stream()
                .map(ElderlyInfo::getRoomId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        
        final java.util.Map<Long, Room> roomMap;
        if (!roomIds.isEmpty()) {
            List<Room> rooms = roomMapper.selectBatchIds(roomIds);
            roomMap = rooms.stream().collect(java.util.stream.Collectors.toMap(Room::getId, room -> room));
        } else {
            roomMap = new java.util.HashMap<>();
        }

        // 转换为VO
        Page<ElderlyInfoVO> voPage = new Page<>(elderlyPage.getCurrent(), elderlyPage.getSize(), elderlyPage.getTotal());
        voPage.setRecords(elderlyPage.getRecords().stream().map(elderly -> {
            ElderlyInfoVO vo = new ElderlyInfoVO();
            BeanUtils.copyProperties(elderly, vo);
            // 使用批量查询的房间信息
            if (elderly.getRoomId() != null && elderly.getRoomId() > 0) {
                Room room = roomMap.get(elderly.getRoomId());
                if (room != null && room.getRoomNumber() != null && !room.getRoomNumber().isEmpty()) {
                    vo.setRoomNumber(room.getRoomNumber());
                } else {
                    vo.setRoomNumber("-");
                }
            } else {
                vo.setRoomNumber("-");
            }
            return vo;
        }).toList());

        return voPage;
    }

    @Override
    @Transactional
    public ElderlyInfo createElderly(AdminElderlyCreateDTO dto) {
        // 检查身份证号是否已存在
        if (StringUtils.hasText(dto.getIdCard())) {
            LambdaQueryWrapper<ElderlyInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ElderlyInfo::getIdCard, dto.getIdCard());
            if (elderlyInfoMapper.selectCount(wrapper) > 0) {
                throw new BusinessException("身份证号已存在");
            }
        }

        // 如果分配了房间，检查房间容量
        if (dto.getRoomId() != null) {
            Room room = roomMapper.selectById(dto.getRoomId());
            if (room == null) {
                throw new BusinessException("指定的房间不存在");
            }
            if (room.getOccupiedBeds() >= room.getBedCount()) {
                throw new BusinessException("房间 " + room.getRoomNumber() + " 已满，请选择其他房间");
            }
            // 增加房间入住人数
            room.setOccupiedBeds(room.getOccupiedBeds() + 1);
            roomMapper.updateById(room);
        }

        ElderlyInfo elderlyInfo = new ElderlyInfo();
        BeanUtils.copyProperties(dto, elderlyInfo);

        // 设置默认健康阈值
        elderlyInfo.setHeartRateHigh(100);
        elderlyInfo.setHeartRateLow(60);
        elderlyInfo.setSystolicPressureHigh(140);
        elderlyInfo.setSystolicPressureLow(90);
        elderlyInfo.setDiastolicPressureHigh(80);
        elderlyInfo.setDiastolicPressureLow(60);
        elderlyInfo.setTemperatureHigh(37.3);
        elderlyInfo.setTemperatureLow(36.0);

        elderlyInfoMapper.insert(elderlyInfo);
        return elderlyInfo;
    }

    @Override
    @Transactional
    public ElderlyInfo updateElderly(Long id, AdminElderlyUpdateDTO dto) {
        ElderlyInfo elderlyInfo = elderlyInfoMapper.selectById(id);
        if (elderlyInfo == null) {
            throw new BusinessException("老人信息不存在");
        }

        Long oldRoomId = elderlyInfo.getRoomId();
        Long newRoomId = dto.getRoomId();

        // 检查房间是否发生变化
        if (!Objects.equals(oldRoomId, newRoomId)) {
            // Case 1: 从一个房间搬出（到未分配或其他房间）
            if (oldRoomId != null) {
                Room oldRoom = roomMapper.selectById(oldRoomId);
                if (oldRoom != null) {
                    oldRoom.setOccupiedBeds(Math.max(0, oldRoom.getOccupiedBeds() - 1));
                    roomMapper.updateById(oldRoom);
                }
            }
            // Case 2: 搬入一个新房间
            if (newRoomId != null) {
                Room newRoom = roomMapper.selectById(newRoomId);
                if (newRoom == null) {
                    throw new BusinessException("指定的新房间不存在");
                }
                if (newRoom.getOccupiedBeds() >= newRoom.getBedCount()) {
                    // 注意：由于这是一个事务，如果这里抛出异常，上面对旧房间的修改会回滚
                    throw new BusinessException("新房间 " + newRoom.getRoomNumber() + " 已满，无法入住");
                }
                newRoom.setOccupiedBeds(newRoom.getOccupiedBeds() + 1);
                roomMapper.updateById(newRoom);
            }
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

        // 使用 BeanUtils 复制属性，但排除 id、createTime、updateTime
        BeanUtils.copyProperties(dto, elderlyInfo, "id", "createTime", "updateTime");

        // 特殊处理 roomId，允许设置为 null（未分配房间）
        elderlyInfo.setRoomId(dto.getRoomId());

        // MyBatis Plus 会自动更新 update_time
        elderlyInfoMapper.updateById(elderlyInfo);
        return elderlyInfo;
    }

    @Override
    @Transactional
    public void deleteElderly(Long id) {
        ElderlyInfo elderlyInfo = elderlyInfoMapper.selectById(id);
        if (elderlyInfo == null) {
            throw new BusinessException("老人信息不存在");
        }

        // 如果老人在房间里，删除时需要更新房间入住人数
        if (elderlyInfo.getRoomId() != null) {
            Room room = roomMapper.selectById(elderlyInfo.getRoomId());
            if (room != null) {
                room.setOccupiedBeds(Math.max(0, room.getOccupiedBeds() - 1));
                roomMapper.updateById(room);
            }
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
        
        // 设置房间信息
        if (elderlyInfo.getRoomId() != null && elderlyInfo.getRoomId() > 0) {
            Room room = roomMapper.selectById(elderlyInfo.getRoomId());
            if (room != null) {
                vo.setRoomNumber(room.getRoomNumber());
                vo.setRoomType(room.getRoomType());
            } else {
                vo.setRoomNumber("-");
                vo.setRoomType("-");
            }
        } else {
            vo.setRoomNumber("-");
            vo.setRoomType("-");
        }
        
        return vo;
    }

    @Override
    public java.util.List<ElderlyInfoVO> getAllElderly() {
        LambdaQueryWrapper<ElderlyInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ElderlyInfo::getCreateTime);

        return elderlyInfoMapper.selectList(wrapper).stream().map(elderly -> {
            ElderlyInfoVO vo = new ElderlyInfoVO();
            BeanUtils.copyProperties(elderly, vo);
            return vo;
        }).toList();
    }
}
