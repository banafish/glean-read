package com.gleanread.server.domain.model.tree;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识树节点实体类 (充血)
 */
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@TableName("knowledge_tree_node")
public class KnowledgeTreeNode {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    // 父节点ID
    private Long parentId;
    
    // 节点/专题名称
    private String nodeTitle;
    
    // AI生成的提炼大纲文本(Markdown格式)
    private String outlineMarkdown;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;

    /**
     * 工厂方法：创建一个新的树状合成节点
     */
    public static KnowledgeTreeNode create(Long parentNodeId, String topicName, String outlineMarkdown) {
        KnowledgeTreeNode node = new KnowledgeTreeNode();
        node.parentId = parentNodeId;
        node.nodeTitle = topicName;
        node.outlineMarkdown = outlineMarkdown;
        node.createTime = LocalDateTime.now();
        node.updateTime = LocalDateTime.now();
        return node;
    }

    public void updateOutline(String outlineMarkdown) {
        this.outlineMarkdown = outlineMarkdown;
        this.updateTime = LocalDateTime.now();
    }
}
