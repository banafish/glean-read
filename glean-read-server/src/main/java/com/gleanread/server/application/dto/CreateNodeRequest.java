package com.gleanread.server.application.dto;

import lombok.Data;

@Data
public class CreateNodeRequest {
    private Long parentId;
    private String nodeTitle;
    private String outlineMarkdown;
}
