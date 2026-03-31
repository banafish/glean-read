package com.gleanread.server.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gleanread.server.domain.model.tag.FragmentTag;
import com.gleanread.server.domain.model.tag.Tag;
import com.gleanread.server.domain.model.tag.TagRepository;
import com.gleanread.server.infrastructure.persistence.mapper.FragmentTagMapper;
import com.gleanread.server.infrastructure.persistence.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TagRepositoryImpl implements TagRepository {

    private final TagMapper tagMapper;
    private final FragmentTagMapper fragmentTagMapper;

    @Override
    public Optional<Tag> findByName(String tagName) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tag::getTagName, tagName);
        return Optional.ofNullable(tagMapper.selectOne(wrapper));
    }

    @Override
    public Tag save(Tag tag) {
        tagMapper.insert(tag);
        return tag;
    }

    @Override
    public void update(Tag tag) {
        tagMapper.updateById(tag);
    }

    @Override
    public void saveFragmentTagRelation(Long fragmentId, Long tagId) {
        fragmentTagMapper.insert(new FragmentTag(fragmentId, tagId));
    }
}
