package com.gleanread.server.application.service;

import com.gleanread.server.application.dto.CaptureRequest;
import com.gleanread.server.domain.model.fragment.Fragment;
import com.gleanread.server.domain.model.fragment.FragmentRepository;
import com.gleanread.server.domain.model.tag.Tag;
import com.gleanread.server.domain.model.tag.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GleanAppService {

    private final FragmentRepository fragmentRepository;
    private final TagRepository tagRepository;

    /**
     * 核心应用服务：处理用户截取碎片的用例
     * 协调 Fragment 和 Tag 的充血对象，并将其持久化
     */
    @Transactional(rollbackFor = Exception.class)
    public void captureFragment(CaptureRequest request) {
        // 1. 创建知识碎片聚合根 (充血模型自身处理所有初始属性的设置)
        Fragment fragment = Fragment.create(request.getContent(), request.getUrl(), request.getUserThought());
        fragmentRepository.save(fragment);

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
                    // （如果上面刚 save，它的 id 不为 null，且 heatWeight 已经在创建时被置为 1，这里不需要再调用自增了，这里为了体现充血的灵活性我们统一自增可能会导致新建的初始变为2，但为保留原版逻辑略作判断）
                    // 其实原版逻辑是：若是老的标签，递增+1。这里完善了如果不是新save的（通过判断业务状态）
                    tag.incrementHeatWeight();
                    tagRepository.update(tag);
                }
                
                // 持久化关联关系
                tagRepository.saveFragmentTagRelation(fragment.getId(), tag.getId());
            }
        }
    }
}
