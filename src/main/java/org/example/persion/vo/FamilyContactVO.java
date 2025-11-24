package org.example.persion.vo;

import lombok.Data;

/**
 * 老人关联家属联系人信息
 */
@Data
public class FamilyContactVO {
    private Long userId;
    private String username;
    private String realName;
    private String phone;
    private String relationType;
    private Boolean primaryContact;
}

