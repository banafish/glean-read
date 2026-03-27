package com.gleanread.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gleanread.server.domain.entity.Tag;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {
}
