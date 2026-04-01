package com.gleanread.server.infrastructure.persistence.repository;

import com.gleanread.server.domain.model.excerpt.Excerpt;
import com.gleanread.server.domain.model.excerpt.ExcerptRepository;
import com.gleanread.server.infrastructure.persistence.mapper.ExcerptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ExcerptRepositoryImpl implements ExcerptRepository {

    private final ExcerptMapper excerptMapper;

    @Override
    public Excerpt save(Excerpt excerpt) {
        excerptMapper.insert(excerpt);
        return excerpt;
    }

    @Override
    public Excerpt findById(Long id) {
        return excerptMapper.selectById(id);
    }

    @Override
    public List<Excerpt> listByIds(Collection<Long> excerptIds) {
        return excerptMapper.selectBatchIds(excerptIds);
    }

    @Override
    public void update(Excerpt excerpt) {
        excerptMapper.updateById(excerpt);
    }
}
