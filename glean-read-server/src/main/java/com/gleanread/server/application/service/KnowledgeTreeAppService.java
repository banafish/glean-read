package com.gleanread.server.application.service;

import com.gleanread.server.application.dto.CreateNodeRequest;
import com.gleanread.server.domain.model.tree.KnowledgeTreeNode;
import com.gleanread.server.domain.model.tree.KnowledgeTreeNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KnowledgeTreeAppService {

    private final KnowledgeTreeNodeRepository treeNodeRepository;

    @Transactional(rollbackFor = Exception.class)
    public KnowledgeTreeNode createNode(CreateNodeRequest request) {
        if (request.getNodeTitle() == null || request.getNodeTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("节点名称不能为空");
        }

        if (request.getParentId() != null) {
            KnowledgeTreeNode parent = treeNodeRepository.findById(request.getParentId());
            if (parent == null) {
                throw new IllegalArgumentException("指定的父节点不存在: " + request.getParentId());
            }
        }

        KnowledgeTreeNode newNode = KnowledgeTreeNode.create(
                request.getParentId(),
                request.getNodeTitle(),
                request.getOutlineMarkdown()
        );
        treeNodeRepository.save(newNode);
        
        return newNode;
    }
}
