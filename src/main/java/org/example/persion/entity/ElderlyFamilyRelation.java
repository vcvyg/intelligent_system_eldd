package org.example.persion.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 老人-子女关系实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("elderly_family_relation")
public class ElderlyFamilyRelation extends BaseEntity {

    /**
     * 老人ID
     */
    private Long elderlyId;

    /**
     * 子女用户ID
     */
    private Long familyUserId;

    /**
     * 关系类型: 子女/儿媳/女婿/孙子/孙女等
     */
    private String relationType;

    /**
     * 是否主要联系人 0-否 1-是
     */
    private Integer isPrimaryContact;
}
