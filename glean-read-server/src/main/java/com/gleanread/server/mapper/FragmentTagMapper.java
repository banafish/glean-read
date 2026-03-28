package com.gleanread.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gleanread.server.domain.entity.FragmentTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 碎片-标签关联中间表 Mapper
 */
@Mapper
public interface FragmentTagMapper extends BaseMapper<FragmentTag> {
}
