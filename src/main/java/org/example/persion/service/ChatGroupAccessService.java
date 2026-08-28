package org.example.persion.service;

import lombok.RequiredArgsConstructor;
import org.example.persion.repository.ElderlyInfoMapper;
import org.springframework.stereotype.Service;

/**
 * Unified authorization check for elderly-centered chat groups.
 * A chat group id is the elderly id in the current data model.
 */
@Service
@RequiredArgsConstructor
public class ChatGroupAccessService {

    private final ElderlyInfoMapper elderlyInfoMapper;

    public boolean canAccess(Long userId, Long groupId) {
        if (userId == null || groupId == null) {
            return false;
        }

        boolean familyAccess = elderlyInfoMapper.selectElderlyListByFamilyUserId(userId).stream()
                .anyMatch(elderly -> groupId.equals(elderly.getId()));
        if (familyAccess) {
            return true;
        }

        return elderlyInfoMapper.selectElderlyListByMedicalUserId(userId).stream()
                .anyMatch(elderly -> groupId.equals(elderly.getId()));
    }
}
