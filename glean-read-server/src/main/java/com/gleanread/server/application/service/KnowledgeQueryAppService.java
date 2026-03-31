package com.gleanread.server.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gleanread.server.domain.model.tree.KnowledgeTreeNode;
import com.gleanread.server.infrastructure.persistence.mapper.KnowledgeTreeNodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识树 读模型应用服务 (CQRS)
 */
@Service
@RequiredArgsConstructor
public class KnowledgeQueryAppService {

    private final KnowledgeTreeNodeMapper treeNodeMapper;

    public List<KnowledgeTreeNode> getFullKnowledgeTree() {
        LambdaQueryWrapper<KnowledgeTreeNode> wrapper = new LambdaQueryWrapper<>();
        // 可以根据实际需求过滤根节点或者全量返回，由于我们做了 parentId 支持，通过该接口返回一维结构，前端借助 parentId
        // 自挂载即可变为立体树UI。
        wrapper.orderByAsc(KnowledgeTreeNode::getId);
        return treeNodeMapper.selectList(wrapper);
    }
}
