package com.gleanread.server.domain.model.tree;

public interface KnowledgeTreeNodeRepository {
    
    KnowledgeTreeNode save(KnowledgeTreeNode entity);
    
    KnowledgeTreeNode findById(Long id);
    
    void update(KnowledgeTreeNode entity);
}
