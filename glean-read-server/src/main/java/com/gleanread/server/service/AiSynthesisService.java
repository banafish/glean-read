package com.gleanread.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gleanread.server.domain.dto.SynthesisRequest;
import com.gleanread.server.domain.entity.Fragment;
import com.gleanread.server.domain.entity.KnowledgeTreeNode;
import com.gleanread.server.mapper.FragmentMapper;
import com.gleanread.server.mapper.KnowledgeTreeNodeMapper;
import com.gleanread.server.service.llm.LlmPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSynthesisService {

    private final FragmentMapper fragmentMapper;
    private final KnowledgeTreeNodeMapper treeNodeMapper;
    private final LlmPort llm;

    /**
     * 核心业务：汇聚碎片，请求 AI 合成大纲，并将结果与原始碎片完成双向关联与入库
     */
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeTreeNode synthesizeAndMount(SynthesisRequest request) {
        // 1. 查询出传入的所有待处理叶子碎片
        LambdaQueryWrapper<Fragment> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Fragment::getId, request.getFragmentIds());
        List<Fragment> fragments = fragmentMapper.selectList(wrapper);

        if (fragments.isEmpty()) {
            throw new IllegalArgumentException("未找到待处理的碎片集，合成逻辑中止");
        }

        // 2. 将碎片和个人想法拼接，构建发送给 LLM 的特定结构化 Prompt
        String prompt = buildAiSynthesisPrompt(fragments, request.getTopicName());
        log.info("向大语言模型请求的合成 Prompt内容: \n{}", prompt);

        // 3. 通过 LlmPort 策略接口调用大语言模型（生产环境用 DeepSeekLlmAdapter，本地开发用 MockLlmAdapter）
        String outlineMarkdown = llm.complete(prompt);

        // 4. 将 AI 萃取出的大纲落库，形成 "KnowledgeTree" 上的一根实干节点
        KnowledgeTreeNode topicNode = new KnowledgeTreeNode();
        topicNode.setNodeTitle(request.getTopicName());
        topicNode.setParentId(request.getParentNodeId());
        topicNode.setOutlineMarkdown(outlineMarkdown);
        topicNode.setCreateTime(LocalDateTime.now());
        topicNode.setUpdateTime(LocalDateTime.now());
        treeNodeMapper.insert(topicNode);

        // 5. 状态流转转移：将原本在 Inbox 的碎片关联至该树节点名下
        // （这也意味着碎片被"认领"了，前台展示收件箱时过滤掉 treeNodeId 不为空的元素即可实现归档）
        for (Fragment fragment : fragments) {
            fragment.setTreeNodeId(topicNode.getId());
            fragmentMapper.updateById(fragment);
        }

        return topicNode;
    }

    /**
     * 实现任务 4.2: 开发提取大纲以及严格约束溯源标注的 AI 请求指令参数 (Prompt)
     */
    private String buildAiSynthesisPrompt(List<Fragment> fragments, String topicName) {
        StringBuilder sb = new StringBuilder();
        sb.append("你现在是一个顶级架构师级知识归纳工具，负责把开发者的碎片化摘录化腐朽为神奇，整合成系统化大纲。\n");
        sb.append(String.format("本次需要构建的体系分支名称为：【%s】。\n\n", topicName));
        sb.append("下面是供给你的【基础素材】与【开发者的灵感（User Thought）】，各有唯一ID：\n");

        for (Fragment fragment : fragments) {
            sb.append(String.format("- [知识碎片标号: #%d]:\n", fragment.getId()));
            sb.append(String.format("  - 原文摘抄: %s\n", fragment.getContent()));
            if (fragment.getUserThought() != null && !fragment.getUserThought().trim().isEmpty()) {
                sb.append(String.format("  - 用户自己记录的脑图灵感: %s\n", fragment.getUserThought()));
            }
        }

        sb.append("\n【你必须遵守的输出约束条件】：\n");
        sb.append("1. **结构化呈现**：请输出美观、层次分明的 Markdown 语法。包含 1~2 句该技术的引介、深入点概括、可实操建议。\n");
        sb.append(
                "2. **严丝合缝溯源**：如果某句话得出的结论或者概念是基于上面传给你的某个知识片段提取出来的，请你严格使用 Markdown加小括号（如：`(引自 碎片#ID)`）在此句末尾给原文打标记证明。这点极其核心！\n");

        return sb.toString();
    }
}
