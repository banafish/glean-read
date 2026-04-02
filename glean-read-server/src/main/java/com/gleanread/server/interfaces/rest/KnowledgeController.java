package com.gleanread.server.interfaces.rest;

import com.gleanread.server.application.dto.OutlineBatchRequest;
import com.gleanread.server.application.dto.MountRequest;
import com.gleanread.server.domain.model.tree.KnowledgeTreeNode;
import com.gleanread.server.application.service.SynthesisAppService;
import com.gleanread.server.application.service.KnowledgeQueryAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final SynthesisAppService synthesisService;
    private final KnowledgeQueryAppService knowledgeQueryAppService;

    /**
     * 实现任务 2：批量摘录生成 AI Outline
     */
    @PostMapping("/ai/outline/batch")
    public ResponseEntity<String> generateOutlineForExcerpts(@RequestBody OutlineBatchRequest request) {
        String outline = synthesisService.generateOutlineForExcerpts(request.getExcerptIds(), request.getTopicName());
        return ResponseEntity.ok(outline);
    }

    /**
     * 实现任务 3：纯净版挂载接口，剥离了AI调用，支持挂接到已有对象
     */
    @PostMapping("/mount")
    public ResponseEntity<String> mountExcerpts(@RequestBody MountRequest request) {
        synthesisService.mountExcerpts(request);
        return ResponseEntity.ok("Successfully mounted excerpts.");
    }

    /**
     * 实现任务 4.4: 提供供 Web 前端呈现专属大树结构的接口
     */
    @GetMapping("/tree")
    public ResponseEntity<List<KnowledgeTreeNode>> getFullKnowledgeTree() {
        return ResponseEntity.ok(knowledgeQueryAppService.getFullKnowledgeTree());
    }
}
