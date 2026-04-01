package com.gleanread.server.domain.model.tag;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Excerpt 和 Tag 的多对多关系实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("excerpt_tags")
public class ExcerptTag {

    private Long excerptId;
    
    // Tag 表的主键，如果是标签的话，记录下打的什么标签
    private Long tagId;
}
