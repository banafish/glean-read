package com.gleanread.server.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 知识树节点实体类
 * 存放AI提炼出来的总结大纲，或用户自己建立的子分类
 */
@Data
@TableName("knowledge_tree_node")
public class KnowledgeTreeNode {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    // 父节点ID，支持无限极树分类
    private Long parentId;
    
    // 节点/专题名称
    private String nodeTitle;
    
    // AI生成的提炼大纲文本(Markdown格式)
    private String outlineMarkdown;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
