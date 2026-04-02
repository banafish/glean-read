package com.gleanread.server.application.dto;

import java.util.List;
import lombok.Data;

@Data
public class OutlineBatchRequest {
    private List<Long> excerptIds;
    // 选填：为了保持 AI 原先上下文需要可能提供的主题方向
    private String topicName; 
}
