package com.gleanread.server.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 知识碎片实体类
 * 用于存储从移动端采集的最原始的内容
 */
@Data
@TableName("fragment")
public class Fragment {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    // 摘录的高亮文本/正文
    private String content;
    
    // 来源链接
    private String url;
    
    // 用户的随手想法
    private String userThought;
    
    // 归属的体系树节点ID，未处理的碎片该值为null(即存在于Inbox中)
    private Long treeNodeId;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
