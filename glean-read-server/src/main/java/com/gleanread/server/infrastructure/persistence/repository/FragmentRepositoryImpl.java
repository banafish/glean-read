package com.gleanread.server.infrastructure.persistence.repository;

import com.gleanread.server.domain.model.fragment.Fragment;
import com.gleanread.server.domain.model.fragment.FragmentRepository;
import com.gleanread.server.infrastructure.persistence.mapper.FragmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class FragmentRepositoryImpl implements FragmentRepository {

    private final FragmentMapper fragmentMapper;

    @Override
    public Fragment save(Fragment fragment) {
        fragmentMapper.insert(fragment);
        return fragment;
    }

    @Override
    public Fragment findById(Long id) {
        return fragmentMapper.selectById(id);
    }

    @Override
    public List<Fragment> listByIds(Collection<Long> fragmentIds) {
        return fragmentMapper.selectBatchIds(fragmentIds);
    }

    @Override
    public void update(Fragment fragment) {
        fragmentMapper.updateById(fragment);
    }
}
