package org.example.persion.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.persion.common.Result;
import org.example.persion.dto.AdminElderlyCreateDTO;
import org.example.persion.dto.AdminElderlyUpdateDTO;
import org.example.persion.entity.ElderlyInfo;
import org.example.persion.service.AdminElderlyService;
import org.example.persion.vo.ElderlyInfoVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端 - 老人信息管理控制器
 */
@RestController
@RequestMapping("/api/admin/elderly")
@RequiredArgsConstructor
public class AdminElderlyController {

    private final AdminElderlyService adminElderlyService;

    /**
     * 分页查询老人信息列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Page<ElderlyInfoVO>> getElderlyList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        Page<ElderlyInfoVO> page = adminElderlyService.getElderlyList(current, size, keyword);
        return Result.success(page);
    }

    /**
     * 查询老人详细信息
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ElderlyInfoVO> getElderlyDetail(@PathVariable Long id) {
        ElderlyInfoVO vo = adminElderlyService.getElderlyDetail(id);
        return Result.success(vo);
    }

    /**
     * 创建老人信息
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ElderlyInfo> createElderly(@Valid @RequestBody AdminElderlyCreateDTO dto) {
        ElderlyInfo elderlyInfo = adminElderlyService.createElderly(dto);
        return Result.success(elderlyInfo);
    }

    /**
     * 更新老人信息
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ElderlyInfo> updateElderly(@PathVariable Long id, @Valid @RequestBody AdminElderlyUpdateDTO dto) {
        ElderlyInfo elderlyInfo = adminElderlyService.updateElderly(id, dto);
        return Result.success(elderlyInfo);
    }

    /**
     * 删除老人信息
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteElderly(@PathVariable Long id) {
        adminElderlyService.deleteElderly(id);
        return Result.success();
    }

    /**
     * 获取所有老人信息列表(不分页)
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICAL')")
    public Result<java.util.List<ElderlyInfoVO>> getAllElderly() {
        java.util.List<ElderlyInfoVO> list = adminElderlyService.getAllElderly();
        return Result.success(list);
    }
}
