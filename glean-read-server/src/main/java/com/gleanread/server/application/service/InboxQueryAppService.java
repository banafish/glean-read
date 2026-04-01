package com.gleanread.server.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gleanread.server.domain.model.excerpt.Excerpt;
import com.gleanread.server.domain.model.tag.ExcerptTag;
import com.gleanread.server.domain.model.tag.Tag;
import com.gleanread.server.infrastructure.persistence.mapper.ExcerptMapper;
import com.gleanread.server.infrastructure.persistence.mapper.ExcerptTagMapper;
import com.gleanread.server.infrastructure.persistence.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Inbox 读模型应用服务 (CQRS 中的 Q 端处理逻辑)
 * 提供所有的只读查询接口给 Controller，避免 Controller 直接耦合 Infrastructure 层的 Mapper。
 */
@Service
@RequiredArgsConstructor
public class InboxQueryAppService {

    private final ExcerptMapper excerptMapper;
    private final TagMapper tagMapper;
    private final ExcerptTagMapper excerptTagMapper;

    public Page<Excerpt> getInboxExcerpts(long current, long size) {
        LambdaQueryWrapper<Excerpt> wrapper = new LambdaQueryWrapper<>();
        // 核心过滤条件：treeNodeId为空即代表未出 Inbox
        wrapper.isNull(Excerpt::getTreeNodeId)
                .orderByDesc(Excerpt::getCreateTime);

        return excerptMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public List<Tag> getTagCloud() {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Tag::getHeatWeight)
                .last("LIMIT 50"); // 限制最多展示 50 个词缀，防止客户端过载渲染
        return tagMapper.selectList(wrapper);
    }

    public Page<Excerpt> getExcerptsByTag(Long tagId, long current, long size) {
        // 1. 从中间表查询该标签关联的所有摘录 ID
        LambdaQueryWrapper<ExcerptTag> ftWrapper = new LambdaQueryWrapper<>();
        ftWrapper.eq(ExcerptTag::getTagId, tagId);
        List<Long> excerptIds = excerptTagMapper.selectList(ftWrapper)
                .stream()
                .map(ExcerptTag::getExcerptId)
                .collect(Collectors.toList());

        if (excerptIds.isEmpty()) {
            return new Page<>(current, size);
        }

        // 2. 再过滤出其中处于 Inbox 状态（treeNodeId IS NULL）的摘录
        LambdaQueryWrapper<Excerpt> excerptWrapper = new LambdaQueryWrapper<>();
        excerptWrapper.in(Excerpt::getId, excerptIds)
                .isNull(Excerpt::getTreeNodeId)
                .orderByDesc(Excerpt::getCreateTime);

        return excerptMapper.selectPage(new Page<>(current, size), excerptWrapper);
    }
}
