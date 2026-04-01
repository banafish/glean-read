package com.gleanread.server.application.service;

import com.gleanread.server.application.dto.SynthesisRequest;
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
     * 合成和挂载 - 应用层编排逻辑
     */
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeTreeNode synthesizeAndMount(SynthesisRequest request) {
        // 1. 获取聚合根
        List<Excerpt> excerpts = excerptRepository.listByIds(request.getExcerptIds());

        if (excerpts.isEmpty()) {
            throw new IllegalArgumentException("未找到待处理的摘录集，合成逻辑中止");
        }

        // 2. 领域服务/能力：组装发送给 LLM 的Prompt (也可以抽取到专门的Domain Service类中)
        String prompt = buildAiSynthesisPrompt(excerpts, request.getTopicName());
        log.info("向大语言模型请求的合成 Prompt内容: \n{}", prompt);

        // 3. 依赖倒置：通过领域接口 LlmPort 调用基础设施层适配的大语言模型能力
        String outlineMarkdown = llm.complete(prompt);

        // 4. 利用 KnowledgeTreeNode 的充血方法创建节点实体并持久化
        KnowledgeTreeNode topicNode = KnowledgeTreeNode.create(
                request.getParentNodeId(), 
                request.getTopicName(), 
                outlineMarkdown
        );
        treeNodeRepository.save(topicNode);

        // 5. 改变摘录生命周期流转状态: 调用摘录的归属方法完成挂载
        for (Excerpt excerpt : excerpts) {
            excerpt.mountToNode(topicNode.getId());
            excerptRepository.update(excerpt);
        }

        return topicNode;
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
