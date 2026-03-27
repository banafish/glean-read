package com.gleanread.server.domain.dto;

import java.util.List;
import lombok.Data;

@Data
public class SynthesisRequest {
    // 专题名称，用于创建节点名，比如 "Android 内存优化"
    private String topicName;
    
    // 可能隶属的父知识树节点 ID（可选）
    private Long parentNodeId;
    
    // 圈定的要发送给大模型处理的原始纯碎片 ID 集合
    private List<Long> fragmentIds;
}
