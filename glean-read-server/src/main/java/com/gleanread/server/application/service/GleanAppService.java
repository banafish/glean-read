package com.gleanread.server.application.service;

import com.gleanread.server.application.dto.CaptureRequest;
import com.gleanread.server.domain.model.excerpt.Excerpt;
import com.gleanread.server.domain.model.excerpt.ExcerptRepository;
import com.gleanread.server.domain.model.tag.Tag;
import com.gleanread.server.domain.model.tag.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GleanAppService {

    private final ExcerptRepository excerptRepository;
    private final TagRepository tagRepository;

    /**
     * 核心应用服务：处理用户截取摘录的用例
     * 协调 Excerpt 和 Tag 的充血对象，并将其持久化
     */
    @Transactional(rollbackFor = Exception.class)
    public void captureExcerpt(CaptureRequest request) {
        // 1. 创建知识摘录聚合根 (充血模型自身处理所有初始属性的设置)
        Excerpt excerpt = Excerpt.create(request.getContent(), request.getUrl(), request.getUserThought());
        excerptRepository.save(excerpt);

        // 2. 统计更新标签的热度指数
        if (request.getTags() != null) {
            for (String targetParamName : request.getTags()) {
                // 读取已存在的标签模型，若不存在则新建
                Tag tag = tagRepository.findByName(targetParamName).orElseGet(() -> {
                    // 通过充血工厂创建
                    Tag newTag = Tag.createNew(targetParamName);
                    return tagRepository.save(newTag);
                });

                if (tag.getId() != null) {
                    // 这里对已存在的标签热度自增
                    tag.incrementHeatWeight();
                    tagRepository.update(tag);
                }
                
                // 持久化关联关系
                tagRepository.saveExcerptTagRelation(excerpt.getId(), tag.getId());
            }
        }
    }
}
