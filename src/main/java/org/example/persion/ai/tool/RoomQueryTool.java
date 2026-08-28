package org.example.persion.ai.tool;

import lombok.RequiredArgsConstructor;
import org.example.persion.repository.ElderlyInfoMapper;
import org.example.persion.vo.ElderlyInfoVO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RoomQueryTool implements MedicalAiTool {

    private final ElderlyInfoMapper elderlyInfoMapper;

    @Override
    public String name() {
        return "room_lookup";
    }

    @Override
    public boolean supports(String question) {
        if (question == null) return false;
        String q = question.toLowerCase();
        return containsAny(q, "房间", "房号", "几号房", "住哪", "住在哪里", "room");
    }

    @Override
    public MedicalAiToolResult execute(MedicalAiToolContext context) {
        ElderlyInfoVO detail = elderlyInfoMapper.selectElderlyWithRoom(context.elderlyId());
        String room = detail == null || detail.getRoomNumber() == null || detail.getRoomNumber().isBlank()
                ? "系统暂未登记房间"
                : detail.getRoomNumber() + (detail.getRoomType() == null ? "" : "（" + detail.getRoomType() + "）");

        return new MedicalAiToolResult(
                "房间信息",
                context.elderlyName() + "目前为：" + room + "。",
                "ok",
                room,
                List.of("老人档案 / 房间信息")
        );
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }
}
