package com.gleanread.server.infrastructure.persistence.repository;

import com.gleanread.server.domain.model.tree.KnowledgeTreeNode;
import com.gleanread.server.domain.model.tree.KnowledgeTreeNodeRepository;
import com.gleanread.server.infrastructure.persistence.mapper.KnowledgeTreeNodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class KnowledgeTreeNodeRepositoryImpl implements KnowledgeTreeNodeRepository {

    private final KnowledgeTreeNodeMapper mapper;

    @Override
    public KnowledgeTreeNode save(KnowledgeTreeNode entity) {
        mapper.insert(entity);
        return entity;
    }
}
