package com.gleanread.server.application.dto;

import java.util.List;
import lombok.Data;

@Data
public class MountRequest {
    private List<Long> excerptIds;
    // 明确的挂载目标节点。为空代表只解除或者挂在Inbox/根下
    private Long targetNodeId;
    
    // 如果没有 targetNodeId，但提供了 parentNodeId, title，则视为新建
    private Long parentNodeId;
    private String title;
    
    // 生成的大纲，如果是 targetNodeId 则更新该节点，如果是新建则放到新建节点中
    private String outlineMarkdown;
}
