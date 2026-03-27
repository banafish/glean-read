package com.gleanread.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gleanread.server.domain.dto.CaptureRequest;
import com.gleanread.server.domain.entity.Fragment;
import com.gleanread.server.domain.entity.Tag;
import com.gleanread.server.mapper.FragmentMapper;
import com.gleanread.server.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GleanService {

    private final FragmentMapper fragmentMapper;
    private final TagMapper tagMapper;

    /**
     * 核心：知识碎片入库、并触发标签热度计算器
     */
    @Transactional(rollbackFor = Exception.class)
    public void captureFragment(CaptureRequest request) {
        // 1. 保存从移动端采集的最原始碎片，状态自动设入 Inbox (treeNodeId为null)
        Fragment fragment = new Fragment();
        fragment.setContent(request.getContent());
        fragment.setUrl(request.getUrl());
        fragment.setUserThought(request.getUserThought());
        fragment.setCreateTime(LocalDateTime.now());
        fragment.setUpdateTime(LocalDateTime.now());
        fragmentMapper.insert(fragment);

        // 2. 统计更新标签的热度指数（形成标签云基础与重点追踪）
        if (request.getTags() != null) {
            for (String targetParamName : request.getTags()) {
                LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Tag::getTagName, targetParamName);
                Tag existTag = tagMapper.selectOne(wrapper);
                
                if (existTag == null) {
                    // 若是新产生的词汇，新增入库
                    Tag newTag = new Tag();
                    newTag.setTagName(targetParamName);
                    newTag.setHeatWeight(1);
                    newTag.setCreateTime(LocalDateTime.now());
                    tagMapper.insert(newTag);
                } else {
                    // 词汇已被关注过，每次引用热度指数递增+1
                    existTag.setHeatWeight(existTag.getHeatWeight() + 1);
                    tagMapper.updateById(existTag);
                }
            }
            // (通常在此处还需生成 fragment <-> tag 中间表的关联数据)
        }
    }
}
