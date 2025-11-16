package org.example.persion.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.persion.dto.AdminElderlyCreateDTO;
import org.example.persion.dto.AdminElderlyUpdateDTO;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.vo.ElderlyInfoVO;

/**
 * 管理员端-老人信息管理服务接口
 */
public interface AdminElderlyService {

    /**
     * 分页查询老人信息列表
     */
    Page<ElderlyInfoVO> getElderlyList(Integer current, Integer size, String keyword);

    /**
     * 创建老人信息
     */
    ElderlyInfo createElderly(AdminElderlyCreateDTO dto);

    /**
     * 更新老人信息
     */
    ElderlyInfo updateElderly(Long id, AdminElderlyUpdateDTO dto);

    /**
     * 删除老人信息
     */
    void deleteElderly(Long id);

    /**
     * 查询老人详细信息
     */
    ElderlyInfoVO getElderlyDetail(Long id);

    /**
     * 获取所有老人信息列表(不分页)
     */
    java.util.List<ElderlyInfoVO> getAllElderly();
}