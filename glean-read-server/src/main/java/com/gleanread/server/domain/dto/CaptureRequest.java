package com.gleanread.server.domain.dto;

import java.util.List;
import lombok.Data;

@Data
public class CaptureRequest {
    private String content;
    private String url;
    private String userThought;
    private List<String> tags;
}
