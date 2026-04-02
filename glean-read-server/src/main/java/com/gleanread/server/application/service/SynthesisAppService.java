package com.gleanread.server.application.service;

import com.gleanread.server.application.dto.MountRequest;
import com.gleanread.server.domain.model.excerpt.Excerpt;
import com.gleanread.server.domain.model.excerpt.ExcerptRepository;
import com.gleanread.server.domain.model.tree.KnowledgeTreeNode;
import com.gleanread.server.domain.model.tree.KnowledgeTreeNodeRepository;
import com.gleanread.server.domain.port.LlmPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SynthesisAppService {

    private final ExcerptRepository excerptRepository;
    private final KnowledgeTreeNodeRepository treeNodeRepository;
    private final LlmPort llm;

    /**
     * 独立出纯粹的 AI 大纲生成逻辑，不具有产生和持久化副作用
     */
    public String generateOutlineForExcerpts(List<Long> excerptIds, String topicName) {
        List<Excerpt> excerpts = excerptRepository.listByIds(excerptIds);
        if (excerpts.isEmpty()) {
            throw new IllegalArgumentException("未找到待处理的摘录集，无法生成大纲");
        }
        
        String prompt = buildAiSynthesisPrompt(excerpts, topicName != null && !topicName.isEmpty() ? topicName : "通用系统综合");
        log.info("向大模型请求的批处理 Outline 总结 Prompt内容: \n{}", prompt);
        
        return llm.complete(prompt);
    }

    /**
     * 纯净挂载：不再隐式调用 AI，直接将摘录挂载到指定的 targetNodeId（如果提供）。
     * 若指定 outlineMarkdown 则更新或创建新节点。
     */
    @Transactional(rollbackFor = Exception.class)
    public void mountExcerpts(MountRequest request) {
        if (request.getExcerptIds() == null || request.getExcerptIds().isEmpty()) {
            return;
        }

        List<Excerpt> excerpts = excerptRepository.listByIds(request.getExcerptIds());
        if (excerpts.isEmpty()) {
            return;
        }

        Long finalTargetId = request.getTargetNodeId();

        // 1. 如果提供了 targetNodeId，则看是否需要附加上 outlineMarkdown
        if (finalTargetId != null) {
            if (request.getOutlineMarkdown() != null && !request.getOutlineMarkdown().trim().isEmpty()) {
                KnowledgeTreeNode node = treeNodeRepository.findById(finalTargetId);
                if (node != null) {
                    node.updateOutline(request.getOutlineMarkdown());
                    treeNodeRepository.update(node);
                }
            }
        } 
        // 2. 否则，如果提供了 parentNodeId 以及 title，则视为新建树节点
        else if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            KnowledgeTreeNode newNode = KnowledgeTreeNode.create(
                    request.getParentNodeId(), 
                    request.getTitle(), 
                    request.getOutlineMarkdown()
            );
            treeNodeRepository.save(newNode);
            // 将刚创建新节点的id作为真正的目标挂载点
            finalTargetId = newNode.getId();
        }
        
        for (Excerpt excerpt : excerpts) {
            excerpt.mountToNode(finalTargetId);
            excerptRepository.update(excerpt);
        }
    }

    private String buildAiSynthesisPrompt(List<Excerpt> excerpts, String topicName) {
        StringBuilder sb = new StringBuilder();
        sb.append("你现在是一个顶级架构师级知识归纳工具，负责把开发者的碎摘录化腐朽为神奇，整合成系统化大纲。\n");
        sb.append(String.format("本次需要构建的体系分支名称为：【%s】。\n\n", topicName));
        sb.append("下面是供给你的【基础素材】与【开发者的灵感（User Thought）】，各有唯一ID：\n");

        for (Excerpt excerpt : excerpts) {
            sb.append(String.format("- [知识摘录标号: #%d]:\n", excerpt.getId()));
            sb.append(String.format("  - 原文摘抄: %s\n", excerpt.getContent()));
            if (excerpt.getUserThought() != null && !excerpt.getUserThought().trim().isEmpty()) {
                sb.append(String.format("  - 用户自己记录的脑图灵感: %s\n", excerpt.getUserThought()));
            }
        }

        sb.append("\n【你必须遵守的输出约束条件】：\n");
        sb.append("1. **结构化呈现**：请输出美观、层次分明的 Markdown 语法。包含 1~2 句该技术的引介、深入点概括、可实操建议。\n");
        sb.append("2. **严丝合缝溯源**：如果某句话得出的结论或者概念是基于上面传给你的某个知识片段提取出来的，请你严格使用 Markdown加小括号（如：`(引自 摘录#ID)`）在此句末尾给原文打标记证明。这点极其核心！\n");

        return sb.toString();
    }
}
