package com.gleanread.server.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 碎片-标签多对多关联中间表实体
 * 对应 fragment_tag 表，联合主键通过复合对象表示
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("fragment_tag")
public class FragmentTag {

    // 片段 ID（联合主键之一）
    @TableId(type = IdType.INPUT)
    private Long fragmentId;

    // 标签 ID（联合主键之一）
    private Long tagId;
}
