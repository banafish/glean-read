package com.gleanread.server.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gleanread.server.domain.model.tag.ExcerptTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 摘录-标签关联中间表 Mapper
 */
@Mapper
public interface ExcerptTagMapper extends BaseMapper<ExcerptTag> {
}
