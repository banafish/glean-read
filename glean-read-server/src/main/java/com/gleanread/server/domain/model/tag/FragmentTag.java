package com.gleanread.server.domain.model.tag;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Fragment 和 Tag 的多对多关系实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("fragment_tag")
public class FragmentTag {

    private Long fragmentId;
    
    // Tag 表的主键，如果是标签的话，记录下打的什么标签
    private Long tagId;
}
