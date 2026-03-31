package com.gleanread.server.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gleanread.server.domain.model.tree.KnowledgeTreeNode;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KnowledgeTreeNodeMapper extends BaseMapper<KnowledgeTreeNode> {
}
