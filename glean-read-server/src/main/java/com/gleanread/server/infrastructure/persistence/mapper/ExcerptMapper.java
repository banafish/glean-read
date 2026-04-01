package com.gleanread.server.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gleanread.server.domain.model.excerpt.Excerpt;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExcerptMapper extends BaseMapper<Excerpt> {
}
