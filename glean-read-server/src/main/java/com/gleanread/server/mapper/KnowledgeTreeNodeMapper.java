package com.gleanread.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gleanread.server.domain.entity.KnowledgeTreeNode;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KnowledgeTreeNodeMapper extends BaseMapper<KnowledgeTreeNode> {
}
