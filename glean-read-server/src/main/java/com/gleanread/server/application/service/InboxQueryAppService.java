package com.gleanread.server.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gleanread.server.domain.model.fragment.Fragment;
import com.gleanread.server.domain.model.tag.FragmentTag;
import com.gleanread.server.domain.model.tag.Tag;
import com.gleanread.server.infrastructure.persistence.mapper.FragmentMapper;
import com.gleanread.server.infrastructure.persistence.mapper.FragmentTagMapper;
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

    private final FragmentMapper fragmentMapper;
    private final TagMapper tagMapper;
    private final FragmentTagMapper fragmentTagMapper;

    public Page<Fragment> getInboxFragments(long current, long size) {
        LambdaQueryWrapper<Fragment> wrapper = new LambdaQueryWrapper<>();
        // 核心过滤条件：treeNodeId为空即代表未出 Inbox
        wrapper.isNull(Fragment::getTreeNodeId)
                .orderByDesc(Fragment::getCreateTime);

        return fragmentMapper.selectPage(new Page<>(current, size), wrapper);
    }

    public List<Tag> getTagCloud() {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Tag::getHeatWeight)
                .last("LIMIT 50"); // 限制最多展示 50 个词缀，防止客户端过载渲染
        return tagMapper.selectList(wrapper);
    }

    public Page<Fragment> getFragmentsByTag(Long tagId, long current, long size) {
        // 1. 从中间表查询该标签关联的所有碎片 ID
        LambdaQueryWrapper<FragmentTag> ftWrapper = new LambdaQueryWrapper<>();
        ftWrapper.eq(FragmentTag::getTagId, tagId);
        List<Long> fragmentIds = fragmentTagMapper.selectList(ftWrapper)
                .stream()
                .map(FragmentTag::getFragmentId)
                .collect(Collectors.toList());

        if (fragmentIds.isEmpty()) {
            return new Page<>(current, size);
        }

        // 2. 再过滤出其中处于 Inbox 状态（treeNodeId IS NULL）的碎片
        LambdaQueryWrapper<Fragment> fragmentWrapper = new LambdaQueryWrapper<>();
        fragmentWrapper.in(Fragment::getId, fragmentIds)
                .isNull(Fragment::getTreeNodeId)
                .orderByDesc(Fragment::getCreateTime);

        return fragmentMapper.selectPage(new Page<>(current, size), fragmentWrapper);
    }
}
