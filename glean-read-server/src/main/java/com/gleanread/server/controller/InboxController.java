package com.gleanread.server.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gleanread.server.domain.entity.Fragment;
import com.gleanread.server.domain.entity.FragmentTag;
import com.gleanread.server.domain.entity.Tag;
import com.gleanread.server.mapper.FragmentMapper;
import com.gleanread.server.mapper.FragmentTagMapper;
import com.gleanread.server.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/inbox")
@RequiredArgsConstructor
public class InboxController {

    private final FragmentMapper fragmentMapper;
    private final TagMapper tagMapper;
    private final FragmentTagMapper fragmentTagMapper;

    /**
     * 实现任务 3.3: 供前台调用，分页展示当前所有还未被归入某一个大纲树节点的、处于野生状态的碎片。
     */
    @GetMapping("/fragments")
    public ResponseEntity<Page<Fragment>> getInboxFragments(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        
        LambdaQueryWrapper<Fragment> wrapper = new LambdaQueryWrapper<>();
        // 核心过滤条件：treeNodeId为空即代表未出 Inbox
        wrapper.isNull(Fragment::getTreeNodeId)
               .orderByDesc(Fragment::getCreateTime);
        
        Page<Fragment> page = fragmentMapper.selectPage(new Page<>(current, size), wrapper);
        return ResponseEntity.ok(page);
    }

    /**
     * 实现任务 3.4: 获取所有标签，并按引用热度排倒序。供前端构建热力标签云。
     */
    @GetMapping("/tag-cloud")
    public ResponseEntity<List<Tag>> getTagCloud() {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Tag::getHeatWeight)
               .last("LIMIT 50"); // 限制最多展示 50 个词缀，防止客户端过载渲染
        return ResponseEntity.ok(tagMapper.selectList(wrapper));
    }
    /**
     * 新增任务 2.4: 按标签筛选 Inbox 碎片，用于 AI 合成前的选料流程
     * GET /api/v1/inbox/fragments/by-tag/{tagId}
     */
    @GetMapping("/fragments/by-tag/{tagId}")
    public ResponseEntity<Page<Fragment>> getFragmentsByTag(
            @PathVariable Long tagId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {

        // 1. 从中间表查询该标签关联的所有碎片 ID
        LambdaQueryWrapper<FragmentTag> ftWrapper = new LambdaQueryWrapper<>();
        ftWrapper.eq(FragmentTag::getTagId, tagId);
        List<Long> fragmentIds = fragmentTagMapper.selectList(ftWrapper)
                .stream()
                .map(FragmentTag::getFragmentId)
                .collect(Collectors.toList());

        if (fragmentIds.isEmpty()) {
            return ResponseEntity.ok(new Page<>(current, size));
        }

        // 2. 再过滤出其中处于 Inbox 状态（treeNodeId IS NULL）的碎片
        LambdaQueryWrapper<Fragment> fragmentWrapper = new LambdaQueryWrapper<>();
        fragmentWrapper.in(Fragment::getId, fragmentIds)
                       .isNull(Fragment::getTreeNodeId)
                       .orderByDesc(Fragment::getCreateTime);

        Page<Fragment> page = fragmentMapper.selectPage(new Page<>(current, size), fragmentWrapper);
        return ResponseEntity.ok(page);
    }
}
