package com.gleanread.server.application.service;

import com.gleanread.server.domain.model.tag.Tag;
import com.gleanread.server.domain.model.tag.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TagAppService {

    private final TagRepository tagRepository;

    /**
     * 独立创建标签
     * 如果标签已存在，则返回现有标签
     */
    @Transactional(rollbackFor = Exception.class)
    public Tag createTag(String tagName) {
        if (tagName == null || tagName.trim().isEmpty()) {
            throw new IllegalArgumentException("标签名称不能为空");
        }

        return tagRepository.findByName(tagName)
                .orElseGet(() -> {
                    Tag newTag = Tag.createNew(tagName);
                    return tagRepository.save(newTag);
                });
    }
}
