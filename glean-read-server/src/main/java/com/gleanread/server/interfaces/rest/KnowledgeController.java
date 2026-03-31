package com.gleanread.server.interfaces.rest;

import com.gleanread.server.application.dto.SynthesisRequest;
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
     * 将指定零散碎片丢给云端大模型，融合成系统化大纲，并建立起“树节点”联系
     */
    @PostMapping("/synthesis")
    public ResponseEntity<KnowledgeTreeNode> synthesizeOutline(@RequestBody SynthesisRequest request) {
        KnowledgeTreeNode resultTopicNode = synthesisService.synthesizeAndMount(request);
        return ResponseEntity.ok(resultTopicNode);
    }

    /**
     * 实现任务 4.4: 提供供 Web 前端呈现专属大树结构的接口
     */
    @GetMapping("/tree")
    public ResponseEntity<List<KnowledgeTreeNode>> getFullKnowledgeTree() {
        return ResponseEntity.ok(knowledgeQueryAppService.getFullKnowledgeTree());
    }
}
