package com.gleanread.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gleanread.server.domain.dto.SynthesisRequest;
import com.gleanread.server.domain.entity.KnowledgeTreeNode;
import com.gleanread.server.mapper.KnowledgeTreeNodeMapper;
import com.gleanread.server.service.AiSynthesisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final AiSynthesisService synthesisService;
    private final KnowledgeTreeNodeMapper treeNodeMapper;

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
        LambdaQueryWrapper<KnowledgeTreeNode> wrapper = new LambdaQueryWrapper<>();
        // 可以根据实际需求过滤根节点或者全量返回，由于我们做了 parentId 支持，通过该接口返回一维结构，前端借助 parentId 自挂载即可变为立体树UI。
        wrapper.orderByAsc(KnowledgeTreeNode::getId);
        return ResponseEntity.ok(treeNodeMapper.selectList(wrapper));
    }
}
